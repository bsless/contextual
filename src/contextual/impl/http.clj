(ns contextual.impl.http
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.string :refer [->str]]
   [contextual.impl.control :refer [->if]]
   [contextual.impl.invoke :refer [->fn]]
   [contextual.impl.path :refer [->path]]
   [contextual.impl.collections :refer [->map]]
   ))


(defrecord QueryParams [m]
  p/IContext
  (-invoke [this ctx]
    (persistent!
     (reduce-kv
      (fn [m k v]
        (let [k (p/-invoke k ctx)]
          (if (nil? k)
            m
            (let [v (p/-invoke v ctx)]
              (assoc! m k v)))))
      (transient {})
      m))))

(defn ->query-params [m]
  (->QueryParams m))

(defrecord Key [k]
  p/IContext
  (-invoke [this ctx]
    (let [k (p/-invoke k ctx)]
      (if (keyword? k)
        (name k)
        k))))

(defn ->key [k] (->Key k))

(defrecord KeyValue [k v]
  p/IStringBuild
  (-invoke-with-builder [this ctx sb]
    (let [k (p/-invoke k ctx)]
      (when k
        (.append ^StringBuilder sb k)
        (.append ^StringBuilder sb "=")
        (p/-invoke-with-builder v ctx sb)
        (.append ^StringBuilder sb "&")))))

(defn ->kv [k v] (->KeyValue (->key k) v))
