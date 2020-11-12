(ns contextual.http-test
  (:require
   [contextual.core :refer [invoke]]
   [contextual.http :as sut]
   [clojure.test :as t]))

(t/deftest request

  (t/testing "URL compiler"
    (t/testing "Simple"
      (t/is
       (= '(-map {:url "https://foo.bar.com"
                  :method "GET"})
          (sut/request '{:url "https://foo.bar.com"}
                       {}))))

    (t/testing "Expression"
      (t/is
       (= '(-map {:url (str "https://" (path :foo) ".bar.com")
                  :method "GET"})
          (sut/request '{:url (str "https://" (path :foo) ".bar.com")}
                       {}))))

    (t/testing "Vector"
      (t/is
       (= '(-map {:url (str "https://hello." (path :foo) ".bar.com")
                  :method "GET"})
          (sut/request '{:url ["https://hello" (path :foo) "bar.com"]}
                       {})))))

  (t/testing "Path compiler"
    (t/testing "Simple"
      (t/is
       (= '(-map {:url (str "https://foo.bar.com" "/" "fizz/buzz")
                  :method "GET"})
          (sut/request '{:url "https://foo.bar.com"
                         :path "fizz/buzz"}
                       {}))))

    (t/testing "Expression"
      (t/is
       (= '(-map {:url (str (str "https://" (path :foo) ".bar.com") "/" (path :bar))
                  :method "GET"})
          (sut/request '{:url (str "https://" (path :foo) ".bar.com")
                         :path [(path :bar)]}
                       {}))))

    (t/testing "Vector"
      (t/is
       (= '(-map {:url (str (str "https://hello." (path :foo) ".bar.com") "/" (str "fizz/" (path :buzz)))
                  :method "GET"})
          (sut/request '{:url ["https://hello" (path :foo) "bar.com"]
                         :path ["fizz" (path :buzz)]}
                       {})))))


  (t/testing "Query params"
    (t/testing "As map")
    (t/is
     (= '(-map {:url (str "https://" (path :foo) ".bar.com")
                :method "GET"
                :query-params (-map {:a 1})})
        (sut/request '{:url (str "https://" (path :foo) ".bar.com")
                       :query-params {:a 1}}
                     {})))
    (t/testing "Serialized"
      (t/is
       (= '(-map {:url (str (str "https://" (path :foo) ".bar.com") "?" (str "a=1&"))
                  :method "GET"})
          (sut/request '{:url (str "https://" (path :foo) ".bar.com")
                         :query-params {:a 1}}
                       {:serialize-query-params true})))))

  (t/testing "Body"
    (t/testing "As map")
    (t/is
     (= '(-map {:url "https://bar.com"
                :method "GET"
                :body (-map {:a 1})})
        (sut/request '{:url "https://bar.com"
                       :body {:a 1}}
                     {})))
    (t/testing "Serialized"
      (t/is
       (= '(-map {:url "https://bar.com"
                  :body (body-serializer (-map {:a 1}))
                  :method "GET"})
          (sut/request '{:url "https://bar.com"
                         :body {:a 1}}
                       {:serialize-body true})))))

  (t/testing "Form"
    (t/testing "As map")
    (t/is
     (= '(-map {:url "https://bar.com"
                :method "GET"
                :form (-map {:a 1})})
        (sut/request '{:url "https://bar.com"
                       :form {:a 1}}
                     {})))
    (t/testing "Serialized"
      (t/is
       (= '(-map {:url "https://bar.com"
                  :form (form-serializer (-map {:a 1}))
                  :method "GET"})
          (sut/request '{:url "https://bar.com"
                         :form {:a 1}}
                       {:serialize-form true}))))))
