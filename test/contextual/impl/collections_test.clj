(ns contextual.impl.collections-test
  (:require
   [contextual.core :as c]
   [contextual.impl.collections :as sut]
   [clojure.test :as t]))

(t/deftest Map
  (t/testing "A constant map is unchanged."
    (t/is
     (=
      {:a 1}
      (c/invoke
       (c/compile
        {:a 1})
       {})))

    (t/testing "Explicit nils will be part of a map."
      (t/is
       (=
        {:a 1 :b nil}
        (c/invoke
         (c/compile
          {:a 1 :b nil})
         {})))
      (t/is
       (=
        {:a 1 :b nil}
        (c/invoke
         (c/compile
          {:a 1 :b nil}
          {}
          {'->hashmap sut/->maybe-map})
         {})))))

  (t/testing "Map containing an expression."
    (t/testing "Expression is normally evaluated."
      (t/is
       (=
        {:a 1 :b 1}
        (c/invoke
         (c/compile
          '{:a 1 :b (path :x)}
          {}
          {})
         {:x 1})))
      (t/is
       (=
        {:a 1 :b nil}
        (c/invoke
         (c/compile
          '{:a 1 :b (path :x)}
          {}
          {})
         {:x nil})))
      (t/is
       (=
        {:a 1 :b nil}
        (c/invoke
         (c/compile
          '{:a 1 :b (path :x)}
          {}
          {})
         {}))))

    (t/testing "Expression in optional map"
      (t/testing "evaluate normally when untagged."
        (t/is
         (=
          {:a 1 :b 1}
          (c/invoke
           (c/compile
            '{:a 1 :b (path :x)}
            {}
            {'->hashmap sut/->maybe-map})
           {:x 1})))
        (t/is
         (=
          {:a 1 :b nil}
          (c/invoke
           (c/compile
            '{:a 1 :b (path :x)}
            {}
            {'->hashmap sut/->maybe-map})
           {:x nil})))
        (t/is
         (=
          {:a 1 :b nil}
          (c/invoke
           (c/compile
            '{:a 1 :b (path :x)}
            {}
            {'->hashmap sut/->maybe-map})
           {}))))
      (t/testing "are nil checked when tagged optional."
        (t/is
         (=
          {:a 1 :b 1}
          (c/invoke
           (c/compile
            '{:a 1 :b ^:optional (path :x)}
            {}
            {'->hashmap sut/->maybe-map})
           {:x 1})))
        (t/is
         (=
          {:a 1}
          (c/invoke
           (c/compile
            '{:a 1 :b ^:optional (path :x)}
            {}
            {'->hashmap sut/->maybe-map})
           {:x nil})))
        (t/is
         (=
          {:a 1}
          (c/invoke
           (c/compile
            '{:a 1 :b ^:optional (path :x)}
            {}
            {'->hashmap sut/->maybe-map})
           {})))))))
