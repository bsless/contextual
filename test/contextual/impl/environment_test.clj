(ns contextual.impl.environment-test
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.environment :as sut]
   [clojure.test :as t]))

(t/deftest environment
  (t/testing "Empty environment"
    (t/testing "Lookup always fails"
      (t/is (thrown? RuntimeException (sut/lookup nil :b))))
    (t/testing "With new bindings lookup succeeds for existing keys and fails for non existing keys"
      (let [e (sut/with nil :a 1)]
        (t/is (= 1 (sut/lookup e :a)))
        (t/is (thrown? RuntimeException (sut/lookup e :b))))))
  (t/testing "Chaining"
    (t/testing "Environments chain correctly"
      (let [e (-> nil (sut/with :a 1) (sut/with :b 2) (sut/with :c 3))]
        (t/is (= 1 (sut/lookup e :a)))
        (t/is (= 2 (sut/lookup e :b)))
        (t/is (= 3 (sut/lookup e :c)))
        (t/is (thrown? RuntimeException (sut/lookup e :d)))))
    (t/testing "Chaining shadows values"
      (let [e (-> nil (sut/with :a 1) (sut/with :a 2))]
        (t/is (= 2 (sut/lookup e :a)))))))
