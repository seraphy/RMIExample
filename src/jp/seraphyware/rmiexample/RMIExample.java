package jp.seraphyware.rmiexample;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * 公開オブジェクト用インターフェイス
 */
public interface RMIExample extends Remote {

	/**
	 * Hello, Worldをサーバー上で表示する.
	 * @throws RemoteException
	 */
	void sayHello(Message message) throws RemoteException;

	/**
	 * サーバからコールバックを返す.
	 * @param msg メッセージ
	 * @param callback コールバック
	 * @throws RemoteException
	 */
	void doCallback(String msg, RMIExampleCallback callback) throws RemoteException;

	/**
	 * サーバを停止する
	 * @return
	 * @throws RemoteException
	 */
	String shutdown() throws RemoteException;
}
