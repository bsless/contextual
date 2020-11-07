(ns contextual.impl.accumulator-test
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.path :refer [->path]]
   [contextual.impl.accumulator :as sut]
   [clojure.test :as t]))

(t/deftest string
  (t/testing "->str builds a string of its arguments"
    (t/is (= "123" (p/-invoke (sut/->str 1 2 3) {}))))
  (t/testing "nil is discarded"
    (t/is (= "13" (p/-invoke (sut/->str 1 nil 3) {}))))
  (t/testing "Nested strings are concatenated"
    (t/is (= "123" (p/-invoke (sut/->str 1 (sut/->str 2 3)) {}))))
  (t/testing "Complex expressions"
    (t/is (= "ab3" (p/-invoke (sut/->str (->path :x) (sut/->str (->path :y) 3)) {:x "a" :y "b"}))))
  )
