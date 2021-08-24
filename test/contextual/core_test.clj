(ns contextual.core-test
  (:require
   [contextual.core :as sut]
   [clojure.test :as t]))


(t/deftest path
  (let [c (sut/compile '(path :a :b))]
    (t/is (= 1 (sut/invoke c {:a {:b 1}})))))

(t/deftest multi-path
  (let [c (sut/compile '(multi-path [:a] [:b] [:c :d]))]
    (t/is (= 1 (sut/invoke c {:a 1 :b 2 :c {:d 3}})))
    (t/is (= 2 (sut/invoke c {:b 2 :c {:d 3}})))
    (t/is (= 3 (sut/invoke c {:c {:d 3}})))))
