package jp.seraphyware.rmiexample;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * 引数として送り込むリモートコールバック用インターフェイス
 */
@FunctionalInterface
public interface RMIExampleCallback extends Remote  {

	void callback(Message message) throws RemoteException;
}
