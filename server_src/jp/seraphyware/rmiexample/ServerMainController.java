package jp.seraphyware.rmiexample;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import jp.seraphyware.rmiexample.rmi.Message;
import jp.seraphyware.rmiexample.rmi.RMICustomSocketWatcher;
import jp.seraphyware.rmiexample.rmi.RemoteControl;
import jp.seraphyware.rmiexample.rmi.RemoteFileIO;
import jp.seraphyware.rmiexample.rmi.RemoteObjectHelper;
import jp.seraphyware.rmiexample.ui.AbstractFXMLController;
import jp.seraphyware.rmiexample.ui.ErrorDialogUtils;
import jp.seraphyware.rmiexample.util.XMLResourceBundleControl;

public class ServerMainController extends AbstractFXMLController
		implements Initializable {

	/**
	 * ロガー
	 */
	private static final Logger log = Logger
			.getLogger(ServerMainController.class.getName());

	@FXML
	private TextField txtRegisterPort;

	@FXML
	private TextField txtExportPort;

	@FXML
	private TextField txtWorkDir;

	@FXML
	private Button btnWorkDir;

	@FXML
	private Button btnRegister;

	@FXML
	private Button btnUnregister;

	@FXML
	private Button btnExit;

	@FXML
	private TextArea txtLogs;

	@FXML
	private Label lblStatus;

	/**
	 * リソースバンドル
	 */
	private ResourceBundle resource = ResourceBundle.getBundle(
			getClass().getName(), new XMLResourceBundleControl());

	/**
	 * RMIレジストリ(ローカルマシン上)
	 */
	private ObjectProperty<Registry> registryProperty = new SimpleObjectProperty<>();

	/**
	 * 公開したリモートファイル入出力オブジェクト
	 */
	private RemoteFileIOImpl remoteFileIOObj;

	/**
	 * 公開したリモート制御オブジェクト
	 */
	private RemoteControlImpl remoteControlObj;

	@Override
	protected Stage makeStage() {
		Stage stage = super.makeStage();
		stage.setTitle(resource.getString("title"));
		stage.setOnCloseRequest(evt -> {
			onExit();
			evt.consume();
		});
		return stage;
	}

	@Override
	protected Parent makeRoot() {
		// FXMLファイルとリソースバンドルより画面を構成する
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("ServerMain.fxml"));
		loader.setResources(resource);

		// コントローラは明示的に、このオブジェクトに関連づける
		loader.setController(this);

		// FXMLのロード
		Parent root;
		try {
			root = (Parent) loader.load();

		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return root;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// サーバー･クライアント接続数、およびステータス表示
		SimpleIntegerProperty numOfServerSocket = new SimpleIntegerProperty();
		SimpleIntegerProperty numOfClientSocket = new SimpleIntegerProperty();

		StringBinding strNumOfSocketCounts = new StringBinding() {
			{
				bind(numOfServerSocket);
				bind(numOfClientSocket);
			}

			@Override
			protected String computeValue() {
				return "server:" + numOfServerSocket.intValue() + " client:"
						+ numOfClientSocket.intValue();
			}
		};

		// サーバ接続がある場合はexitは不活性とする
		btnExit.disableProperty().bind(numOfServerSocket.greaterThan(0));

		// サーバー･クライアント接続数ステータス表示のバインド
		lblStatus.textProperty().bind(strNumOfSocketCounts);

		// サーバ･クライアントソケットの接続・切断を監視し、
		// 接続数に変化がある場合は接続数プロパティを更新する.
		RemoteObjectHelper helper = RemoteObjectHelper.getInstance();
		RMICustomSocketWatcher socketWatcher = helper.getSocketWatcher();
		socketWatcher.setNumOfSocketsListener((server, client) -> {
			Platform.runLater(() -> {
				// Propertyの変更はJavaFXスレッド上で行う.
				numOfServerSocket.set(server);
				numOfClientSocket.set(client);
			});
		});

		// クライアントソケットファクトリのUUIDを設定
		onUpdateClientSocketFactoryUUID();

		// ワークディレクトリが妥当であるか検証する
		BooleanBinding validWorkDir = new BooleanBinding() {
			{
				bind(txtWorkDir.textProperty());
			}

			@Override
			protected boolean computeValue() {
				String strWorkDir = txtWorkDir.getText();
				if (strWorkDir.trim().length() > 0) {
					File file = new File(strWorkDir);
					if (file.isDirectory()) {
						return true;
					}
				}
				return false;
			}
		};

		// 初期値の設定
		int port = Registry.REGISTRY_PORT;
		txtRegisterPort.setText(Integer.toString(port));
		txtExportPort.setText(Integer.toString(port + 1));
		txtWorkDir.setText(".");

		// 活性制御
		btnRegister.disableProperty().bind(registryProperty.isNotNull().or(validWorkDir.not()));
		btnUnregister.disableProperty().bind(registryProperty.isNull());
		txtRegisterPort.disableProperty().bind(registryProperty.isNotNull());
		txtExportPort.disableProperty().bind(registryProperty.isNotNull());
		txtWorkDir.disableProperty().bind(registryProperty.isNotNull());
		btnWorkDir.disableProperty().bind(registryProperty.isNotNull());
	}

	@FXML
	protected void onBrowseWorkDir() {
		String strWorkDir = txtWorkDir.getText();
		DirectoryChooser dirChooser = new DirectoryChooser();
		if (strWorkDir.trim().length() > 0) {
			File workDir = new File(strWorkDir);
			if (workDir.isDirectory()) {
				dirChooser.setInitialDirectory(workDir);
			}
		}
		File choosedDir = dirChooser.showDialog(getStage());
		if (choosedDir != null) {
			txtWorkDir.setText(choosedDir.toString());
		}
	}

	@FXML
	protected void onUpdateClientSocketFactoryUUID() {
		RemoteObjectHelper helper = RemoteObjectHelper.getInstance();
		helper.updateClientSocketFactoryUUID();
	}

	@FXML
	protected void onRegister() {
		if (registryProperty.get() != null) {
			throw new IllegalStateException();
		}

		try {
			// ワークディレクトリ
			String strWorkDir = txtWorkDir.getText();
			File dir = new File(strWorkDir);

			remoteFileIOObj = new RemoteFileIOImpl() {
				@Override
				protected void showError(Throwable ex) {
					ServerMainController.this.showError(ex);
				}

				@Override
				protected void showMessage(String message) {
					ServerMainController.this.showMessage(message);
				}

				@Override
				protected File getBaseDir() {
					return dir;
				}
			};

			remoteControlObj = new RemoteControlImpl() {
				@Override
				protected void showMessage(String message) {
					ServerMainController.this.showMessage(message);
				}

				@Override
				public String shutdown() throws RemoteException {
					// Unregisterをリモート側より要求する
					onRequestShutdown();
					return "shutdown";
				}
			};

			// オブジェクトのエクスポートヘルパ
			int exportPort = Integer.parseInt(txtExportPort.getText());

			RemoteObjectHelper helper = RemoteObjectHelper.getInstance();
			helper.setExportPort(exportPort);

			// ローカルのRMIレジストリの作成と公開
			int registerPort = Integer.parseInt(txtRegisterPort.getText());
			Registry registry = helper.createLocalRegistry(registerPort);
			registryProperty.set(registry);

			// サーバオブジェクトをリモートとして公開可能にする.
			RemoteFileIO remoteFileIOStub = (RemoteFileIO) helper
					.exportObject(remoteFileIOObj);
			RemoteControl remoteControlStub = (RemoteControl) helper
					.exportObject(remoteControlObj);

			// RMIレジストリにクラス名でリモートオブジェクトを登録する.
			registry.bind(RemoteFileIO.class.getName(), remoteFileIOStub);
			registry.bind(RemoteControl.class.getName(), remoteControlStub);

		} catch (Exception ex) {
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}

	protected void showError(Throwable ex) {
		log.log(Level.WARNING, ex.toString(), ex);
		Platform.runLater(() -> {
			ErrorDialogUtils.showException(getStage(), ex);
		});
	}

	protected void showMessage(String message) {
		log.info(message);
		Platform.runLater(() -> {
			int en = txtLogs.getLength();
			txtLogs.insertText(en, message + "\r\n");
		});
	}

	@FXML
	protected void onGarbageCollect() {
		for (int idx = 0; idx < 3; idx++) {
			System.gc();

			try {
				Thread.sleep(100);

			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	@FXML
	protected void onUnregister() {
		if (registryProperty.get() == null) {
			return;
		}
		try {
			RemoteObjectHelper helper = RemoteObjectHelper.getInstance();

			// オブジェクトの取り下げ
			helper.unexportObject(remoteFileIOObj, true);
			remoteFileIOObj = null;

			helper.unexportObject(remoteControlObj, true);
			remoteControlObj = null;

			// RMIレジストリの公開取り下げ
			Registry registry = registryProperty.get();
			helper.unexportObject(registry, true);
			registryProperty.set(null);

			log.info("◆◆unregistered◆◆");

		} catch (Exception ex) {
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}

	protected void onRequestShutdown() {
		CompletableFuture<Void> future = new CompletableFuture<>();
		Platform.runLater(() -> {
			try {
				onUnregister();
				future.complete(null);

			} catch (Throwable ex) {
				ErrorDialogUtils.showException(getStage(), ex);
				future.completeExceptionally(ex);
			}
		});
		try {
			future.get();

		} catch (InterruptedException ex) {
			log.log(Level.WARNING, ex.toString(), ex);

		} catch (ExecutionException ex) {
			throw new RuntimeException(ex.getCause());
		}

		try {
			for (int idx = 0; idx < 3; idx++) {
				System.gc();
				Thread.sleep(300);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	protected void onSelfTest() {
		try {
			RemoteObjectHelper helper = RemoteObjectHelper.getInstance();
			int registerPort = Integer.parseInt(txtRegisterPort.getText());

			Registry registry = helper.getRegistry(null, registerPort);

			RemoteControl example = (RemoteControl) registry
					.lookup(RemoteControl.class.getName());
			Message message = new Message();
			message.setTime(LocalDateTime.now());
			message.setMessage("★FROM LOCAL★");
			example.echo(message);

		} catch (RemoteException | NotBoundException ex) {
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}

	@FXML
	protected void onExit() {
		RemoteObjectHelper helper = RemoteObjectHelper.getInstance();
		RMICustomSocketWatcher socketWatcher = helper.getSocketWatcher();
		if (socketWatcher.getNumOfServerSockets() > 0) {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setHeaderText("Still connected.");
			alert.showAndWait();
			return;
		}
		getStage().close();
		Platform.exit();
	}
}
