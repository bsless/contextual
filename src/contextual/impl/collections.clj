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

(defrecord OptionalMapWrapper [base m]
  p/IContext
  (-invoke [this ctx]
    (persistent!
     (reduce-kv
      (fn [m k v]
        (if-some [v (p/-invoke v ctx)]
          (assoc! m (p/-invoke k ctx) v)
          m))
      (transient (p/-invoke base ctx))
      m))))

(defonce ^:private opt-map-wrapper-builders (atom {}))

(defmacro ^:private def-opt-map-wrappers []
  (let [invoke '-invoke
        ctx 'ctx
        name "OptionalMapWrapper"
        base 'base
        defs
        (for [n (range 1 12)
              :let [ks (map (comp symbol #(str "k" %)) (range n))
                    vs (map (comp symbol #(str "v" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    m (zipmap ks vs)
                    step (fn [m k v] (conj m base `(if-some [v# (p/-invoke ~v ~ctx)]
                                                   (assoc ~base (p/-invoke ~k ~ctx) v#)
                                                   ~base)))
                    steps (reduce-kv step [base `(p/-invoke ~base ~ctx)] m)
                    body `(let ~steps ~base)]]
          `(do
             (defrecord ~rec [~base ~@(interleave ks vs)]
               p/IContext
               (~invoke [~'this ~ctx]
                ~body))
             (swap! opt-map-wrapper-builders assoc ~n ~constructor)))]
    `(do
       ~@defs)))

(def-opt-map-wrappers)

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

(defn ->optional-map
  [m]
  (let [optional (filter (comp :optional meta val) m)
        mandatory (remove (comp :optional meta val) m)
        base (->map (into {} mandatory))
        c (get @opt-map-wrapper-builders (count optional))]
    (if c
      (apply c base (mapcat identity optional))
      (->OptionalMapWrapper base (into {} optional)))))

(defn ->maybe-map
  [m]
  (if (some (comp :optional meta val) m)
    (->optional-map m)
    (->map m)))

(comment
  (->map {:a 1 :b 2})
  (->map {:a 1 :b 'x})
  (->maybe-map {:a 1 :b (with-meta 'x {:optional true})})
  (->maybe-map '{:a ^:optional (path :x :y)})
  )

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
