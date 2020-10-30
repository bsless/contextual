(ns contextual.http
  (:require
   [contextual.core :as c])
  (:import
   [java.net URLEncoder]))

(defn ^String url-encode
  [s]
  (URLEncoder/encode (str s) "utf8"))

(def ^:const path-sep \/)

(defrecord QueryParams [m]
  c/IContext
  (-invoke [this ctx]
    (persistent!
     (reduce-kv
      (fn [m k v]
        (let [k (c/-invoke k ctx)]
          (if (nil? k)
            m
            (let [v (c/-invoke v ctx)]
              (assoc! m k v)))))
      (transient {})
      m))))


(defn query-string
  [m]
  (apply
   c/->str
   (into
    []
    (map (fn [[k v]]
           (c/->if
            k
            (c/->str
             (c/->if
              (c/->fn keyword? k)
              (c/->fn name k)
              k) \= v \&))))
    m)))

(comment
  (def qs (query-string
           {:a 1
            nil 3
            :b 2
            :c (c/->path :c)}))
  (time
   (dotimes [_ 1e6]
     (c/-invoke qs {:c 4}))))

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
  (cond
    (and (scalar? k) (scalar? v)) (str (emit-scalar k) "=" (emit-scalar v) "&")
    (scalar? k) (list 'str (emit-scalar k) \= v \&)
    :else
    `(if ~k
       (~'let [~'k ~k]
        (~'str (if (keyword? ~'k)
                 (name ~'k)
                 ~'k)
         \=
         ~v
         \&)))))

(defn qs->ir
  [m]
  (let [parts (->>
               m
               (map (fn [[k v]] (emit-kv-pair k v)))
               (partition-by string?)
               (mapcat
                (fn [xs]
                  (if (string? (first xs))
                    [(apply str xs)]
                    xs))))]
    `(~'str ~@parts)))

(comment
  (def ir (qs->ir
           {:a 1
            nil 3
            :b 2
            :d nil
            :c (c/->path :c)}))
  (def c (c/-compile ir))

  (c/-invoke c {:c 4})
  )

#_
(defrecord Request
    [url path query-params body form method]
  c/IContext
  (-invoke [this ctx]
    (let [url (c/-invoke url ctx)
          path (c/-invoke path ctx)
          query-params (c/-invoke query-params ctx)
          body (c/-invoke body ctx)
          form (c/-invoke form ctx)
          method (c/invoke method ctx)])
    (cond->
        {:url url}
      )))

(comment
  '(->request
    {:url (->url (->str ,,,))
     }))
