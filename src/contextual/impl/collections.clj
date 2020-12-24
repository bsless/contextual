(ns contextual.impl.collections
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.box :as b]))

(defrecord MapWrapper [m]
  p/IContext
  (-invoke [this ctx]
    (persistent!
     (reduce-kv
      (fn [m k v]
        (assoc! m (p/-invoke k ctx) (p/-invoke v ctx)))
      (transient {})
      m))))


(defonce ^:private map-wrapper-builders (atom {}))

(defmacro ^:private def-map-wrappers []
  (let [invoke '-invoke
        ctx 'ctx
        name "MapWrapper"
        defs
        (for [n (range 1 13)
              :let [ks (map (comp symbol #(str "k" %)) (range n))
                    vs (map (comp symbol #(str "v" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    body (reduce-kv
                          (fn [m k v]
                            (assoc m `(p/-invoke ~k ~ctx) `(p/-invoke ~v ~ctx)))
                          {}
                          (zipmap ks vs))]]
          `(do
             (defrecord ~rec [~@(interleave ks vs)]
               p/IContext
               (~invoke [~'this ~ctx]
                ~body))
             (swap! map-wrapper-builders assoc ~n ~constructor)))]
    `(do
       ~@defs)))

(def-map-wrappers)

(defn ->map
  [m]
  (let [n (count m)
        args (mapcat identity m)
        c (get @map-wrapper-builders n)]
    (if (every? b/box? args)
      (b/->box (reduce-kv (fn [m k v] (assoc m (b/unbox k) (b/unbox v))) {} m))
      (if c
        (apply c args)
        (->MapWrapper m)))))

(comment
  (->map {:a 1 :b 2}))

(defrecord VectorWrapper [v]
  p/IContext
  (-invoke [this ctx]
    (into [] (map #(p/-invoke % ctx)) v))
  p/IBox
  (-boxed? [this] true)
  (-get [this] v))

(defn ->vector [v]
  (if (every? b/box? v)
    (b/->box (mapv b/unbox v))
    (->VectorWrapper v)))
