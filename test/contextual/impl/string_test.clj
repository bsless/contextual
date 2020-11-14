(ns contextual.impl.string-test
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.path :refer [->path]]
   [contextual.impl.string :as sut]
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


(t/deftest unnest-str1
  (t/testing "Can only be called on str"
    (t/is (thrown? java.lang.AssertionError (sut/unnest-str1 '(a b c)))))
  (t/testing "Identity"
    (t/is (= '(str b c) (sut/unnest-str1 '(str b c)))))
  (t/testing "One level unnesting"
    (t/is (= '(str a b c) (sut/unnest-str1 '(str (str a) b c))))
    (t/is (= '(str "a" b c d) (sut/unnest-str1 '(str "a" b (str c d))))))
  (t/testing "Twu level unnesting"
    (t/is (= '(str (str a) b c) (sut/unnest-str1 '(str (str (str a)) b c))))))

(t/deftest unnest-str
  (t/testing "Can only be called on str"
    (t/is (thrown? java.lang.AssertionError (sut/unnest-str '(a b c)))))
  (t/testing "Identity"
    (t/is (= '(str b c) (sut/unnest-str '(str b c)))))
  (t/testing "One level unnesting"
    (t/is (= '(str a b c) (sut/unnest-str '(str (str a) b c))))
    (t/is (= '(str "a" b c d) (sut/unnest-str '(str "a" b (str c d))))))
  (t/testing "Twu level unnesting"
    (t/is (= '(str a b c) (sut/unnest-str '(str (str (str a)) b c))))))
