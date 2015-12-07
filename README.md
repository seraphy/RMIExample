# RMIExample
RMIサーバとクライアントの実験 (Java8u60)

## ビルド方法
antにより、RMIExampleServer.jarとRMIExampleClient.jarが生成される。

双方とも実行可能jarである。


## RMIExampleServer
RMIExampleServerはJavaFXアプリである。
画面上で任意のレジストリポートとエクスポートポートを指定してサーバを開始できる。



## RMIExampleClient
RMIExampleClientはJavaFX8によるGUIアプリである。
接続先のホスト名とボートを指定してRMIを接続する。

クライアント・サーバ間で受け渡すシリアライズ型のデータの例の他、
コールバックオブジェクトをエクスポートするメソッド例もある。

