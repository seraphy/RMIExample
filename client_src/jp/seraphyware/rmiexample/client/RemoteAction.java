package jp.seraphyware.rmiexample.client;

import java.rmi.RemoteException;

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
	 */
	default void action(T remote) {
		try {
			if (remote == null) {
				throw new IllegalStateException();
			}

			run(remote);

		} catch (Exception ex) {
			ErrorDialogUtils.showException(ex);
		}
	}

	/**
	 * 指定されたアクションのリモート呼び出しを行う.<br>
	 * リモート例外が発生した場合はエラーダイアログを表示する.
	 * @param remote リモート
	 * @param action 実行するアクション
	 */
	static <T> void doRun(T remote, RemoteAction<T> action) {
		action.action(remote);
	}
}
