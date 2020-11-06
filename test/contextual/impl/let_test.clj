(ns contextual.impl.let-test
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.path :refer [->path]]
   [contextual.impl.invoke :refer [->fn]]
   [contextual.impl.environment :as e]
   [contextual.impl.let :as sut]
   [clojure.test :as t]))

(t/deftest lookup
  (let [s1 (sut/->lookup 's1)
        s2 (sut/->lookup 's2)
        ctx (e/with-env {} {'s1 1})]
    (t/testing "Lookup in environment returns bound value"
      (t/is (= 1 (p/-invoke s1 ctx))))
    (t/testing "Lookup of missing values throws RuntimeException"
      (t/is (thrown? RuntimeException (p/-invoke s2 ctx))))))

(t/deftest bindings
  (t/testing "Loop bindings"
    (t/is (= '{a 1 b 2} (p/-invoke (sut/->Bindings '[[a 1] [b 2]]) {})))
    (t/is (= '{a 1 b 2 c 3}
             (p/-invoke
              (sut/->Bindings
               [['a (->path :x)]
                ['b (->path :y)]
                ['c (->fn +
                          (sut/->lookup 'a)
                          (sut/->lookup 'b))]])
              {:x 1 :y 2}))))
  (t/testing "Unrolled bindings"
    (t/is (= '{a 1 b 2 c 3}
             (p/-invoke
              (sut/->Bindings3
               'a (->path :x)
               'b (->path :y)
               'c (->fn +
                        (sut/->lookup 'a)
                        (sut/->lookup 'b)))
              {:x 1 :y 2})))))

(t/deftest let-bindings
  (t/testing "Simple bindings"
    (t/is
     (=
      3
      (p/-invoke (sut/->let
                  '[a 1 b 2]
                  (->fn +
                        (sut/->lookup 'a)
                        (sut/->lookup 'b))) {}))))
  (t/testing "Expression bindings"
    (t/is
     (=
      3
      (p/-invoke (sut/->let
                  ['a (->path :x)
                   'b (->path :y)]
                  (->fn +
                        (sut/->lookup 'a)
                        (sut/->lookup 'b)))
                 {:x 1 :y 2}))))
  (t/testing "Nested let"
    (t/is
     (=
      3
      (p/-invoke
       (sut/->let
        ['a (->path :x)]
        (sut/->let
         ['b (->path :y)]
         (->fn +
               (sut/->lookup 'a)
               (sut/->lookup 'b))))
       {:x 1 :y 2}))))
  (t/testing "Shadowing"
    (t/is
     (=
      12
      (p/-invoke
       (sut/->let
        ['a (->path :x)]
        (sut/->let
         ['a 10
          'b (->path :y)]
         (->fn +
               (sut/->lookup 'a)
               (sut/->lookup 'b))))
       {:x 1 :y 2})))))
