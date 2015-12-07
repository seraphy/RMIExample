package jp.seraphyware.rmiexample.client;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class BgExecutor implements Executor {

	private ExecutorService bgExecutor;

	private static final BgExecutor inst = new BgExecutor();

	private BgExecutor() {
		super();
	}

	public static BgExecutor getInstance() {
		return inst;
	}

	public void start() {
		if (bgExecutor == null) {
			bgExecutor = Executors.newCachedThreadPool();
		}
	}

	public void stop() {
		if (bgExecutor != null) {
			bgExecutor.shutdownNow();
			try {
				bgExecutor.awaitTermination(1, TimeUnit.MINUTES);

			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}

			bgExecutor = null;
		}
	}

	@Override
	public void execute(Runnable command) {
		if (bgExecutor == null) {
			throw new IllegalStateException();
		}
		bgExecutor.execute(command);
	}
}
