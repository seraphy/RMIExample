package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleIntegerProperty;
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
import javafx.stage.Stage;

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
	private Registry registry;

	/**
	 * 公開したオブジェクト
	 */
	private RMIExampleObject exportedObj;

	/**
	 * サーバソケットファクトリ
	 */
	private RMICustomServerSocketFactory serverSocketFactory;

	/**
	 * ソケットファクトリ
	 */
	private RMICustomClientSocketFactory clientSocketFactory;

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

		btnExit.disableProperty().bind(numOfServerSocket.greaterThan(0));

		lblStatus.textProperty().bind(strNumOfSocketCounts);

		serverSocketFactory = new RMICustomServerSocketFactory();
		serverSocketFactory.setUpdateSocketCount((count) -> {
			Platform.runLater(() -> {
				numOfServerSocket.set(count);
			});
		});

		HashSet<RMICustomClientSocketFactory> factories = new HashSet<>();

		RMICustomClientSocketFactory.setClientFactoryHandler(factory -> {
			UUID uuid = factory.getUUID();
			Platform.runLater(() -> {
				int en = txtLogs.getLength();
				txtLogs.insertText(en,
						"ClientFactory Created: " + factory + ":UUID="
								+ uuid + "\r\n");
			});

			if (factories.add(factory)) {
				factory.setUpdateSocketCount((count) -> {
					Platform.runLater(() -> {
						numOfClientSocket.set(count);
					});
				});
			}
		});
		clientSocketFactory = new RMICustomClientSocketFactory();

		UUID uuid = UUID.randomUUID();
		clientSocketFactory.setUUID(uuid);
		log.info("◆◆CLIENT SOCKET UUID=" + uuid);

		int port = Registry.REGISTRY_PORT;
		txtRegisterPort.setText(Integer.toString(port));
		txtExportPort.setText(Integer.toString(port + 1));

		btnRegister.setDisable(false);
		btnUnregister.setDisable(true);
	}

	@FXML
	protected void onRegister() {
		if (registry != null) {
			throw new IllegalStateException();
		}

		try {
			int registerPort = Integer.parseInt(txtRegisterPort.getText());
			int exportPort = Integer.parseInt(txtExportPort.getText());

			registry = LocateRegistry.createRegistry(registerPort,
					clientSocketFactory, serverSocketFactory);

			exportedObj = new RMIExampleObject() {

				@Override
				public String shutdown() throws RemoteException {
					String ret = super.shutdown();

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

					// ※ 公開停止後、Sleepとガベージコレクトしても接続は即時には切れないようだ
					// ※ コールバック用に受け取ったリモートオブジェクトも解放させる.
					try {
						for (int idx = 0; idx < 3; idx++) {
							System.gc();
							Thread.sleep(300);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}

					return ret;
				}

				@Override
				protected void showError(Throwable ex) {
					Platform.runLater(() -> {
						ErrorDialogUtils.showException(getStage(), ex);
					});
				}

				@Override
				protected void showMessage(String message) {
					Platform.runLater(() -> {
						int en = txtLogs.getLength();
						txtLogs.insertText(en, message + "\r\n");
					});
				}
			};
			RMIServer stub = (RMIServer) UnicastRemoteObject.exportObject(
					exportedObj,
					exportPort, clientSocketFactory, serverSocketFactory);
			registry.bind("RMIExample", stub);

			log.info("◆◆registered◆◆");

			btnRegister.setDisable(true);
			btnUnregister.setDisable(false);
			txtRegisterPort.setDisable(true);
			txtExportPort.setDisable(true);

		} catch (Exception ex) {
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}

	@FXML
	protected void onUnregister() {
		if (registry == null) {
			return;
		}
		try {
			// オブジェクトの取り下げ
			UnicastRemoteObject.unexportObject(exportedObj, true);
			exportedObj = null;

			// RMIレジストリの公開取り下げ
			UnicastRemoteObject.unexportObject(registry, true);
			registry = null;

			log.info("◆◆unregistered◆◆");

			btnRegister.setDisable(false);
			btnUnregister.setDisable(true);
			txtRegisterPort.setDisable(false);
			txtExportPort.setDisable(false);

		} catch (Exception ex) {
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}

	@FXML
	protected void onSelfTest() {
		try {
			int registerPort = Integer.parseInt(txtRegisterPort.getText());
			Registry registry = LocateRegistry.getRegistry(registerPort);

			RMIServer example = (RMIServer) registry.lookup("RMIExample");
			Message message = new Message();
			message.setTime(LocalDateTime.now());
			message.setMessage("★FROM LOCAL★");
			example.sayHello(message);

		} catch (RemoteException | NotBoundException ex) {
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}

	@FXML
	protected void onExit() {
		if (serverSocketFactory.getNumOfSockets() > 0) {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setHeaderText("Still connected.");
			alert.showAndWait();
			return;
		}
		getStage().close();
		Platform.exit();
	}
}
