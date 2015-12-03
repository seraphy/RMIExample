package jp.seraphyware.rmiexample.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jp.seraphyware.rmiexample.AbstractFXMLController;
import jp.seraphyware.rmiexample.ErrorDialogUtils;
import jp.seraphyware.rmiexample.Message;
import jp.seraphyware.rmiexample.RMICustomClientSocketFactory;
import jp.seraphyware.rmiexample.RMICustomServerSocketFactory;
import jp.seraphyware.rmiexample.RMICustomSocketWatcher;
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
	private Button btnSimpleCallback;

	@FXML
	private Button btnSendFile;

	@FXML
	private Button btnRecvFile;

	@FXML
	private Button btnDispose;

	@FXML
	private Button btnShutdown;

	/**
	 * サーバソケットファクトリ
	 */
	private final RMICustomServerSocketFactory serverSocketFactory = new RMICustomServerSocketFactory();

	/**
	 * ソケットファクトリ
	 */
	private final RMICustomClientSocketFactory clientSocketFactory = new RMICustomClientSocketFactory();

	/**
	 * ソケット数監視
	 */
	private final RMICustomSocketWatcher socketWatcher = new RMICustomSocketWatcher();

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

		socketWatcher.setNumOfSocketsListener((server, client) -> {
			Platform.runLater(() -> {
				numOfServerSocket.set(server);
				numOfClientSocket.set(client);
			});
		});

		RMICustomServerSocketFactory.setServerSocketListener(socketWatcher);
		RMICustomClientSocketFactory.setClientSocketListener(socketWatcher);

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
			Registry registry = LocateRegistry.getRegistry(url, port,
					clientSocketFactory);
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

		public RMIExampleCallbackImpl(int port,
				RMIClientSocketFactory clientSocketFactory,
				RMIServerSocketFactory serverSocketFactory)
						throws RemoteException {
			super(0, clientSocketFactory, serverSocketFactory);
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
			RMIExampleCallbackImpl callback = new RMIExampleCallbackImpl(
					0, clientSocketFactory, serverSocketFactory) {
				private static final long serialVersionUID = -7693543679821540221L;

				@Override
				public void callback(Message message) throws RemoteException {
					Platform.runLater(() -> {
						Alert alert = new Alert(AlertType.INFORMATION);
						alert.initOwner(getStage());
						alert.initModality(Modality.NONE);
						alert.setHeaderText("recv");
						alert.setContentText(message.toString());
						alert.show();
					});
				}
			};

			// コールバック
			CompletableFuture<Void> future = RemoteAction.doRun(remote,
					remote -> remote.doCallback("Client", callback),
					getStage());

			future.whenComplete((val, ex) -> {
				// クライアント側が保持しないことが明らかであれば、
				// コールバック終了後にエクスポートを解除する.
				// (unexportObjectしないと残りつづけるので注意)
				Platform.runLater(() -> {
					try {
						System.out.println("★RMIExampleCallback unexportObject");
						UnicastRemoteObject.unexportObject(callback, false);

					} catch (Exception ex2) {
						ErrorDialogUtils.showException(getStage(), ex2);
					}
				});
			});

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
			RMIInputStream ris = new RMIInputStreamImpl(bis, 0,
					clientSocketFactory, serverSocketFactory) {
				private static final long serialVersionUID = 3432605918256177087L;

				@Override
				public void close() throws IOException {
					super.close();

					StackTraceElement[] callers = Thread.currentThread()
							.getStackTrace();
					String stacktrace = Arrays.stream(callers)
							.map(caller -> caller.toString())
							.collect(Collectors.joining("\r\n"));

					Platform.runLater(() -> {
						Alert alert = new Alert(AlertType.INFORMATION);
						alert.setHeaderText("closed inst=" + this);
						alert.setContentText(stacktrace);
						alert.initModality(Modality.NONE);
						alert.show();
					});
				}
			};
			CompletableFuture<Void> future = RemoteAction.doRun(remote,
					remote -> remote.send("sendFile", ris), getStage());
			future.whenComplete((val, ex) -> {
				Platform.runLater(() -> {
					if (ex != null) {
						ErrorDialogUtils.showException(getStage(), ex);

					} else {
						Alert alert = new Alert(AlertType.INFORMATION);
						alert.setHeaderText("Send Complete");
						alert.showAndWait();
					}
				});
			});

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
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				RMIOutputStream ros = new RMIOutputStreamImpl(bos, 0,
						clientSocketFactory, serverSocketFactory) {
					private static final long serialVersionUID = -5981618635857282823L;

					@Override
					public void close() throws IOException {
						super.close();

						StackTraceElement[] callers = Thread.currentThread()
								.getStackTrace();
						String stacktrace = Arrays.stream(callers)
								.map(caller -> caller.toString())
								.collect(Collectors.joining("\r\n"));

						Platform.runLater(() -> {
							Alert alert = new Alert(AlertType.INFORMATION);
							alert.setHeaderText("closed inst=" + this);
							alert.setContentText(stacktrace);
							alert.initModality(Modality.NONE);
							alert.show();
						});
					}
				};
				CompletableFuture<Void> future = RemoteAction.doRun(remote,
						remote -> remote.recv("recvFile", ros), getStage());
				future.whenComplete((val, ex) -> {
					Platform.runLater(() -> {
						if (ex != null) {
							ErrorDialogUtils.showException(getStage(), ex);

						} else {
							String msg = new String(bos.toByteArray());
							Alert alert = new Alert(AlertType.INFORMATION);
							alert.setContentText("recv: " + msg);
							alert.showAndWait();
						}

						try {
							ros.close();
						} catch (Exception ex2) {
							ErrorDialogUtils.showException(getStage(), ex2);
						}
					});
				});

			} catch (IOException ex) {
				ErrorDialogUtils.showException(getStage(), ex);
			}
		}).start();
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
}

