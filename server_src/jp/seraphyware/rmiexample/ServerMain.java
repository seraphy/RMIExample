package jp.seraphyware.rmiexample;

import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * RMIサーバー
 */
public final class ServerMain {

	/**
	 * シングルトン
	 */
	public static ServerMain inst = new ServerMain();

	/**
	 * シングルトンの取得
	 * @return
	 */
	public static ServerMain getInstance() {
		return inst;
	}

	/**
	 * RMIレジストリ(ローカルマシン上)
	 */
	private Registry registry;

	/**
	 * 公開したオブジェクト
	 */
	private RMIExampleObject obj;


	/**
	 * オブジェクトを公開し、RMIレジストリも作成し公開する.
	 * @param port
	 * @throws RemoteException
	 * @throws AlreadyBoundException
	 */
	public void start(int port) throws RemoteException, AlreadyBoundException {
		if (registry != null) {
			throw new IllegalStateException();
		}

		obj = new RMIExampleObject();
		obj.sayHello();

		RMIExample stub = (RMIExample) UnicastRemoteObject.exportObject(obj, 0);

		registry = LocateRegistry.createRegistry(port);
		registry.bind("RMIExample", stub);
		System.out.println("★start");
	}

	/**
	 * 公開オブジェクトを取り下げる.
	 * @throws NoSuchObjectException
	 */
	public void stop() throws NoSuchObjectException {
		if (registry == null) {
			return;
		}
		try {
			// オブジェクトの取り下げ
			UnicastRemoteObject.unexportObject(obj, true);
			obj = null;

			// RMIレジストリの公開取り下げ
			UnicastRemoteObject.unexportObject(registry, true);
			registry = null;

			System.out.println("★shutdown");
			// メインスレッドはすでに終了済みであるため、
			// RMIの待機スレッドが解放されて、このプログラムは終了する.

		} catch (NoSuchObjectException ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	/**
	 * エントリポイント
	 * @param args
	 * @throws Exception
	 */
	public static void main(String... args) throws Exception {
		int port = Registry.REGISTRY_PORT;
		if (args.length >= 1) {
			port = Integer.parseInt(args[0]);
		}
		getInstance().start(port);
	}
}
