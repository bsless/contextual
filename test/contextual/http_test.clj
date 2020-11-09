(ns contextual.http-test
  (:require
   [contextual.core :refer [invoke]]
   [contextual.http :as sut]
   [clojure.test :as t]))

(t/deftest request
  (t/testing "Simple url"
    (t/is
     (= '(-map {:url "https://foo.bar.com"
                :method "GET"})
        (sut/request '{:url "https://foo.bar.com"}
                     {}))))
  (t/testing "Expression url"
    (t/is
     (= '(-map {:url (str "https://" (path :foo) ".bar.com")
                :method "GET"})
        (sut/request '{:url (str "https://" (path :foo) ".bar.com")}
                     {}))))
  (t/testing "Query params"
    (t/is
     (= '(-map {:url (str "https://" (path :foo) ".bar.com")
                :method "GET"
                :query-params (-map {:a 1})})
        (sut/request '{:url (str "https://" (path :foo) ".bar.com")
                       :query-params {:a 1}}
                     {}))))
  (t/testing "Serialized query params"
    (t/is
     (= '(-map {:url (str (str "https://" (path :foo) ".bar.com") "?" (str "a=1&"))
                :method "GET"})
        (sut/request '{:url (str "https://" (path :foo) ".bar.com")
                       :query-params {:a 1}}
                     {:serialize-query-params true})))))
