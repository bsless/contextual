(ns contextual.impl.invoke-test
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.invoke :as sut]
   [clojure.test :as t]))

(t/deftest invoke
  (t/testing "Rolled fn"
    (let [p (sut/->Fn* + [1 2])]
      (t/is (= 3 (p/-invoke p {})))))
  (t/testing "Unrolled Fn"
    (let [p (sut/->Fn2 + 1 2)]
      (t/is (= 3 (p/-invoke p {})))))
  (t/testing "Fn dispatch"
    (let [p (sut/->fn + 1 2)]
      (t/is (= 3 (p/-invoke p {})))))
  (t/testing "Nested functions"
    (let [p (sut/->fn + 1 2 (sut/->fn + 1 2))]
      (t/is (= 6 (p/-invoke p {}))))))
