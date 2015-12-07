package jp.seraphyware.rmiexample;

import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.Unreferenced;

import jp.seraphyware.rmiexample.rmi.Message;
import jp.seraphyware.rmiexample.rmi.RemoteControl;

public class RemoteControlImpl implements RemoteControl, Unreferenced {

	@Override
	public void echo(Message message) throws RemoteException {
		// 接続元クライアントのホスト情報を文字列で取得する.
		String clientHost;
		try {
			clientHost = RemoteServer.getClientHost();

		} catch (ServerNotActiveException ex) {
			// リモートからでない場合は"not in remote call"となる.
			clientHost = "Unknown: " + ex.toString();
		}

		// メッセージを表示する.
		showMessage(clientHost + "/message=" + message.toString());
	}

	@Override
	public String shutdown() throws RemoteException {
		return "do nothing";
	}

	@Override
	public void unreferenced() {
		// 参照数が0になるたびに呼び出される.
		// (クライアント側で参照を放置し分散GCでゼロになるたびに呼び出される.)
		showMessage("★★UNREFERENCED: " + this + "/isProxyClass="
				+ Proxy.isProxyClass(this.getClass()));
	}

	protected void showMessage(String msg) {
		System.out.println(msg);
	}
}
