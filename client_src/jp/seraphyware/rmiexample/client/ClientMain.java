package jp.seraphyware.rmiexample.client;

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
public class ClientMain extends Application {

	/**
	 * Lookupされているか示す
	 */
	private SimpleBooleanProperty lookuped = new SimpleBooleanProperty();

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

	@Override
	public void start(Stage primaryStage) throws Exception {
		// FXMLファイルとリソースバンドルより画面を構成する
		ResourceBundle resource = ResourceBundle.getBundle(getClass().getName());
		FXMLLoader loader = new FXMLLoader(getClass().getResource(
				getClass().getSimpleName() + ".fxml"), resource);
		loader.setControllerFactory((Class<?> cls) -> {
			// コントローラは、このインスタンス自身を使用する.
			return this;
		});
		Parent root = (Parent) loader.load();

		// ボタンの制御
		btnLookup.setOnAction(this::handleLookup);
		btnSayHello.setOnAction(this::handleSayHello);
		btnSimpleCallback.setOnAction(this::handleDoLambda);
		btnShutdown.setOnAction(this::handleShutdown);

		btnLookup.disableProperty().bind(lookuped);
		btnSayHello.disableProperty().bind(lookuped.not());
		btnSimpleCallback.disableProperty().bind(lookuped.not());
		btnShutdown.disableProperty().bind(lookuped.not());

		// 初期値
		txtPort.setText(Integer.toString(Registry.REGISTRY_PORT));
		txtURL.setText("localhost");

		// 表示
		primaryStage.setScene(new Scene(root));
		primaryStage.show();
	}

	/**
	 * Lookupボタン押下時
	 * @param event
	 */
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
	protected void handleSayHello(ActionEvent event) {
		Message message = new Message();
		message.setTime(LocalDateTime.now());
		message.setMessage("FROM-CLIENT!");
		handleAction(remote -> remote.sayHello(message));
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
				UnicastRemoteObject.unexportObject(this, true);

			} catch (RemoteException ex) {
				ex.printStackTrace();
			}
		}
	}


	/**
	 * Callbackボタン押下時
	 * @param event
	 */
	protected void handleDoLambda(ActionEvent event) {
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
			handleAction(remote -> remote.doCallback("Client", callback));

		} catch (RemoteException ex) {
			ErrorDialogUtils.showException(ex);
		}
	}

	/**
	 * Shutdownボタン押下時
	 * @param event
	 */
	protected void handleShutdown(ActionEvent event) {
		handleAction(remote -> System.out.println(remote.shutdown()));

		// 正常終了した場合はサーバ側が停止している
		lookuped.set(false);
	}

	/**
	 * リモート呼び出し用ヘルパ
	 * @param <T>
	 */
	@FunctionalInterface
	private interface RemoteAction<T> {
		void run(T remote) throws RemoteException;
	}

	/**
	 * リモート呼び出しを行う.<br>
	 * リモート例外が発生した場合はエラーダイアログを表示する.
	 * @param remoteAction
	 */
	protected void handleAction(RemoteAction<RMIExample> remoteAction) {
		try {
			if (remote == null) {
				throw new IllegalStateException();
			}

			remoteAction.run(remote);

		} catch (Exception ex) {
			ErrorDialogUtils.showException(ex);
		}
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
