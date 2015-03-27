package jp.seraphyware.rmiexample;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;

/**
 * 公開オブジェクト用クラス
 */
public class RMIExampleObject implements RMIExample {

	@Override
	public void sayHello() throws RemoteException {
		// 接続元クライアントのホスト情報を文字列で取得する.
		String clientHost;
		try {
			clientHost = RemoteServer.getClientHost();

		} catch (ServerNotActiveException ex) {
			// リモートからでない場合は"not in remote call"となる.
			clientHost = "Unknown: " + ex.toString();
		}

		// Hello Worldを表示する.
		System.out.println("Hello World!! (" + clientHost + ")");
	}

	@Override
	public String shutdown() throws RemoteException {
		ServerMain inst = ServerMain.getInstance();
		inst.stop();

		// ※ 公開停止後、Sleepとガベージコレクトしても接続は即時には切れないようだ
		try {
			for (int idx = 0; idx < 3; idx++) {
				System.gc();
				Thread.sleep(300);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return "Stopped!";
	}
}
