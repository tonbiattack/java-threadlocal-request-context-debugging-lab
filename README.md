# Java ThreadLocal request-context debugging lab

単一ワーカースレッドを再利用する処理で、前のリクエストが設定した `ThreadLocal` の actor を匿名リクエストが引き継ぐ不具合を再現します。

## 前提

- Java 21 以上
- 外部ライブラリは不要

## 実行

```bash
./run-tests.sh
```

バグ状態では、`alice` の認証済みリクエストの後に同じワーカースレッドで処理した匿名リクエストが `actor=alice` と表示されます。修正後は `actor=anonymous` になります。
