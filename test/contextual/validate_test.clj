(ns contextual.validate-test
  (:require
   [contextual.validate :as sut]
   [contextual.core]
   [clojure.test :as t]))

(def core-lookup (contextual.core/namespaces->lookup '[clojure.core]))

(t/deftest validate
  (t/testing "Empty expression"
    (t/is (= {} (sut/validate-expression 1 {} {}))))
  (t/testing "Unrecognized symbol"
    (t/is (= {:unresolvable-symbols '#{x}}
             (sut/validate-expression 'x {} {}))))
  (t/testing "Registered macro"
    (t/is (= {}
             (sut/validate-expression '(if true 1 2) {} {}))))
  (t/testing "Nesting"
    (t/is (= {}
             (sut/validate-expression '(if true (if false 2 3) 2) {} {}))))
  (t/testing "Unrecognized symbol at call position"
    (t/is (= '{:unresolvable-symbols #{+},
               :bad-function-calls [{:name +, :cause "Does not exist!"}]}
             (sut/validate-expression '(+ 1 2) {} {}))))
  (t/testing "Lookup success"
    (t/is (= '{}
             (sut/validate-expression '(+ 1 2) core-lookup {}))))
  (t/testing "Wrong arity"
    (t/is (= '{:bad-function-calls
               [{:name assoc, :cause "Wrong number of arguments: 2"}]}
             (sut/validate-expression '(assoc {} 1) core-lookup {}))))
  (t/testing "Not callable"
    (t/is (= '{:bad-function-calls [{:name 1, :cause "1 is not a function"}]}
             (sut/validate-expression '(1 + 2) core-lookup {}))))
  (t/testing "Let scoping"
    (sut/validate-expression '(let [x 1
                                    y (+ x 3)
                                    z (* x y)]
                                (let [v (vector x y z)
                                      w (let [n 1] (nth v 1))]
                                  [v w])) core-lookup {})))
