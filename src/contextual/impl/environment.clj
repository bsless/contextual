(ns contextual.impl.environment
  (:require
   [contextual.impl.frame :as f]
   [contextual.impl.protocols :as p]))

(declare ->Env)

(defn- die
  [k]
  (throw (new RuntimeException (str "Unable to resolve symbol: " k " in this context"))))

(defn -env
  ([]
   (f/-create f/persistent-frame-factory))
  ([k v]
   (f/-create f/persistent-frame-factory k v))
  ([k v kvs]
   (f/-create f/persistent-frame-factory k v kvs)))

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
      (let [f (p/-lookup curr k)]
        (if (f/not-found? f)
          (p/-lookup prev k)
          f))
      (die k)))
  (-lookup [this k nf]
    (if curr
      (let [f (p/-lookup curr k)]
        (if (f/not-found? f)
          (p/-lookup prev k)
          f))
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
