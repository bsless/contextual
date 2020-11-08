(ns contextual.http
  (:refer-clojure :exclude [compile])
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.compile :refer [-compile]]
   [contextual.impl.collections :refer [->map]]
   [contextual.impl.http :refer [->kv ->query-params]])
  (:import
   [java.net URLEncoder]))

(defn ^String url-encode
  [s]
  (URLEncoder/encode (str s) "utf8"))

(def ^:const path-sep "/")

(def scalar?
  (some-fn
   string?
   keyword?
   symbol?
   number?
   nil?
   boolean?
   ))

(defn- emit-scalar
  [k]
  (cond
    (or (keyword? k) (symbol? k)) (name k)
    :else k))

(defn- emit-kv-pair
  [k v]
  (when k
    (cond
      (and (scalar? k) (scalar? v)) (str (emit-scalar k) "=" (emit-scalar v) "&")
      (scalar? k) (list 'str (emit-scalar k) "=" v "&")
      (some? k) `(~'kv ~k ~v))))

(def compress-string-xf
  (comp
   (remove nil?)
   (partition-by (some-fn string? char?))
   (mapcat
    (fn [xs]
      (if ((some-fn string? char?) (first xs))
        [(apply str xs)]
        xs)))))

(comment
  (transduce compress-string-xf conj [] '[a b "c" d "e" "f" g])
  (transduce compress-string-xf conj [] '["0" a b "c" d "e" "f" g])
  (transduce compress-string-xf conj [] '["0" a b "c" d "e" \= "f" g]))

(defn qs->ir
  [m]
  (let [parts (into
               []
               (comp
                (map (fn [[k v]] (emit-kv-pair k v)))
                compress-string-xf)
               m)]
    `(~'str ~@parts)))

(comment
  (def ir (qs->ir
           {:a 1
            nil 3
            :b 2
            :d nil
            :c '(path :c)
            '(path :e) 3
            '(path :f) 2
            }))
  (def c (-compile ir {} {'kv ->kv}))

  (p/-invoke c {:c 4
                :e "e"})
  )

(def body->ir qs->ir)

(defn path->ir
  [path]
  (if (string? path)
    path
    (let [path (interpose path-sep path)
          path (transduce compress-string-xf conj [] path)]
      (if (= 1 (count path)) (first path)
          (list* 'str path)))))

(comment
  (path->ir "a/b")
  (path->ir ["a" "b"])
  (path->ir ["a" '(aha! 2) "b"]))

(def http-symbols-registry
  {'kv ->kv
   '-map ->map
   'query-params ->query-params})

(defn request
  ([{:keys [url path query-params body form method headers]
     :or {method "GET"}}
    {:keys [serialize-query-params
            serialize-body
            serialize-form]}]
   (let [url (cond->
                 url
               path (as-> $ `(~'str ~$ "/" ~(path->ir path)))
               serialize-query-params (as-> $ `(~'str ~$ "?" ~(qs->ir query-params))))]
     `(~'-map
       ~(cond->
           {:method method
            :url url}
         headers (assoc :headers (->map headers))
         (and query-params (not serialize-query-params)) (assoc :query-params `(~'query-params ~query-params))
         body (assoc :body (if serialize-body (list 'body-serializer (body->ir body)) `(~'-map ~body)))
         form (assoc :form (if serialize-form (list 'form-serializer (body->ir form)) `(~'-map ~form))))))))

(comment
  (p/-invoke
   (-compile
    (request '{:url (str "https://" (path :foo) ".bar.com")
               :method "POST"}
             {})
    {}
    {'kv ->kv
     '-map ->map})
   {:foo "foo"})


  (p/-invoke
   (-compile
    (request '{:url (str "https://" (path :foo) ".bar.com")
               :method "POST"
               :query-params {:a 1
                              nil 3
                              :b 2
                              :d nil
                              :c (path :c)
                              (path :e) 3
                              (path :f) 2
                              }}
             {})
    {}
    {'kv ->kv
     '-map ->map
     'query-params ->query-params})
   {:foo "foo"
    :c 4
    :e "e"})


  (p/-invoke
   (-compile
    (request '{:url (str "https://" (path :foo) ".bar.com")
               :method "POST"
               :query-params {:a 1
                              nil 3
                              :b 2
                              :d nil
                              :c (path :c)
                              (path :e) 3
                              (path :f) 2
                              }}
             {:serialize-query-params true})
    {}
    {'kv ->kv
     '-map ->map
     'query-params ->query-params})
   {:foo "foo"
    :c 4
    :e "e"})

  (p/-invoke
   (-compile
    (request '{:url (str "https://" (path :foo) ".bar.com")
               :path "fizz/buzz"
               :method "POST"
               :query-params {:a 1
                              nil 3
                              :b 2
                              :d nil
                              :c (path :c)
                              (path :e) 3
                              (path :f) 2
                              }}
             {:serialize-query-params true})
    {}
    {'kv ->kv
     '-map ->map
     'query-params ->query-params})
   {:foo "foo"
    :c 4
    :e "e"})

  (p/-invoke
   (-compile
    (request '{:url (str "https://" (path :foo) ".bar.com")
               :path ["fizz" "buzz"]
               :method "POST"
               :query-params {:a 1
                              nil 3
                              :b 2
                              :d nil
                              :c (path :c)
                              (path :e) 3
                              (path :f) 2
                              }}
             {:serialize-query-params true})
    {}
    {'kv ->kv
     '-map ->map
     'query-params ->query-params})
   {:foo "foo"
    :c 4
    :e "e"}))

(defn compile-request
  "Compile request template to `IContext` tree which outputs a map of
  similar form.
  Takes:
  - `request`, a map of at least :url, which can also contain:
    - method: GET, POST, etc.
    - path: a vector of segments or a single string
    - query-params: a map or sequence of pairs
    - body: a map or sequence of pairs
    - form: a map or sequence of pairs
  - `lookup`: like `contextual.core/compile`
  - `registry`: like `contextual.core/compile`
  - options:
    - serialize-body & body-serializer: when the former is true and the
      latter is provided, will serialize body to a string (after evaluation)
      with the provided serializer
    - serialize-form & form-serializer: same as with body.
    - serialize-query-params: will serialize the query params at the end of the url if true.
  "
  {:arglists
   '([{:keys [url path query-params body form method headers]}]
     [{:keys [url path query-params body form method headers]}
      lookup]
     [{:keys [url path query-params body form method headers]}
      lookup
      registry]
     [{:keys [url path query-params body form method headers]}
      lookup
      registry
      {:keys [serialize-query-params
              serialize-body
              serialize-form
              form-serializer
              body-serializer]}])}
  ([req]
   (compile-request req {}))
  ([req lookup]
   (compile-request req lookup {}))
  ([req lookup registry]
   (compile-request req lookup registry {}))
  ([req
    lookup
    registry
    {:keys [serialize-body
            serialize-form
            form-serializer
            body-serializer] :as opts}]
   (assert (and serialize-body (not (fn? body-serializer)))
           "Must provide body-serializer fn when serialize-body is true.")
   (assert (and serialize-form (not (fn? form-serializer)))
           "Must provide form-serializer fn when serialize-body is true.")
   (-compile
    (request req opts)
    lookup
    (merge http-symbols-registry registry))))
