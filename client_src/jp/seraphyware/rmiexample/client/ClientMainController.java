package jp.seraphyware.rmiexample.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import jp.seraphyware.rmiexample.AbstractFXMLController;
import jp.seraphyware.rmiexample.Downloader;
import jp.seraphyware.rmiexample.ErrorDialogUtils;
import jp.seraphyware.rmiexample.Message;
import jp.seraphyware.rmiexample.MyServerObject;
import jp.seraphyware.rmiexample.RMICustomSocketWatcher;
import jp.seraphyware.rmiexample.RemoteObjectHelper;
import jp.seraphyware.rmiexample.Uploader;
import jp.seraphyware.rmiexample.XMLResourceBundleControl;


/**
 * RMIクライアント(GUI)
 */
public class ClientMainController extends AbstractFXMLController
		implements Initializable {

	/**
	 * Lookupされているか示す
	 */
	private SimpleBooleanProperty lookuped = new SimpleBooleanProperty(this, "lookuped");

	/**
	 * リソースバンドル
	 */
	private ResourceBundle resource;

	/**
	 * リモートスタブ
	 */
	private MyServerObject remote;

	@FXML
	private Label txtStatus;

	@FXML
	private TextField txtURL;

	@FXML
	private TextField txtPort;

	@FXML
	private Button btnLookup;

	@FXML
	private Button btnSayHello;

	@FXML
	private Button btnSendFile;

	@FXML
	private Button btnRecvFile;

	@FXML
	private Button btnDispose;

	@FXML
	private Button btnShutdown;

	/**
	 * FXMLが読み込まれコントローラとフィールドが結び付けられた後で、
	 * FXMLLoaderより呼び出される.
	 */
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

		txtStatus.textProperty().bind(strNumOfSocketCounts);

		RemoteObjectHelper helper = RemoteObjectHelper.getInstance();
		RMICustomSocketWatcher socketWatcher = helper.getSocketWatcher();

		socketWatcher.setNumOfSocketsListener((server, client) -> {
			Platform.runLater(() -> {
				numOfServerSocket.set(server);
				numOfClientSocket.set(client);
			});
		});

		// テキストフィールドの制御
		txtPort.textProperty().addListener((ob, oldValue, newValue)->{
			if (!oldValue.equals(newValue) && !newValue.matches("[0-9]+")) {
				txtPort.setText(oldValue);
			}
		});

		// ボタンの制御
		btnLookup.disableProperty().bind(lookuped);
		btnSayHello.disableProperty().bind(lookuped.not());
		btnSendFile.disableProperty().bind(lookuped.not());
		btnRecvFile.disableProperty().bind(lookuped.not());
		btnShutdown.disableProperty().bind(lookuped.not());
		btnDispose.disableProperty().bind(lookuped.not());

		// 初期値
//		Preferences prefs = Preferences.userRoot()
//				.node(System.getProperty("app.preferences.id"))
//				.node("JVMUserOptions");
//		String minHeap = prefs.get("-Xms", null);
//		String maxHeap = prefs.get("-Xmx", null);
//		Alert alert = new Alert(AlertType.INFORMATION);
//		alert.setHeaderText(minHeap + "/" + maxHeap);
//		alert.show();

		txtPort.setText(Integer.toString(Registry.REGISTRY_PORT));
		txtURL.setText("localhost");

//		RMICustomClientSocketFactory.setClientSocketListener(l);
	}

	protected Stage makeStage() {
		Stage stage = super.makeStage();
		stage.setTitle(resource.getString("title"));
		return stage;
	}

	@Override
	protected Parent makeRoot() {
		// リソース
		resource = ResourceBundle.getBundle(
				ClientMainController.class.getName(), new XMLResourceBundleControl());

		// FXMLファイルとリソースバンドルより画面を構成する
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("ClientMain.fxml"));
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

	/**
	 * Lookupボタン押下時
	 * @param event
	 */
	@FXML
	protected void handleLookup(ActionEvent event) {
		try {
			String url = txtURL.getText();
			int port = Integer.parseInt(txtPort.getText());

			RemoteObjectHelper helper = RemoteObjectHelper.getInstance();

			// RMIレジストリを取得する.
			Registry registry = helper.getRegistry(url, port);

			// サーバオブジェクトを取得する.
			this.remote = (MyServerObject) registry
					.lookup(MyServerObject.class.getName());

			lookuped.set(true);

		} catch (Exception ex) {
			lookuped.set(false);
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}

	/**
	 * SayHelloボタン押下時
	 * @param event
	 */
	@FXML
	protected void handleSayHello(ActionEvent event) {
		Message message = new Message();
		message.setTime(LocalDateTime.now());
		message.setMessage("FROM-CLIENT!");
		RemoteAction.doRun(remote, remote -> remote.echo(message),
				getStage());
	}

	/**
	 * ファイルの送信テスト(擬似)
	 * @param event
	 */
	@FXML
	protected void handleSendFile(ActionEvent event) {
		try {
			String fileName = "UploadFile" + System.currentTimeMillis() + ".txt";

			try (Uploader uploader = remote.upload(fileName)) {
				try {
					for (int idx = 0; idx < 10; idx++) {
						uploader.write(("hello, world!" + idx).getBytes());
					}

				} catch (Exception ex) {
					uploader.cancel(ex);
				}
			}

		} catch (IOException ex) {
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}

	/**
	 * ファイルの受信テスト(擬似)
	 * @param event
	 */
	@FXML
	protected void handleRecvFile(ActionEvent event) {
		try {
			String fileName = "DownloadFile" + System.currentTimeMillis() + ".txt";

			try (Downloader downloader = remote.download(fileName)) {
				try {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					for (;;) {
						byte[] data = downloader.read();
						if (data == null) {
							break;
						}
						bos.write(data);
					}
					System.out.println(new String(bos.toByteArray()));

				} catch (Exception ex) {
					downloader.cancel(ex);
				}
			}

		} catch (IOException ex) {
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}

	@FXML
	protected void handleDispose(ActionEvent event) {
		remote = null;
		lookuped.set(false);
	}

	/**
	 * Shutdownボタン押下時
	 * @param event
	 */
	@FXML
	protected void handleShutdown(ActionEvent event) {
		CompletableFuture<Void> future = RemoteAction.doRun(remote,
				remote -> System.out.println(remote.shutdown()), getStage());
		future.whenComplete((val, ex) -> {
			Platform.runLater(() -> {
				if (ex != null) {
					ErrorDialogUtils.showException(getStage(), ex);

				} else {
					// 正常終了した場合はサーバ側が停止している
					lookuped.set(false);
				}
			});
		});
	}

	/**
	 * GCを行う.
	 */
	@FXML
	protected void handleGC() {
		try {
			for (int idx = 0; idx < 3; idx++) {
				System.gc();
				Thread.sleep(300);
			}
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}
}

