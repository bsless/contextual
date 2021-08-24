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

(t/deftest multi-path
  (let [p (sut/->multi-path [:a] [:b] [:c :d])]
    (t/is (= 1 (p/-invoke p {:a 1 :b 2 :c {:d 3}})))
    (t/is (= 2 (p/-invoke p {:b 2 :c {:d 3}})))
    (t/is (= 3 (p/-invoke p {:c {:d 3}})))))

(t/deftest predicative-multi-path
  (let [p (sut/->pred-multi-path even? [:a] [:b] [:c :d])]
    (t/is (= 10 (p/-invoke p {:a 10 :b 2 :c {:d 3}})))
    (t/is (= 2 (p/-invoke p {:a 1 :b 2 :c {:d 3}})))))
