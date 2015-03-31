package jp.seraphyware.rmiexample.client;

import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import jp.seraphyware.rmiexample.ErrorDialogUtils;
import jp.seraphyware.rmiexample.Message;
import jp.seraphyware.rmiexample.RMIExample;
import jp.seraphyware.rmiexample.RMIExampleCallback;


/**
 * RMIクライアント(GUI)
 */
public class ClientMain extends Application implements Initializable {

	/**
	 * Lookupされているか示す
	 */
	private SimpleBooleanProperty lookuped = new SimpleBooleanProperty(this, "lookuped");

	/**
	 * リモートスタブ
	 */
	private RMIExample remote;

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
		btnShutdown.disableProperty().bind(lookuped.not());

		// 初期値
		txtPort.setText(Integer.toString(Registry.REGISTRY_PORT));
		txtURL.setText("localhost");
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// FXMLファイルとリソースバンドルより画面を構成する
		ResourceBundle resource = ResourceBundle.getBundle(getClass().getName());
		FXMLLoader loader = new FXMLLoader(getClass().getResource(
				getClass().getSimpleName() + ".fxml"), resource);

		// コントローラは明示的に、このオブジェクトに関連づける
		loader.setController(this);

		Parent root = (Parent) loader.load();

		// 表示
		primaryStage.setScene(new Scene(root));
		primaryStage.show();
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
			this.remote = (RMIExample) registry.lookup("RMIExample");
			lookuped.set(true);

		} catch (Exception ex) {
			lookuped.set(false);
			ErrorDialogUtils.showException(ex);
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
		RemoteAction.doRun(remote, remote -> remote.sayHello(message));
	}


	/**
	 * コールバック用クラス.
	 * クライアント側から参照が切れた場合に通知を受ける.
	 */
	public static abstract class RMIExampleCallbackImpl extends UnicastRemoteObject
		implements RMIExampleCallback, Unreferenced {

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
				@Override
				public void callback(Message message) throws RemoteException {
					System.out.println("★RMIExampleCallback=" + message);
				}
			};

			// コールバック
			RemoteAction.doRun(remote, remote -> remote.doCallback("Client", callback));

			// クライアント側が保持しないことが明らかであれば、
			// コールバック終了後にエクスポートを解除してもよい
			//UnicastRemoteObject.unexportObject(callback, false);

		} catch (RemoteException ex) {
			ErrorDialogUtils.showException(ex);
		}
	}

	/**
	 * Shutdownボタン押下時
	 * @param event
	 */
	@FXML
	protected void handleShutdown(ActionEvent event) {
		RemoteAction.doRun(remote, remote -> System.out.println(remote.shutdown()));

		// 正常終了した場合はサーバ側が停止している
		lookuped.set(false);
	}

	/**
	 * エントリポイント
	 * @param args
	 * @throws Exception
	 */
	public static void main(String... args) throws Exception {
		launch(args);
	}
}

