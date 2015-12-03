package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * 公開オブジェクト用クラス
 */
public class RMIExampleObject implements RMIServer {

	private final Logger logger = Logger.getLogger(getClass().getName());

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
		showMessage("Hello World!! (" + clientHost + ") " + message);
	}

	@Override
	public void doCallback(String msg, RMIExampleCallback callback) throws RemoteException {
		for (int idx = 0; idx < 3; idx++) {
			try {
				Thread.sleep(300);

			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}

			Message message = new Message();
			message.setTime(LocalDateTime.now());
			message.setMessage(msg + "★ FROM SERVER!");
			callback.callback(message);
		}
	}

	@Override
	public void send(String name, RMIInputStream ris) throws RemoteException {
		logger.info("send: " + Thread.currentThread());
		if (name.contains("\\") || name.contains("/") || name.contains("..")) {
			throw new IllegalArgumentException("不正な名前です: " + name);
		}
		new Thread(() -> {
			showMessage("name=" + name);
			try (InputStream is = new RMIInputStreamClientImpl(ris)) {
				int ch;
				while ((ch = is.read()) > 0) {
					showMessage("" + (char) ch);
					Thread.sleep(500);
				}

			} catch (IOException | InterruptedException ex) {
				showError(ex);
			}
		}).start();
	}

	@Override
	public void recv(String name, RMIOutputStream ros) throws RemoteException {
		logger.info("recv: " + Thread.currentThread());
		if (name.contains("\\") || name.contains("/") || name.contains("..")) {
			throw new IllegalArgumentException("不正な名前です: " + name);
		}
		try (OutputStream os = new RMIOutputStreamClientImpl(ros);
			PrintWriter pw = new PrintWriter(os);) {
			for (int idx = 0; idx < 10; idx++) {
				String msg = "データ送信テスト！ " + idx;
				showMessage(msg);
				ros.write((msg +  "\r\n").getBytes());
				Thread.sleep(500);
			}

		} catch (IOException | InterruptedException ex) {
			showError(ex);
		}
	}

	@Override
	public String shutdown() throws RemoteException {
		logger.info("shutdown");
		return "Stopped!";
	}

	protected void showError(Throwable ex) {
		ex.printStackTrace();
	}

	protected void showMessage(String message) {
		System.out.println(message);
	}
}
