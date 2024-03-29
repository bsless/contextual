(ns contextual.http
  (:refer-clojure :exclude [compile])
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.path :refer [->path]]
   [contextual.impl.string :refer [compress-string-xf strexpr?]]
   [contextual.impl.compile :refer [-compile symbols-registry]]
   [contextual.impl.collections :refer [->map ->maybe-map]]
   [contextual.impl.http :refer [->kv ->okv ->query-params]]
   [contextual.walk :as w])
  (:import
   [java.net URLEncoder]))

(defn ^String url-encode
  [s]
  (.replace (URLEncoder/encode (str s) "utf8") "+" "%20"))

(comment
  (url-encode (url-encode "foo bar")))

(def ^:const path-sep "/")
(def ^:const url-sep ".")

(def scalar?
  (some-fn
   string?
   keyword?
   number?
   nil?
   boolean?
   ))

(defn- emit-scalar
  ([k]
   (emit-scalar k false))
  ([k encode?]
   (cond->
       (cond
         (or (keyword? k) (symbol? k)) (name k)
         :else k)
     encode? url-encode)))

(defn optional? [v] (boolean (:optional (w/maybe-meta v))))

(defn- emit-kv-pair
  ([k v]
   (emit-kv-pair k v false))
  ([k v encode?]
   (when k
     (cond

       (and (scalar? k) (scalar? v))
       (str (emit-scalar k encode?) "=" (emit-scalar v encode?) "&")

       (scalar? v) ;; then k is definitely not a scalar
       (let [v (emit-scalar v encode?)
             k (if encode? `(~'url-encode ~k) k)]
         `(~'kv ~k ~v))

       (optional? v)
       (let [k (if (scalar? k) (emit-scalar k encode?) (if encode? `(~'url-encode ~k) k))]
         `(~'okv ~k ~v)) ;; optional KV container

       :else
       (if (scalar? k)
         (list 'str (emit-scalar k encode?) "=" (if encode? (list 'url-encode v) v) "&")
         (if encode?
           `(~'kv (~'url-encode ~k) (~'url-encode ~v))
           `(~'kv ~k ~v)))))))

(defn qs->ir
  ([m]
   (qs->ir m false))
  ([m encode?]
   (let [parts (into
                []
                (comp
                 (map (fn [[k v]] (emit-kv-pair k v encode?)))
                 compress-string-xf)
                m)]
     `(~'str ~@(conj parts '__qs-trim)))))

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
  (def c (-compile ir {'__qs-trim contextual.impl.string/trim} {'kv ->kv}))

  (p/-invoke c {:c 4
                :e "e"})
  )

(defn path->ir
  ([path]
   (path->ir path path-sep))
  ([path sep]
   (path->ir path sep false))
  ([path sep encode?]
   (cond
     (string? path) path
     (strexpr? path) path
     :else (let [path (transduce (comp (if encode?
                                      (map (fn [part]
                                             (if (scalar? part)
                                               (emit-scalar part true)
                                               `(~'url-encode ~part))))
                                      identity)
                                    (interpose sep)
                                    compress-string-xf) conj [] path)]
             (if (= 1 (count path)) (first path)
                 (list* 'str path))))))

(comment
  (path->ir "a/b")
  (path->ir ["a" "b"])
  (path->ir ["a" '(aha! 2) "b"]))

(def http-symbols-registry
  {'kv ->kv
   'okv ->okv
   '-map ->map
   '->hashmap ->maybe-map
   'query-params ->query-params})

(defn request
  ([{:keys [url path query-params body form method headers]
     :or {method "GET"}}
    {:keys [serialize-query-params
            serialize-body
            serialize-form]}]
   (let [url (cond->
                 (path->ir url url-sep)
               path (as-> $ `(~'str ~$ "/" ~(path->ir path path-sep true)))
               serialize-query-params (as-> $ `(~'str ~$ "?" ~(qs->ir query-params serialize-query-params))))
         body (when body (if serialize-body (list 'body-serializer body) body))
         form (when form (if serialize-form (list 'form-serializer form) form))]
     (cond->
         {:method method
          :url url}
       headers (assoc :headers headers)
       (and query-params (not serialize-query-params)) (assoc :query-params query-params)
       body (assoc :body body)
       form (assoc :form form)))))

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

(defn- -compile-request
  [{:keys [body headers] :as request} lookup registry]
  (if (and body headers (some #(= 'body %) (tree-seq coll? identity headers)))
    (let [request (assoc request :body 'body)
          lookup (assoc lookup 'body (->path ::body))
          body (-compile body lookup registry)
          request (-compile request lookup registry)]
      (reify p/IContext
        (-invoke [this ctx]
          (let [body' (p/-invoke body ctx)
                ctx (assoc ctx ::body body')]
            (p/-invoke request ctx)))))
    (-compile request lookup registry)))

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
   (when serialize-body
     (assert (fn? body-serializer)
             "Must provide body-serializer fn when serialize-body is true."))
   (when serialize-form
     (assert (fn? form-serializer)
             "Must provide form-serializer fn when serialize-body is true."))
   (let [lookup (merge
                 (assoc lookup '__qs-trim contextual.impl.string/trim)
                 (when serialize-body {'body-serializer body-serializer})
                 (when serialize-form {'form-serializer form-serializer}))]
     (-compile-request
      (request req opts)
      (assoc lookup 'url-encode #'url-encode)
      (merge symbols-registry http-symbols-registry registry)))))
