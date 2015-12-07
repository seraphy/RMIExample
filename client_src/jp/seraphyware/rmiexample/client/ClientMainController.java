package jp.seraphyware.rmiexample.client;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventType;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jp.seraphyware.rmiexample.rmi.Downloader;
import jp.seraphyware.rmiexample.rmi.Message;
import jp.seraphyware.rmiexample.rmi.RMICustomSocketWatcher;
import jp.seraphyware.rmiexample.rmi.RemoteControl;
import jp.seraphyware.rmiexample.rmi.RemoteFileIO;
import jp.seraphyware.rmiexample.rmi.RemoteObjectHelper;
import jp.seraphyware.rmiexample.rmi.Uploader;
import jp.seraphyware.rmiexample.ui.AbstractFXMLController;
import jp.seraphyware.rmiexample.ui.ErrorDialogUtils;
import jp.seraphyware.rmiexample.ui.ProgressDialogController;
import jp.seraphyware.rmiexample.util.XMLResourceBundleControl;

/**
 * RMIクライアント(GUI)
 */
public class ClientMainController extends AbstractFXMLController
		implements Initializable {

	/**
	 * Lookupされているか示す
	 */
	private SimpleBooleanProperty lookuped = new SimpleBooleanProperty(this,
			"lookuped");

	/**
	 * リソースバンドル
	 */
	private ResourceBundle resource;

	/**
	 * RemoteFileIOのリモートスタブ
	 */
	private RemoteFileIO remoteFileIO;

	/**
	 * RemoteControlのリモートスタブ
	 */
	private RemoteControl remoteControl;

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
	private Button btnListFiles;

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
		txtPort.textProperty().addListener((ob, oldValue, newValue) -> {
			if (!oldValue.equals(newValue) && !newValue.matches("[0-9]+")) {
				txtPort.setText(oldValue);
			}
		});

		// ボタンの制御
		btnLookup.disableProperty().bind(lookuped);
		btnSayHello.disableProperty().bind(lookuped.not());
		btnListFiles.disableProperty().bind(lookuped.not());
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
				ClientMainController.class.getName(),
				new XMLResourceBundleControl());

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
			this.remoteFileIO = (RemoteFileIO) registry
					.lookup(RemoteFileIO.class.getName());

			this.remoteControl = (RemoteControl) registry
					.lookup(RemoteControl.class.getName());

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
		RemoteAction.doRun(remoteControl,
				remoteControl -> remoteControl.echo(message),
				getStage());
	}

	@FXML
	protected void handleListFiles(ActionEvent event) {
		try {
			List<String> files = remoteFileIO.getFiles();
			String msg = files.stream().collect(Collectors.joining("\r\n"));

			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setHeaderText("GetFiles");

			TextArea textArea = new TextArea();
			textArea.setText(msg);
			//alert.getDialogPane().setExpandableContent(textArea);
			alert.getDialogPane().setContent(textArea);

			alert.showAndWait();

		} catch (IOException ex) {
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}

	/**
	 * FileChooseで最後に使ったディレクトリ
	 */
	private File lastUseDir;

	/**
	 * ファイルの送信テスト
	 * @param event
	 */
	@FXML
	protected void handleSendFile(ActionEvent event) {
		try {
			FileChooser fileChooser = new FileChooser();
			if (lastUseDir != null && lastUseDir.isDirectory()) {
				fileChooser.setInitialDirectory(lastUseDir);
			}
			File selectedFile = fileChooser.showOpenDialog(getStage());
			if (selectedFile == null) {
				return;
			}

			String fileName = selectedFile.getName();

			ProgressDialogController progressCtrl = new ProgressDialogController();
			progressCtrl.setParent(getStage());
			Task<Void> worker = new Task<Void>() {
				protected Void call() throws Exception {
					long total = selectedFile.length();
					long writeSize = 0;
					try (FileChannel channel = (FileChannel) Files
							.newByteChannel(selectedFile.toPath(),
									StandardOpenOption.READ)) {
						byte[] buf = new byte[4096];
						ByteBuffer byteBuf = ByteBuffer.wrap(buf);
						try (Uploader uploader = remoteFileIO
								.upload(fileName)) {
							try {
								for (;;) {
									byteBuf.clear();
									int len = channel.read(byteBuf);
									writeSize += len;

									updateProgress(writeSize, total);
									updateMessage(writeSize + "/" + total);

									if (isCancelled()) {
										throw new RuntimeException("Cancel");
									}

									if (len < 0) {
										break;
									}
									if (len == byteBuf.capacity()) {
										System.out.println("*FULL*");
										uploader.write(buf);

									} else if (len > 0) {
										System.out.println("*LESS*");
										// TCP/IPに不要なバイトを流すほうが、
										// 配列を作り直すよりコストが高いと思われるため。
										byte[] actualsizeBuf = new byte[len];
										System.arraycopy(buf, 0,
												actualsizeBuf, 0, len);
										uploader.write(actualsizeBuf);
									}
								}
								System.out.println("*COMPLETE*");

							} catch (Exception ex) {
								uploader.cancel(ex);
							}
						}
					}
					return null;
				}
			};

			worker.addEventHandler(WorkerStateEvent.ANY, evt -> {
				EventType<? extends Event> typ = evt.getEventType();
				if (typ.equals(WorkerStateEvent.WORKER_STATE_CANCELLED) ||
						typ.equals(WorkerStateEvent.WORKER_STATE_SUCCEEDED) ||
						typ.equals(WorkerStateEvent.WORKER_STATE_FAILED)) {
					progressCtrl.close();
				}

				Throwable ex = worker.getException();
				if (ex != null) {
					ErrorDialogUtils.showException(getStage(), ex);
				}
			});

			progressCtrl.titleProperty().bind(worker.titleProperty());
			progressCtrl.messageProperty().bind(worker.messageProperty());
			progressCtrl.progressProperty().bind(worker.progressProperty());
			progressCtrl.setOnCancelAction(evt -> worker.cancel());

			progressCtrl.show();

			BgExecutor.getInstance().execute(worker);

		} catch (Exception ex) {
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}

	private RecvDialogController recvDialog;

	/**
	 * ファイルの受信テスト
	 * @param event
	 */
	@FXML
	protected void handleRecvFile(ActionEvent event) {
		try {
			if (recvDialog == null) {
				recvDialog = new RecvDialogController();
				recvDialog.setParent(getStage());
			}

			recvDialog.setFiles(remoteFileIO.getFiles());

			Optional<RecvDialogController.Result> result = recvDialog
					.showAndWait();
			if (!result.isPresent()) {
				return;
			}
			String fileName = result.get().getFileName();
			File outFile = result.get().getOutFile();

			ProgressDialogController progressCtrl = new ProgressDialogController();
			progressCtrl.setParent(getStage());

			Task<Void> worker = new Task<Void>() {
				protected Void call() throws Exception {
					long total = 0;
					try (OutputStream bos = new BufferedOutputStream(
							new FileOutputStream(outFile))) {
						try (Downloader downloader = remoteFileIO
								.download(fileName)) {
							try {
								for (;;) {
									updateMessage(Long.toString(total));
									if (isCancelled()) {
										throw new RuntimeException("Cancel");
									}
									byte[] data = downloader.read();
									if (data == null) {
										break;
									}
									bos.write(data);
									total += data.length;
								}

							} catch (Exception ex) {
								downloader.cancel(ex);
							}
						}
					}
					return null;
				};
			};
			worker.addEventHandler(WorkerStateEvent.ANY, evt -> {
				EventType<? extends Event> typ = evt.getEventType();
				if (typ.equals(WorkerStateEvent.WORKER_STATE_CANCELLED) ||
						typ.equals(WorkerStateEvent.WORKER_STATE_SUCCEEDED) ||
						typ.equals(WorkerStateEvent.WORKER_STATE_FAILED)) {
					progressCtrl.close();
				}

				Throwable ex = worker.getException();
				if (ex != null) {
					ErrorDialogUtils.showException(getStage(), ex);
				}
			});

			progressCtrl.titleProperty().bind(worker.titleProperty());
			progressCtrl.messageProperty().bind(worker.messageProperty());
			progressCtrl.progressProperty().bind(worker.progressProperty());
			progressCtrl.setOnCancelAction(evt -> worker.cancel());

			progressCtrl.show();

			BgExecutor.getInstance().execute(worker);

		} catch (Exception ex) {
			ErrorDialogUtils.showException(getStage(), ex);
		}
	}

	@FXML
	protected void handleDispose(ActionEvent event) {
		remoteFileIO = null;
		remoteControl = null;
		lookuped.set(false);
	}

	/**
	 * Shutdownボタン押下時
	 * @param event
	 */
	@FXML
	protected void handleShutdown(ActionEvent event) {
		CompletableFuture<Void> future = RemoteAction.doRun(remoteControl,
				remoteControl -> System.out.println(remoteControl.shutdown()),
				getStage());
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
