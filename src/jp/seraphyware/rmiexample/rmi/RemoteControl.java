package jp.seraphyware.rmiexample.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteControl extends Remote {

	/**
	 * Hello, Worldをサーバー上で表示する.
	 * @throws RemoteException
	 */
	void echo(Message message) throws RemoteException;

	/**
	 * サーバを停止する
	 * @return
	 * @throws RemoteException
	 */
	String shutdown() throws RemoteException;
}
