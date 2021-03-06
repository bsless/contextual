(ns contextual.impl.compile-test
  (:require
   [contextual.impl.compile :as sut]
   [contextual.impl.let :as l]
   [contextual.impl.box :as b :refer [->box]]
   [contextual.impl.path :refer [->path]]
   [contextual.impl.invoke :refer [->fn]]
   [contextual.impl.protocols :as p]
   [clojure.test :as t]
   [contextual.impl.collections :as c]))

(defonce lookup (into {} (map (fn [[k v]] [k (deref v)])) (ns-publics 'clojure.core)))

(t/deftest flatten-strings
  (t/testing ""
    (t/is
     (=
      '(let [x (str 1 2)]
         (let [y 3]
           (str x y 4 5)))
      (sut/flatten-strings
       '(let [x (str 1 (str 2))]
          (let [y 3]
            (str x y (str 4 5)))))))
    (t/is
     (=
      '(let [x (str 1 2)]
         (let [y 3]
           (str x y 4 5 "wvz")))
      (sut/flatten-strings
       '(let [x (str 1 (str 2))]
          (let [y 3]
            (str x y (str 4 5) "w" (str "v" "z")))))))))

(t/deftest lookup-table
  (t/testing "Lookup table resolves to bound symbol"
    (t/is (= (c/->vector [1]) (sut/-compile '[a] {'a 1}))))
  (t/testing "Unresolved symbol is expanded to run-time resolve"
    (t/is (= (c/->vector [(l/->lookup 'a)]) (sut/-compile '[a])))))

(t/deftest invoke
  (t/testing "Function invoke"
    (t/is
     (= 3 (p/-invoke (sut/-compile '(+ 1 2) lookup) {}))))
  (t/testing "Nested function invoke"
    (t/is
     (= 6 (p/-invoke (sut/-compile '(+ 1 (+ 2 3)) lookup) {}))))
  (t/testing "Lookup function resolution"
    (t/is
     (= 6 (p/-invoke (sut/-compile '(f 1 (f 2 3)) {'f +}) {})))))

(t/deftest control
  (t/testing "simple if"
    (t/is
     (= 1 (p/-invoke
           (sut/-compile
            '(if true 1 0))
           {}))))
  (t/testing "expression predicate"
    (t/is
     (= 0 (p/-invoke
           (sut/-compile
            '(if (= 1 0) 1 0) lookup)
           {}))))
  (t/testing "expression branch"
    (t/is
     (false? (p/-invoke
           (sut/-compile
            '(if (= 1 0) 1 (= 0 1)) lookup)
           {}))))
  (t/testing "Only one branch is evaluated"
    (let [a (atom 0)
          f #(swap! a inc)]
      (t/is
       (= 1 (p/-invoke
                (sut/-compile
                 '(if (= 1 0) (f) (f))
                 (merge lookup {'f f}))
                {})))
      (t/is
       (= 1 @a))))
  (t/testing "cond"
    (t/testing "constant tests"
      (t/is
       (= 2
          (p/-invoke
           (sut/-compile
            '(cond
               false 1
               true 2))
           {}))))
    (t/testing "expression tests"
      (t/is
       (= 2
          (p/-invoke
           (sut/-compile
            '(cond
               (= 0 1) 1
               (= 1 1) 2
               true 3)
            {'= =})
           {}))))
    (t/testing "Expression reference"
      (t/is
       (= 2
          (p/-invoke
           (sut/-compile
            '(cond
               (= (path :x) 0) 1
               (= (path :x) 1) 2
               true 3)
            {'= =})
           {:x 1})))))
  (t/testing "condp"
    (t/testing "Expression reference"
      (t/is
       (= 2
          (p/-invoke
           (sut/-compile
            '(condp = (path :x)
               0 1
               1 2
               true 3)
            {'= =})
           {:x 1}))))
    (t/testing "Default case"
      (t/is
       (= 3
          (p/-invoke
           (sut/-compile
            '(condp = 2
               0 1
               1 2
               3)
            {'= =})
           {}))))))

(t/deftest path
  (t/testing ""
    (t/is (= (->path :a) (sut/-compile '(path :a))))
    (t/is (= (->path :a :b) (sut/-compile '(path :a :b))))
    (t/is (= (->fn + (->box 1) (->path :a :b)) (sut/-compile '(+ 1 (path :a :b)) lookup)))))

(t/deftest string)

(t/deftest -let

  (t/testing "Simple let binding"
    (t/is
     (= 1
        (p/-invoke
         (sut/-compile
          '(let [x 1]
             x))
         {}))))

  (t/testing "Multiple bindings and an expression"
    (t/is
     (= 3
        (p/-invoke
         (sut/-compile
          '(let [x 1
                 y 2]
             (+ x y)) lookup)
         {}))))

  (t/testing "Expression in binding"
    (t/is
     (= 3
        (p/-invoke
         (sut/-compile
          '(let [x (path :x)
                 y 2]
             (+ x y)) lookup)
         {:x 1}))))

  (t/testing "Reference expression in bindings"
    (t/is
     (= 3
        (p/-invoke
         (sut/-compile
          '(let [x (path :x)
                 y (inc x)]
             (+ x y)) lookup)
         {:x 1}))))

  (t/testing "Nested let"
    (t/is
     (= 3
        (p/-invoke
         (sut/-compile
          '(let [x (path :x)]
             (let [y (inc x)]
               (+ x y))) lookup)
         {:x 1}))))

  (t/testing "Binding shadowing in same binding"
    (t/is
     (= 7
        (p/-invoke
         (sut/-compile
          '(let [x (path :x)
                 x 3
                 y (inc x)]
             (+ x y)) lookup)
         {:x 1}))))

  (t/testing "Binding shadowing in nested let"
    (t/is
     (= 7
        (p/-invoke
         (sut/-compile
          '(let [x (path :x)]
             (let [x 3
                   y (inc x)]
               (+ x y))) lookup)
         {:x 1})))))
