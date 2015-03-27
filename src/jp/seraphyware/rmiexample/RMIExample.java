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
	void sayHello() throws RemoteException;

	/**
	 * サーバを停止する
	 * @return
	 * @throws RemoteException
	 */
	String shutdown() throws RemoteException;
}
