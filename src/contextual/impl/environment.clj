(ns contextual.impl.environment
  (:require
   [contextual.impl.protocols :as p]))

(declare ->Env)

(defn- die
  [k]
  (throw (new RuntimeException (str "Unable to resolve symbol: " k " in this context"))))

(defn -env
  ([k v]
   {k v})
  ([k v kvs]
   (into {k v} (partition-all 2) kvs)))

(extend-protocol p/IEnv
  nil
  (-lookup [this k] (die k))
  (-with
    ([this k v]
     (->Env (-env k v) this))
    ([this k v kvs]
     (->Env (-env k v kvs) this))))

(defrecord Env [curr prev]
  p/IEnv
  (-lookup [this k]
    (if curr
      (if-let [f (find curr k)]
        (val f)
        (p/-lookup prev k))
      (die k)))
  (-lookup [this k nf]
    (if curr
      (if-let [f (find curr k)]
        (val f)
        (p/-lookup prev k))
      nf))
  (-with [this k v]
    (->Env (-env k v) this))
  (-with [this k v kvs]
    (->Env (-env k v kvs) this)))

(defn with
  ([e k v]
   (p/-with e k v))
  ([e k v & kvs]
   (p/-with e k v kvs)))

(defn lookup
  ([e k]
   (p/-lookup e k))
  ([e k nf]
   (p/-lookup e k nf)))

(defn env
  ([curr]
   (->Env curr nil))
  ([curr prev]
   (->Env curr prev)))

(comment
  (lookup
   (with nil 1 2 3 4 5 6)
   1)
  (lookup
   (->
    nil
    (with 1 2)
    (with 3 4))
   3)
  )

(defn getenv
  [ctx]
  (:env (meta ctx)))

(defn env?
  [e]
  (instance? Env e))

(defn with-env
  [ctx e]
  (let [prev (getenv ctx)
        e (cond (map? e) (env e prev)
                (env? e) e)]
    (with-meta ctx {:env e})))
