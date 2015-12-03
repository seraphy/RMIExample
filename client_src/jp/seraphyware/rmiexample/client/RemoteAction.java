package jp.seraphyware.rmiexample.client;

import java.rmi.RemoteException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import javafx.application.Platform;
import javafx.stage.Window;
import jp.seraphyware.rmiexample.ErrorDialogUtils;

/**
 * リモート呼び出し用ヘルパ
 * @param <T>
 */
@FunctionalInterface
public interface RemoteAction<T> {

	/**
	 * リモートのアクション
	 * @param remote
	 * @throws RemoteException
	 */
	void run(T remote) throws RemoteException;

	/**
	 * リモート呼び出しを行う.<br>
	 * リモート例外が発生した場合はエラーダイアログを表示する.
	 * @param remote リモート
	 * @param owner エラーダイアログを表示する場合のオーナー(null可)
	 */
	default CompletableFuture<Void> action(T remote, Window owner) {
		if (remote == null) {
			throw new IllegalStateException();
		}
		CompletableFuture<Void> future = new CompletableFuture<>();
		ForkJoinPool.commonPool().execute(() -> {
			try {
				run(remote);
				future.complete(null);

			} catch (Throwable ex) {
				future.completeExceptionally(ex);
			}
		});
		future.whenComplete((value, ex) -> {
			if (ex != null) {
				Platform.runLater(() -> {
					ErrorDialogUtils.showException(owner, ex);
				});
			}
		});
		return future;
	}

	/**
	 * 指定されたアクションのリモート呼び出しを行う.<br>
	 * リモート例外が発生した場合はエラーダイアログを表示する.
	 * @param remote リモート
	 * @param action 実行するアクション
	 * @param ownerエラーダイアログを表示するオーナー(null可)
	 */
	static <T> CompletableFuture<Void> doRun(T remote, RemoteAction<T> action, Window owner) {
		return action.action(remote, owner);
	}
}
