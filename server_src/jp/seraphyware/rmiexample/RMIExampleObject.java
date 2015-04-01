package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.time.LocalDateTime;

/**
 * 公開オブジェクト用クラス
 */
public class RMIExampleObject implements RMIServer {

	@Override
	public void sayHello(Message message) throws RemoteException {
		// 接続元クライアントのホスト情報を文字列で取得する.
		String clientHost;
		try {
			clientHost = RemoteServer.getClientHost();

		} catch (ServerNotActiveException ex) {
			// リモートからでない場合は"not in remote call"となる.
			clientHost = "Unknown: " + ex.toString();
		}

		// Hello Worldを表示する.
		System.out.println("Hello World!! (" + clientHost + ") " + message);
	}

	@Override
	public void doCallback(String msg, RMIExampleCallback callback) throws RemoteException {
		Message message = new Message();
		message.setTime(LocalDateTime.now());
		message.setMessage(msg + "★ FROM SERVER!");
		callback.callback(message);
	}

	@Override
	public void send(String name, RMIInputStream ris) throws RemoteException {
		if (name.contains("\\") || name.contains("/") || name.contains("..")) {
			throw new IllegalArgumentException("不正な名前です: " + name);
		}
		new Thread(() -> {
			System.out.println("name=" + name);
			try (InputStream is = new RMIInputStreamClientImpl(ris)) {
				int ch;
				while ((ch = is.read()) > 0) {
					System.out.println(ch);
					Thread.sleep(1000);
				}
				is.close();

			} catch (IOException | InterruptedException ex) {
				ex.printStackTrace();
			}
		}).start();
	}

	@Override
	public void recv(String name, RMIOutputStream ros) throws RemoteException {
		if (name.contains("\\") || name.contains("/") || name.contains("..")) {
			throw new IllegalArgumentException("不正な名前です: " + name);
		}
		try (OutputStream os = new RMIOutputStreamClientImpl(ros);
			PrintWriter pw = new PrintWriter(os);) {
			pw.println("データ送信テスト！");

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public String shutdown() throws RemoteException {
		ServerMain inst = ServerMain.getInstance();
		inst.stop();

		// ※ 公開停止後、Sleepとガベージコレクトしても接続は即時には切れないようだ
		// ※ コールバック用に受け取ったリモートオブジェクトも解放させる.
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
