package jp.seraphyware.rmiexample;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * 公開オブジェクト用インターフェイス
 */
public interface RMIServer extends Remote {

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
	 * クライアントからサーバーにデータを送信する
	 * @param name
	 * @param is
	 * @throws RemoteException
	 */
	void send(String name, RMIInputStream is) throws RemoteException;

	/**
	 * サーバーからクライアントにデータを受信する
	 * @param name
	 * @param is
	 * @throws RemoteException
	 */
	void recv(String name, RMIOutputStream is) throws RemoteException;

	/**
	 * サーバを停止する
	 * @return
	 * @throws RemoteException
	 */
	String shutdown() throws RemoteException;
}
