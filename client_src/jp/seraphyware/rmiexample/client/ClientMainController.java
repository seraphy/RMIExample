package jp.seraphyware.rmiexample.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.UUID;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jp.seraphyware.rmiexample.AbstractFXMLController;
import jp.seraphyware.rmiexample.ErrorDialogUtils;
import jp.seraphyware.rmiexample.Message;
import jp.seraphyware.rmiexample.RMICustomClientSocketFactory;
import jp.seraphyware.rmiexample.RMIExampleCallback;
import jp.seraphyware.rmiexample.RMIInputStream;
import jp.seraphyware.rmiexample.RMIInputStreamImpl;
import jp.seraphyware.rmiexample.RMIOutputStream;
import jp.seraphyware.rmiexample.RMIOutputStreamImpl;
import jp.seraphyware.rmiexample.RMIServer;
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
	private ResourceBundle resource = ResourceBundle.getBundle(
			getClass().getName(), new XMLResourceBundleControl());

	/**
	 * リモートスタブ
	 */
	private RMIServer remote;

	@FXML
	TextField txtURL;

	@FXML
	TextField txtPort;

	@FXML
	Button btnLookup;

	@FXML
	Button btnSayHello;

	@FXML
	Button btnSimpleCallback;

	@FXML
	Button btnSendFile;

	@FXML
	Button btnRecvFile;

	@FXML
	Button btnShutdown;

	/**
	 * FXMLが読み込まれコントローラとフィールドが結び付けられた後で、
	 * FXMLLoaderより呼び出される.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// テキストフィールドの制御
		txtPort.textProperty().addListener((ob, oldValue, newValue)->{
			if (!oldValue.equals(newValue) && !newValue.matches("[0-9]+")) {
				txtPort.setText(oldValue);
			}
		});

		// ボタンの制御
		btnLookup.disableProperty().bind(lookuped);
		btnSayHello.disableProperty().bind(lookuped.not());
		btnSimpleCallback.disableProperty().bind(lookuped.not());
		btnSendFile.disableProperty().bind(lookuped.not());
		btnRecvFile.disableProperty().bind(lookuped.not());
		btnShutdown.disableProperty().bind(lookuped.not());

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

		RMICustomClientSocketFactory.setClientFactoryHandler(factory -> {
			UUID uuid = factory.getUUID();
			Platform.runLater(() -> {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Create/Deserializaed ClientSocketFactory");
				alert.setHeaderText("uuid=" + uuid);
				alert.initModality(Modality.NONE);
				alert.show();
			});
		});
	}

	protected Stage makeStage() {
		Stage stage = super.makeStage();
		stage.setTitle(resource.getString("title"));
		return stage;
	}

	@Override
	protected Parent makeRoot() {
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
			Registry registry = LocateRegistry.getRegistry(url, port);
			this.remote = (RMIServer) registry.lookup("RMIExample");
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
		RemoteAction.doRun(remote, remote -> remote.sayHello(message),
				getStage());
	}


	/**
	 * コールバック用クラス.
	 * クライアント側から参照が切れた場合に通知を受ける.
	 */
	public static abstract class RMIExampleCallbackImpl extends UnicastRemoteObject
		implements RMIExampleCallback, Unreferenced {
		private static final long serialVersionUID = 7419957109601519663L;

		public RMIExampleCallbackImpl() throws RemoteException {
			super(0);
		}

		@Override
		public void unreferenced() {
			System.out.println("★★★unreferenced: " + this);
			try {
				// クライアントからunreferenceされたら、アンエクスポートする.
				// (※ 参照カウントが０になるたびに何度でも呼び出される)
				UnicastRemoteObject.unexportObject(this, false);

			} catch (RemoteException ex) {
				ex.printStackTrace();
			}
		}
	}


	/**
	 * Callbackボタン押下時
	 * @param event
	 */
	@FXML
	protected void handleDoCallback(ActionEvent event) {
		try {
			// コールバック用のオブジェクトを作成しエクスポートする.
			// (クライアントからunreferenceされたら、アンエクスポートする.)
			RMIExampleCallbackImpl callback = new RMIExampleCallbackImpl() {
				private static final long serialVersionUID = -7693543679821540221L;

				@Override
				public void callback(Message message) throws RemoteException {
					System.out.println("★RMIExampleCallback=" + message);
				}
			};

			// コールバック
			RemoteAction.doRun(remote,
					remote -> remote.doCallback("Client", callback),
					getStage());

			// クライアント側が保持しないことが明らかであれば、
			// コールバック終了後にエクスポートを解除してもよい
			//UnicastRemoteObject.unexportObject(callback, false);

		} catch (RemoteException ex) {
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}

	/**
	 * ファイルの送信テスト(擬似)
	 * @param event
	 */
	@FXML
	protected void handleSendFile(ActionEvent event) {
		ByteArrayInputStream bis = new ByteArrayInputStream("hello, world".getBytes());
		try {
			RMIInputStream ris = new RMIInputStreamImpl(bis);
			RemoteAction.doRun(remote, remote -> remote.send("sendFile", ris),
					getStage());

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
		new Thread(()->{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				try (RMIOutputStream ros = new RMIOutputStreamImpl(bos)) {
					RemoteAction.doRun(remote,
							remote -> remote.recv("recvFile", ros), getStage());
				}
				String msg = new String(bos.toByteArray());
				System.out.println("recv: " + msg);

			} catch (IOException ex) {
				ErrorDialogUtils.showException(getStage(), ex);
			}
		}).start();
	}

	/**
	 * Shutdownボタン押下時
	 * @param event
	 */
	@FXML
	protected void handleShutdown(ActionEvent event) {
		RemoteAction.doRun(remote,
				remote -> System.out.println(remote.shutdown()), getStage());

		// 正常終了した場合はサーバ側が停止している
		lookuped.set(false);
	}
}

