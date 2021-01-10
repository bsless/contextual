(ns contextual.impl.control-test
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.control :as sut]
   [clojure.test :as t]))

(t/deftest control
  (t/testing "Constant predicate"
    (t/is (= 1 (p/-invoke (sut/->if true 1 0) nil))))
  (t/testing "Expression predicate"
    (t/is (= 0 (p/-invoke (sut/->if (= 1 2) 1 0) nil))))
  (t/testing "Or"
    (t/is (= 1 (p/-invoke (sut/->or 1 2) nil)))
    (t/is (= 2 (p/-invoke (sut/->or false 2) nil))))
  (t/testing "And"
    (t/is (= 2 (p/-invoke (sut/->and 1 2) nil)))
    (t/is (= false (p/-invoke (sut/->and false 2) nil))))
  (t/testing "Complex predicate"
    (t/is (= 0 (p/-invoke
                (sut/->if
                 (sut/->and false 2)
                 1
                 0)
                nil))))
  (t/testing "Cond"
    (t/is (= 3
             (p/-invoke
              (sut/->cond
               false 1
               nil 2
               true 3
               true 4)
              {}))))
  (t/testing "Condp"
    (t/is (= 3
             (p/-invoke
              (sut/->condp = :c
               :a 1
               :b 2
               :c 3
               :d 4)
              {})))))
