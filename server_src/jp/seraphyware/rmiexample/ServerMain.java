package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.logging.LogManager;

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
	 * プライベートコンストラクタ
	 */
	private ServerMain() {
		super();
	}

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

		registry = LocateRegistry.createRegistry(port);

		obj = new RMIExampleObject();
		RMIServer stub = (RMIServer) UnicastRemoteObject.exportObject(obj, 0);
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
	 * ロガーの設定を行う.
	 */
	private static void initLogger() {
		try (InputStream is = ServerMain.class
				.getResourceAsStream("logging.properties")) {
			LogManager logManager = LogManager.getLogManager();
			logManager.readConfiguration(is);

		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * ローカル上の自分自身のRMIに接続するテスト.
	 * @param port
	 */
	private static void testLocal(int port) {
		try {
			Registry registry = LocateRegistry.getRegistry(port);
			RMIServer example = (RMIServer) registry.lookup("RMIExample");
			Message message = new Message();
			message.setTime(LocalDateTime.now());
			message.setMessage("★FROM LOCAL★");
			example.sayHello(message);

		} catch (RemoteException | NotBoundException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * エントリポイント
	 * @param args
	 * @throws Exception
	 */
	public static void main(String... args) throws Exception {
		initLogger();

		int port = Registry.REGISTRY_PORT;
		if (args.length >= 1) {
			port = Integer.parseInt(args[0]);
		}

		getInstance().start(port);

		testLocal(port);
	}
}
