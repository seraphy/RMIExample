# RMIExample
RMIサーバとクライアントの実験例 (Java8u40)

## ビルド方法
antにより、RMIExampleServer.jarとRMIExampleClient.jarが生成される。

双方とも実行可能jarである。


## RMIExampleServer
RMIExampleServerはコンソールアプリであり、第一引数にポートを指定できる。
起動するとローカルホスト上の指定したポートでRMIの待ち受けを行う。

接続されたクライアント側よりShutdownが呼び出されることで終了する。


## RMIExampleClient
RMIExampleClientはJavaFX8によるGUIアプリである。
接続先のホスト名とボートを指定してRMIを接続する。

クライアント・サーバ間で受け渡すシリアライズ型のデータの例の他、
コールバックオブジェクトをエクスポートするメソッド例もある。

