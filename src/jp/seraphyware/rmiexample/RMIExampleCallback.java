package jp.seraphyware.rmiexample;

import java.rmi.Remote;
import java.rmi.RemoteException;

@FunctionalInterface
public interface RMIExampleCallback extends Remote  {

	void callback(Message message) throws RemoteException;

}
