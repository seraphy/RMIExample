package jp.seraphyware.rmiexample.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.stage.Stage;

public class ClientMain extends Application {

	/**
	 * ロガー
	 */
	private static final Logger log = Logger
			.getLogger(ClientMain.class.getName());

	@Override
	public void init() throws Exception {
		// ロガーの設定
		initLogger();

		// セキュリティポリシーの適用
		URL policy = ClientMain.class.getResource("security.policy");
		System.setProperty("java.security.policy", policy.toExternalForm());
		System.setProperty("java.security.debug:failure", "access");

		// セキュリティマネージャの有効化
		System.setSecurityManager(new SecurityManager());

		// BgJobスレッドの開始
		BgExecutor.getInstance().start();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		ClientMainController mainWnd = new ClientMainController();
		Stage stg = mainWnd.getStage();
		stg.show();
	}

	@Override
	public void stop() throws Exception {
		// ジョブの停止
		BgExecutor.getInstance().stop();

		// 分散GCを早期実施させるため明示的にGCを行う.
		for (int idx = 0; idx < 3; idx++) {
			System.gc();
			Thread.sleep(100);
		}

		// プロセスを終了する.
		log.info("Process Exit.");
		System.exit(0);
	}

	/**
	 * ロガーの設定を行う.
	 */
	private static void initLogger() {
		try (InputStream is = ClientMain.class
				.getResourceAsStream("logging.properties")) {
			LogManager logManager = LogManager.getLogManager();
			logManager.readConfiguration(is);

		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public static void main(String... args) {
		launch(args);
	}
}
