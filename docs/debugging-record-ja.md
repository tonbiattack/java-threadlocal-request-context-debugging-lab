# デバッグ記録: ThreadLocal の actor が次のリクエストへ漏れる

## 対象の不具合

単一ワーカースレッドを再利用する処理で、認証済みリクエストが `ThreadLocal` に保存した actor を消去しないため、後続の匿名リクエストが前リクエストの actor を監査出力へ表示する。リクエスト境界ごとに actor を分離し、匿名リクエストは必ず `anonymous` と表示することが契約である。

| 観測点 | 期待値 | バグ状態の実際値 |
| --- | --- | --- |
| 境界出力 | 匿名リクエストは `actor=anonymous` | `actor=alice` |
| 最終状態 | 次タスクで actor が残らない | 同一ワーカーに `alice` が残る |
| 保持対象 | 認証済みリクエストは `actor=alice` | `actor=alice` |

## 再現条件

バグ状態のコミットは `27e3cba00eb0567f69cc16828bd2b730425e160a` です。

```bash
git checkout 27e3cba
./run-tests.sh
```

```text
Exception in thread "main" java.lang.AssertionError: 同じワーカースレッド上の匿名リクエストは前リクエストのactorを引き継がない expected=actor=anonymous actual=actor=alice
    at lab.RequestProcessorTest.assertEquals(RequestProcessorTest.java:42)
    at lab.RequestProcessorTest.anonymousRequestDoesNotInheritPreviousActorsContext(RequestProcessorTest.java:33)
    at lab.RequestProcessorTest.main(RequestProcessorTest.java:10)
```

## 調査

| 確認対象 | 観測結果 | 判断 |
| --- | --- | --- |
| 入力 | 1タスク目は `alice`、2タスク目は actor を設定しない匿名処理 | 入力差は意図的であり、2タスク目が匿名であることは明確。 |
| 境界出力 | 1タスク目は `actor=alice`、2タスク目も `actor=alice` | 認証済み処理の結果は正しく、後続処理だけが壊れる。 |
| 最終状態 | 単一スレッドの executor が同じワーカーを再利用し、2タスク目の `ThreadLocal.get()` が前回値を読んだ | ワーカースレッド単位の状態が残っている。 |
| 実装 | `processAuthenticated` は `RequestContext.setActor(actor)` の後に `clear()` を呼ばない | 直接原因を採用。 |
| 仕様 | Oracle は、スレッドプールではタスクで設定した `ThreadLocal` 値が別タスクへ漏れ得ると説明し、`remove()` は現在スレッドの値を削除すると定義する。[1] [2] | 実装の観測と仕様が一致する。 |

## 原因

`ThreadLocal` は各スレッドに独立した値を持つため、リクエスト単位の状態を自動で初期化する仕組みではない。[2] この再現では、executor が1本のワーカースレッドを再利用し、1タスク目の `setActor("alice")` が残った。匿名処理は actor を設定しないため、`get()` がその残留値を返した。

## 修正

`processAuthenticated` を `try/finally` で囲み、レスポンス文字列を作った後に必ず `RequestContext.clear()` を呼ぶようにした。修正コミットは `2def4b42ebc84d08e90bf434f6f06b30cd02f8f8` である。例外発生時も cleanup を実行できるよう、成功経路だけに `clear()` を置かなかった。

## 回帰確認

```bash
git checkout main
./run-tests.sh
```

```text
PASS: all tests
```

認証済みタスクと匿名タスクを同じ単一ワーカーで順に実行することで、スレッド再利用を決定的に再現している。認証済み出力を保持しつつ、匿名出力を `actor=anonymous` に固定した。

## 設計上の制約

このラボは同期的な1本の executor と `ThreadLocal` の cleanup に限定する。非同期チェーン、reactive context、仮想スレッド、ログフレームワークの MDC の伝播規則は扱わない。`ThreadLocal` を使う設計自体を全面否定するものではなく、タスク境界で所有権と削除責務を明示する例である。
