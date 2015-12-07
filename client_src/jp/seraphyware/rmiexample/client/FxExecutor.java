package jp.seraphyware.rmiexample.client;

import java.util.concurrent.Executor;

import javafx.application.Platform;

public final class FxExecutor implements Executor {

	private static final FxExecutor inst = new FxExecutor();

	private FxExecutor() {
		super();
	}

	public static FxExecutor getInstance() {
		return inst;
	}

	@Override
	public void execute(Runnable command) {
		Platform.runLater(command);
	}
}
