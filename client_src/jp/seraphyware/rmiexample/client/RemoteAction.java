package jp.seraphyware.rmiexample.client;

import java.rmi.RemoteException;

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
	default void action(T remote, Window owner) {
		try {
			if (remote == null) {
				throw new IllegalStateException();
			}

			run(remote);

		} catch (Exception ex) {
			ErrorDialogUtils.showException(owner, ex);
		}
	}

	/**
	 * 指定されたアクションのリモート呼び出しを行う.<br>
	 * リモート例外が発生した場合はエラーダイアログを表示する.
	 * @param remote リモート
	 * @param action 実行するアクション
	 * @param ownerエラーダイアログを表示するオーナー(null可)
	 */
	static <T> void doRun(T remote, RemoteAction<T> action, Window owner) {
		action.action(remote, owner);
	}
}
