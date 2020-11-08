(ns contextual.http
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.compile :refer [-compile]]
   [contextual.impl.string :refer [->str]]
   [contextual.impl.control :refer [->if]]
   [contextual.impl.invoke :refer [->fn]]
   [contextual.impl.path :refer [->path]]
   [contextual.impl.collections :refer [->map]]
   [contextual.impl.http :refer [->kv ->query-params]])
  (:import
   [java.net URLEncoder]))

(defn ^String url-encode
  [s]
  (URLEncoder/encode (str s) "utf8"))

(def ^:const path-sep "/")

(defn query-string
  [m]
  (apply
   ->str
   (into
    []
    (map (fn [[k v]]
           (->if
            k
            (->str
             (->if
              (->fn keyword? k)
              (->fn name k)
              k) "=" v "&"))))
    m)))

(comment
  (def qs (query-string
           {:a 1
            nil 3
            :b 2
            :c (->path :c)}))
  (time
   (dotimes [_ 1e6]
     (p/-invoke qs {:c 4}))))

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

#_
(defn body->ir
  [body]
  `(~'-map ~body))
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
  ([{:keys [url path query-params body form method headers]}
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
         body (assoc :body (if serialize-body (body->ir body) `(~'-map ~body)))
         form (assoc :form (if serialize-form (body->ir form) `(~'-map ~form))))))))

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
