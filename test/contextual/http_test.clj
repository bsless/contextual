(ns contextual.http-test
  (:require
   [contextual.core :refer [invoke]]
   [contextual.http :as sut]
   [clojure.test :as t]
   [clojure.string :as str]))

(t/deftest request

  (t/testing "URL compiler"
    (t/testing "Simple"
      (t/is
       (= {:url "https://foo.bar.com"
           :method "GET"}
          (sut/request '{:url "https://foo.bar.com"}
                       {}))))

    (t/testing "Expression"
      (t/is
       (= '{:url (str "https://" (path :foo) ".bar.com")
            :method "GET"}
          (sut/request '{:url (str "https://" (path :foo) ".bar.com")}
                       {}))))

    (t/testing "Vector"
      (t/is
       (= '{:url (str "https://hello." (path :foo) ".bar.com")
            :method "GET"}
          (sut/request '{:url ["https://hello" (path :foo) "bar.com"]}
                       {})))))

  (t/testing "Path compiler"
    (t/testing "Simple"
      (t/is
       (= '{:url (str "https://foo.bar.com" "/" "fizz/buzz")
            :method "GET"}
          (sut/request '{:url "https://foo.bar.com"
                         :path "fizz/buzz"}
                       {}))))

    (t/testing "Expression"
      (t/is
       (= '{:url (str (str "https://" (path :foo) ".bar.com") "/" (url-encode (path :bar)))
            :method "GET"}
          (sut/request '{:url (str "https://" (path :foo) ".bar.com")
                         :path [(path :bar)]}
                       {}))))

    (t/testing "Vector"
      (t/is
       (= '{:url (str (str "https://hello." (path :foo) ".bar.com") "/" (str "fizz/" (url-encode (path :buzz))))
            :method "GET"}
          (sut/request '{:url ["https://hello" (path :foo) "bar.com"]
                         :path ["fizz" (path :buzz)]}
                       {})))))


  (t/testing "Query params"
    (t/testing "As map"
      (t/is
       (= '{:url (str "https://" (path :foo) ".bar.com")
            :method "GET"
            :query-params {:a 1}}
          (sut/request '{:url (str "https://" (path :foo) ".bar.com")
                         :query-params {:a 1}}
                       {}))))
    (t/testing "Serialized"
      (t/is
       (= '{:url (str (str "https://" (path :foo) ".bar.com") "?" (str "a=1&" __qs-trim))
            :method "GET"}
          (sut/request '{:url (str "https://" (path :foo) ".bar.com")
                         :query-params {:a 1}}
                       {:serialize-query-params true})))))

  (t/testing "Body"

    (t/testing "As map"
      (t/is
       (= '{:url "https://bar.com"
            :method "GET"
            :body {:a 1}}
          (sut/request '{:url "https://bar.com"
                         :body {:a 1}}
                       {}))))

    (t/testing "Serialized"
      (t/is
       (= '{:url "https://bar.com"
            :body (body-serializer {:a 1})
            :method "GET"}
          (sut/request '{:url "https://bar.com"
                         :body {:a 1}}
                       {:serialize-body true})))))

  (t/testing "Form"
    (t/testing "As map"
      (t/is
       (= '{:url "https://bar.com"
            :method "GET"
            :form {:a 1}}
          (sut/request '{:url "https://bar.com"
                         :form {:a 1}}
                       {}))))

    (t/testing "Serialized"
      (t/is
       (= '{:url "https://bar.com"
            :form (form-serializer {:a 1})
            :method "GET"}
          (sut/request '{:url "https://bar.com"
                         :form {:a 1}}
                       {:serialize-form true}))))))

(t/deftest compile-request

  (t/testing "URL compiler"
    (t/testing "Simple"
      (t/is
       (= {:url "https://foo.bar.com"
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url "https://foo.bar.com"})
           {}))))

    (t/testing "Expression"
      (t/is
       (= {:url (str "https://foo.bar.com")
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url (str "https://" (path :foo) ".bar.com")}
            {})
           {:foo "foo"}))))

    (t/testing "Vector"
      (t/is
       (= {:url (str "https://hello.foo.bar.com")
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url ["https://hello" (path :foo) "bar.com"]}
            {})
           {:foo "foo"})))))

  (t/testing "Multi Path"
    (t/testing "first option taken"
      (t/is
       (= {:url "https://hello.fizz.bar.com"
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url ["https://hello" (multi-path [:foo :bar] [:fizz :buzz]) "bar.com"]}
            {})
           {:foo {:bar "fizz"}
            :fizz {:buzz "else"}}))))
    (t/testing "second option taken"
      (t/is
       (= {:url "https://hello.else.bar.com"
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url ["https://hello" (multi-path [:foo :bar] [:fizz :buzz]) "bar.com"]}
            {})
           {:fizz {:buzz "else"}})))))

  (t/testing "Predicative Multi Path"
    (t/testing "first option taken"
      (t/is
       (= {:url "https://hello.fizz.bar.com"
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url ["https://hello" (pred-multi-path not-blank [:foo :bar] [:fizz :buzz]) "bar.com"]}
            {'not-blank (complement str/blank?)})
           {:foo {:bar "fizz"}
            :fizz {:buzz "else"}}))))
    (t/testing "second option taken"
      (t/is
       (= {:url "https://hello.else.bar.com"
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url ["https://hello" (pred-multi-path not-blank [:foo :bar] [:fizz :buzz]) "bar.com"]}
            {'not-blank (complement str/blank?)})
           {:foo {:bar ""}
            :fizz {:buzz "else"}})))))

  (t/testing "Path compiler"
    (t/testing "Simple"
      (t/is
       (= {:url "https://foo.bar.com/fizz/buzz"
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url "https://foo.bar.com"
              :path "fizz/buzz"}
            {})
           {}))))

    (t/testing "Expression"
      (t/is
       (= {:url (str "https://fizz.bar.com/buzz")
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url (str "https://" (path :foo) ".bar.com")
              :path [(path :bar)]}
            {})
           {:foo "fizz"
            :bar "buzz"}))))

    (t/testing "Vector"
      (t/is
       (= {:url "https://hello.fizz.bar.com/fizz/quux"
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url ["https://hello" (path :foo :bar) "bar.com"]
              :path ["fizz" (path :buzz)]}
            {})
           {:foo {:bar "fizz"}
            :buzz "quux"})))))

  (t/testing "Query params"
    (t/testing "As map")
    (t/is
     (= {:url "https://foo.bar.com"
         :method "GET"
         :query-params {:a 1}}
        (invoke
         (sut/compile-request
          '{:url "https://foo.bar.com"
            :query-params {:a 1}}
          {})
         {})))

    (t/testing "Serialized"
      (t/is
       (= {:url "https://foo.bar.com?a=1"
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url "https://foo.bar.com"
              :query-params {:a 1}}
            {}
            {}
            {:serialize-query-params true})
           {})))))

  (t/testing "Body"

    (t/testing "As map"
      (t/is
       (= {:url "https://bar.com"
           :method "GET"
           :body {:a 1}}
          (invoke
           (sut/compile-request
            '{:url "https://bar.com"
              :body {:a 1}})
           {}))))

    (t/testing "Serialized"
      (t/is
       (= {:url "https://bar.com"
           :body "{:a 1}"
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url "https://bar.com"
              :body {:a 1}}
            {}
            {}
            {:serialize-body true
             :body-serializer pr-str})
           {}))))

    (t/testing "Serialized"
      (t/is
       (= {:url "https://bar.com"
           :headers {:foo "{:a 1}"}
           :body "{:a 1}"
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url "https://bar.com"
              :headers {:foo body}
              :body {:a 1}}
            {}
            {}
            {:serialize-body true
             :body-serializer pr-str})
           {})))))

  (t/testing "Form"

    (t/testing "As map"
      (t/is
       (= {:url "https://bar.com"
           :method "GET"
           :form {:a 1}}
          (invoke
           (sut/compile-request
            '{:url "https://bar.com"
              :form {:a 1}})
           {}))))

    (t/testing "Serialized"
      (t/is
       (= {:url "https://bar.com"
           :form "{:a 1}"
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url "https://bar.com"
              :form {:a 1}}
            {}
            {}
            {:serialize-form true
             :form-serializer pr-str})
           {}))))))

(t/deftest optional-attribute-15
  (let [template '{:url "http://www.url.com",
                   :body {:ip ^:optional ip}}
        lookup {'ip (contextual.core/path :ip)}
        compiled (sut/compile-request template lookup)]
    (t/testing "Optional value exists"
      (t/is (= {:url "http://www.url.com"
                :body {:ip "1.2.3.4"}
                :method "GET"}
               (invoke compiled {:ip "1.2.3.4"}))))
    (t/testing "Optional value is missing"
      (t/is (= {:url "http://www.url.com"
                :body {}
                :method "GET"}
               (invoke compiled {}))))))

(t/deftest query-string-serialization-17
  (let [template '{:url "http://www.query-params-issue.com" :query-params {:ip ip}}
        lookup {'ip (contextual.core/path :ip)}
        compiled (sut/compile-request template lookup {} {:serialize-query-params true})]
    (t/is
     (=
      {:method "GET", :url "http://www.query-params-issue.com?ip=127.0.0.1"}
      (contextual.core/invoke compiled {:ip "127.0.0.1"})))))

(t/deftest encoding

  (t/testing "Path"
    (t/is
     (= {:url (str "https://bar.com/fi%20zz/hello%20world")
         :method "GET"}
        (invoke
         (sut/compile-request
          '{:url (str "https://bar.com")
            :path [(path :foo) (path :bar)]}
          {})
         {:foo "fi zz"
          :bar "hello world"}))))

  (t/testing "Query params"
    (t/testing "Serialized"
      (t/is
       (= {:url "https://foo.bar.com?a=1&b%20B=B%20b&c=x%20X&d%20D=y%20Y&e%20E=z"
           :method "GET"}
          (invoke
           (sut/compile-request
            '{:url "https://foo.bar.com"
              :query-params {:a 1
                             "b B" "B b"
                             :c (path :x)
                             (path :d) (path :y)
                             (path :e) "z"
                             }}
            {}
            {}
            {:serialize-query-params true})
           {:x "x X"
            :d "d D"
            :y "y Y"
            :e "e E"}))))))
