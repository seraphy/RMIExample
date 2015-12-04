package jp.seraphyware.rmiexample;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * サーバオブジェクト用インターフェイス
 */
public interface MyServerObject extends Remote {

	/**
	 * Hello, Worldをサーバー上で表示する.
	 * @throws RemoteException
	 */
	void echo(Message message) throws RemoteException;

	/**
	 * アップロードする.
	 * @param name ファイル名
	 * @return アップローダ
	 * @throws RemoteException
	 * @throws IOException
	 */
	Uploader upload(String name) throws RemoteException, IOException;

	/**
	 * ダウンロードする.
	 * @param name ファイル名
	 * @return ダウンローダ
	 * @throws RemoteException
	 * @throws IOException
	 */
	Downloader download(String name) throws RemoteException, IOException;

	/**
	 * サーバを停止する
	 * @return
	 * @throws RemoteException
	 */
	String shutdown() throws RemoteException;
}
