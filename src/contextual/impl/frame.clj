(ns contextual.impl.frame
  (:require
   [contextual.impl.protocols :as p]))

(defn -empty
  []
  {})

(def not-found ::not-found)

(definline not-found?
  [v]
  `(identical? ~not-found ~v))

(deftype PersistentFrame [m]
  p/IEnv
  (-lookup [this k]
    (let [f (find m k)]
      (if (nil? f)
        not-found
        (val f))))
  (-with [this k v]
    (PersistentFrame. (assoc m k v))))

(defprotocol IFrameFactory
  (-create [this] [this k v] [this k v kvs]))

(deftype PersistentFrameFactory []
  IFrameFactory
  (-create [this] {})
  (-create [this k v] {k v})
  (-create [this k v kvs] (into {k v} (partition-all 2) kvs))
  )

(def persistent-frame-factory (PersistentFrameFactory.))
