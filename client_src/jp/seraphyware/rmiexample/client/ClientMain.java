package jp.seraphyware.rmiexample.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.logging.LogManager;

import javafx.application.Application;
import javafx.stage.Stage;

public class ClientMain extends Application {

	@Override
	public void init() throws Exception {
		initLogger();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		ClientMainController mainWnd = new ClientMainController();
		Stage stg = mainWnd.getStage();
		stg.show();
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
