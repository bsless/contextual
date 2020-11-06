(ns contextual.impl.path-test
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.path :as sut]
   [clojure.test :as t]))

(t/deftest path
  (t/testing "Rolled path"
    (let [p (sut/->Path [:a :b])]
      (t/is (= 1 (p/-invoke p {:a {:b 1}})))))
  (t/testing "Unrolled Path"
    (let [p (sut/->Path1 :a)]
      (t/is (= 1 (p/-invoke p {:a 1})))))
  (t/testing "Unrolled Path"
    (let [p (sut/->Path2 :a :b)]
      (t/is (= 1 (p/-invoke p {:a {:b 1}})))))
  (t/testing "Path dispatch"
    (let [p (sut/->path :a :b)]
      (t/is (= 1 (p/-invoke p {:a {:b 1}}))))))
