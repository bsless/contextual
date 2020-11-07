(ns contextual.impl.collections
  (:require
   [contextual.impl.protocols :as p]))

(defrecord MapWrapper [m]
  p/IContext
  (-invoke [this ctx]
    (persistent!
     (reduce-kv
      (fn [m k v]
        (assoc! m (p/-invoke k ctx) (p/-invoke v ctx)))
      (transient {})
      m))))

(defn ->map [m] (->MapWrapper m))

(defrecord VectorWrapper [v]
  p/IContext
  (-invoke [this ctx]
    (into [] (map p/-invoke) v)))

(defn ->vector [v] (->VectorWrapper v))
