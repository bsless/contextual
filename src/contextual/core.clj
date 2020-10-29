(ns contextual.core
  (:require
   [clojure.walk :as walk])
  (:import
   (java.lang StringBuilder)))

(defprotocol IContext
  (-invoke [this ctx]))

(defprotocol IStringBuild
  (-invoke-with-builder [this ctx sb]))

(definline ^:private -default-invoke-with-builder
  [this ctx ^StringBuilder sb]
  `(let [ret# (-invoke ~this ~ctx)]
    (if (nil? ret#)
      nil
      (.append ~(with-meta sb {:tag "StringBuilder"}) ret#))))

(extend-protocol IContext
  Object
  (-invoke [this ctx] this)
  nil
  (-invoke [this ctx] nil))

(extend-protocol IStringBuild
  Object
  (-invoke-with-builder [this ctx sb]
    (-default-invoke-with-builder this ctx sb))
  String
  (-invoke-with-builder [this ctx sb]
    (.append ^StringBuilder sb this))
  nil
  (-invoke-with-builder [this ctx sb]))

(defrecord If [p t e]
  IContext
  (-invoke [this ctx]
    (if (-invoke p ctx)
      (-invoke t ctx)
      (-invoke e ctx)))
  IStringBuild
  (-invoke-with-builder [this ctx sb]
    (if (-invoke p ctx)
      (-invoke-with-builder t ctx sb)
      (-invoke-with-builder e ctx sb))))

(defn ->if
  ([p t]
   (->If p t nil))
  ([p t e]
   (->If p t e)))

(defrecord Fn* [f args]
  IContext
  (-invoke [this ctx]
    (apply f (map #(-invoke % ctx) args))))

(defonce ^:private fns-builders (atom {}))
(comment @fns-builders)

(defmacro ^:private def-fns []
  (let [f 'f
        invoke '-invoke
        name "Fn"
        ctx 'ctx
        defs
        (for [n (range 23)
              :let [args (map (comp symbol #(str "a" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    body (list* f (map (fn [arg] `(~invoke ~arg ~ctx)) args))]]
          `(do
             (defrecord ~rec [~f ~@args]
               IContext
               (~invoke [~'this ~ctx]
                ~body))
             (swap! fns-builders assoc ~n ~constructor)))]
    `(do
       ~@defs)))

(def-fns)

(defn ->fn
  [f & args]
  (let [n (count args)
        c (get @fns-builders n)]
    (if c
      (apply c f args)
      (->Fn* f args))))

(comment
  (-invoke
   (->fn inc 1)
   nil))

(defrecord Path [ks]
  IContext
  (-invoke [this ctx]
    (reduce get ctx ks)))

(defonce ^:private path-builders (atom {}))

(defmacro ^:private def-paths []
  (let [invoke '-invoke
        ctx 'ctx
        name "Path"
        defs
        (for [n (range 23)
              :let [ks (map (comp symbol #(str "k" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    body `(-> ~ctx ~@(map (fn [k] `(get ~k)) ks))]]
          `(do
             (defrecord ~rec [~@ks]
              IContext
              (~invoke [~'this ~ctx]
               ~body))
            (swap! path-builders assoc ~n ~constructor)))]
    `(do
       ~@defs)))

(def-paths)

(defn ->path
  [& args]
  (let [n (count args)
        c (get @path-builders n)]
    (if c
      (apply c args)
      (->Path args))))

(defrecord Or [a b]
  IContext
  (-invoke [this ctx]
    (or
     (-invoke a ctx)
     (-invoke b ctx))))

(defn ->or
  ([])
  ([a] a)
  ([a b] (->Or a b)))

(defrecord And [a b]
  IContext
  (-invoke [this ctx]
    (and
     (-invoke a ctx)
     (-invoke b ctx))))

(defn ->and
  ([])
  ([a] a)
  ([a b] (->And a b)))

(defn- string-builder-rf
  "Reducing function to start [[transduce]] with, result is string."
  ([] (StringBuilder.))
  ([^StringBuilder ret] (.toString ret))
  ([^StringBuilder acc in]
   (.append acc in)))

(defrecord Str [args]
  IContext
  (-invoke [this ctx]
    (transduce
     (comp
      (map #(-invoke % ctx))
      (remove nil?))
     string-builder-rf
     args))
  IStringBuild
  (-invoke-with-builder [this ctx sb]
    (doseq [arg args]
      (-invoke-with-builder arg ctx sb))
    (.toString ^StringBuilder sb)))

(defn ->str!
  [args]
  (->Str args))

(defn ->join!
  [delim args]
  (->Str (interpose delim args)))

(defonce ^:private str-builders (atom {}))

(defmacro ^:private def-str []
  (let [invoke '-invoke
        ctx 'ctx
        name "Str"
        sb (with-meta 'sb {:tag "StringBuilder"})
        defs
        (for [n (range 1 23)
              :let [args (map (comp symbol #(str "a" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    parts (map (fn [a] `(let [~'a (~invoke ~a ~ctx)]
                                         (if (nil? ~'a) nil (.append ~sb ~'a)))) args)]]
          `(do
             (defrecord ~rec [~@args]
               IStringBuild
               (-invoke-with-builder [~'this ~ctx ~'sb]
                 ~@parts)
               IContext
               (~invoke [~'this ~ctx]
                (let [~sb (StringBuilder.)]
                  (-invoke-with-builder ~'this ~ctx ~sb)
                  (.toString ~sb))))
             (swap! str-builders assoc ~n ~constructor)))]
    `(do
       ~@defs)))

(def-str)

(defn ->str
  [& args]
  (let [n (count args)
        c (get @str-builders n)]
    (if c
      (apply c args)
      (->Str args))))

(comment
  (-invoke (->str 1 2 3) nil)
  )

(defn ->join
  [delim & args]
  (let [args (interpose delim args)]
    (apply ->str args)))

(defprotocol IEnv
  (-lookup [this k] [this k nf])
  (-with [this k v] [this k v kvs]))

(declare ->Env)

(extend-protocol IEnv
  nil
  (-lookup [this k] nil)
  (-with
    ([this k v]
     (->Env {k v} this))
    ([this k v kvs]
     (->Env (into {k v} (partition-all 2) kvs) this))))

(defrecord Env [curr prev]
  IEnv
  (-lookup [this k]
    (when curr
      (if-let [f (find curr k)]
        (val f)
        (-lookup prev k))))
  (-lookup [this k nf]
    (if curr
      (if-let [f (find curr k)]
        (val f)
        (-lookup prev k))
      nf))
  (-with [this k v]
    (->Env {k v} this))
  (-with [this k v kvs]
    (->Env (into {k v} (partition-all 2) kvs) this)))

(defn with
  ([e k v]
   (-with e k v))
  ([e k v & kvs]
   (-with e k v kvs)))

(defn lookup
  ([e k]
   (-lookup e k))
  ([e k nf]
   (-lookup e k nf)))

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

(defn with-env
  [ctx env]
  (with-meta ctx {:env env}))

(defn bindings->ssa
  [bindings]
  (let [bs (partition 2 bindings)]
    (loop [bs bs
           seen {}
           ssa []]
      (if (seq bs)
        (let [[[b e] & bs] bs
              sym (gensym (str b "__"))
              e (walk/postwalk (fn [e] (if (symbol? e) (get seen e e) e)) e)
              seen (assoc seen b sym)
              ssa (conj ssa sym e)]
          (recur bs seen ssa))
        ssa))))

(comment
  (bindings->ssa '[a 1
                   a (+ a 1)
                   b 2
                   c (+ a b)]))

#_
(defrecord Let [bindings expr]
  (-invoke [this ctx]
    (let [e ()])))

(defn assemble
  ([expr]
   (assemble expr {}))
  ([expr lookup]
   (walk/postwalk
    (fn [expr]
      (cond
        (seq? expr)
        (let [[f & args] expr]
          (case f
            if (apply ->if args)
            or (apply ->or args)
            and (apply ->and args)
            str (apply ->str args)
            join (apply ->join args)
            path (apply ->path args)
            (apply ->fn (deref (resolve f)) args)))
        (symbol? expr) (get lookup expr expr)
        :else expr))
    expr)))

(comment

  (def ctx {:x {:y false}
            :a {:b 3}
            :y {:z "foo"}
            :u {:w "bar"}})

  (def c
    (assemble
     '(if (path :x :y)
        (+ (path :a :b) 2)
        (str (path :y :z) "blah" (path :u :w)))))

  (-invoke c ctx)

  (def --p (->path :u :w))
  (def lookup {'--p --p})

  (def c (assemble
          '(if (path :x :y)
             (+ (path :a :b) 2)
             (str (path :y :z) "blah" --p))
          lookup))

  (-invoke c ctx)

  )
