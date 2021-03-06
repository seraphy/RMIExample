package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * RMIサーバー
 */
public final class ServerMain extends Application {

	/**
	 * ロガー
	 */
	private static final Logger log = Logger
			.getLogger(ServerMain.class.getName());

	/**
	 * ロガーの設定を行う.
	 */
	private static void initLogger() {
		try (InputStream is = ServerMain.class
				.getResourceAsStream("logging.properties")) {
			LogManager logManager = LogManager.getLogManager();
			logManager.readConfiguration(is);

		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public void init() throws Exception {
		initLogger();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		ServerMainController mainWnd = new ServerMainController();
		Stage stage = mainWnd.getStage();
		stage.show();
	}

	@Override
	public void stop() throws Exception {
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
	 * エントリポイント
	 * @param args
	 * @throws Exception
	 */
	public static void main(String... args) throws Exception {
		launch(args);
	}
}
