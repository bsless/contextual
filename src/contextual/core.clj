(ns contextual.core
  (:require
   [clojure.walk :as walk])
  (:import
   (java.util Map HashMap)
   (java.lang StringBuilder)))

(set! *warn-on-reflection* true)

(comment
  (use 'clojure.tools.trace)
  (trace-ns contextual.core))

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

(defrecord MapWrapper [m]
  IContext
  (-invoke [this ctx]
    (persistent!
     (reduce-kv
      (fn [m k v]
        (assoc! m (-invoke k ctx) (-invoke v ctx)))
      (transient {})
      m))))

(defn ->map [m] (->MapWrapper m))

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

(def sentinel (Object.))
(defn sentinel? [x] (identical? sentinel x))
(def ^:dynamic *env-type* :persistent)

(defprotocol IEnv
  (-lookup [this k] [this k nf])
  (-with [this k v] [this k v kvs]))

(defrecord LocalPersistentEnv [m]
  IEnv
  (-lookup [this k]
    (if-let [f (find m k)]
      (val f)
      sentinel))
  (-lookup [this k nf]
    (if-let [f (find m k)]
      (val f)
      nf))
  (-with [this k v]
    (assoc this k v))
  (-with [this k v kvs]
    (into (assoc this k v) (partition-all 2) kvs)))

(defrecord LocalMutableEnv [^Map m]
  IEnv
  (-lookup [this k]
    (.getOrDefault m k sentinel))
  (-lookup [this k nf]
    (.getOrDefault m k nf))
  (-with [this k v]
    (.put m k v)
    this)
  (-with [this k v kvs]
    (.put this k v)
    (doseq [[k v] (partition 2 kvs)]
      (.put m k v))
    this))

(defn ->local-persistent-env
  [m]
  (->LocalPersistentEnv m))

(defn ->local-env
  [m]
  (->LocalMutableEnv m))

(defn new-local-persistent-env
  ([]
   (->local-persistent-env {}))
  ([k v]
   (->local-persistent-env {k v}))
  ([k v kvs]
   (->local-persistent-env (into {k v} (partition-all 2) kvs))))

(defn new-local-env
  ([]
   (->local-env (HashMap.)))
  ([k v]
   (->local-env (doto ^Map (HashMap.) (.put k v))))
  ([k v kvs]
   (let [^Map m (doto ^Map (HashMap.) (.put k v))]
     (doseq [[k v] (partition 2 kvs)]
       (.put m k v))
     (->local-env m))))

(declare ->Env)

(extend-protocol IEnv
  nil
  (-lookup [this k] nil)
  (-with
    ([this k v]
     (->Env (new-local-env k v) this))
    ([this k v kvs]
     (->Env (new-local-env k v kvs) this))))

(defn -env-lookup
  [curr prev k]
  (let [f (-lookup curr k sentinel)]
    (if (sentinel? f)
      (-lookup prev k)
      f)))

(defrecord Env [curr prev]
  IEnv
  (-lookup [this k]
    (if curr
      (-env-lookup curr prev k)
      (throw (new RuntimeException (str "Unable to resolve symbol: " k " in this context")))))
  (-lookup [this k nf]
    (if curr
      (-env-lookup curr prev k)
      nf))
  (-with [this k v]
    (->Env (new-local-env k v) this))
  (-with [this k v kvs]
    (->Env (new-local-env k v kvs) this)))

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

(defn env?
  [e]
  (instance? Env e))

(defn with-env
  [ctx e]
  (let [prev (getenv ctx)
        e (cond (map? e) (env e prev)
                (env? e) e)]
    (with-meta ctx {:env e})))

(defn- symbol-lookup
  [lookup sym]
  (if (symbol? sym) (get lookup sym sym) sym))

(defn binding-symbol
  [s]
  (with-meta s {:binding true}))

(defn binding-symbol?
  [s]
  (boolean (:binding (meta s))))

(defn bindings->ssa
  [bindings]
  (let [bs (partition 2 bindings)]
    (loop [bs bs
           seen {}
           ssa []
           trace []]
      (if (seq bs)
        (let [[[b e] & bs] bs
              sym (gensym (str b "__"))
              e (walk/postwalk (partial symbol-lookup seen) e)
              seen (assoc seen b sym)
              ssa (conj ssa (binding-symbol sym) e)
              trace (conj trace seen)]
          (recur bs seen ssa trace))
        {:bindings ssa :seen seen :trace trace}))))

(comment
  (bindings->ssa '[a 1
                   a (+ a 1)
                   b 2
                   c (+ a b)]))

(defn let->ssa
  [[_let bs & body]]
  (let [{:keys [bindings seen]} (bindings->ssa bs)
        body (walk/postwalk (partial symbol-lookup seen) body)]
    (concat (list 'let bindings) body)))

(comment
  (let->ssa
   '(let [a 1
          a (+ a 1)
          b 2
          c (+ a b)]
      (println a b)
      (* c b))))

(defn ssa-bindings
  [expr]
  (walk/postwalk
   (fn [expr]
     (cond
       (seq? expr)
       (let [[f] expr]
         (case f
           let (let->ssa expr)
           expr))
       :else expr))
   expr))

(defrecord Lookup [sym]
  IContext
  (-invoke [this ctx]
    (let [e (getenv ctx)]
      (lookup e sym))))

(defn ->lookup
  [sym]
  (->Lookup sym))

(comment
  (->lookup 's))

(defrecord Bindings [bindings]
  IContext
  (-invoke [this ctx]
    (reduce
     (fn [e [s expr]]
       (let [v (-invoke expr (with-env ctx e))]
         (assoc e s v)))
     {}
     bindings)))

(defonce ^:private binding-builders (atom {}))

(defmacro ^:private def-bindings []
  (let [invoke '-invoke
        name "Bindings"
        ctx 'ctx
        env 'e
        defs
        (for [n (range 1 13)
              :let [syms (map (comp symbol #(str "sym" %)) (range n))
                    exprs (map (comp symbol #(str "expr" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    bindings (reduce
                              (fn [bs [s e]]
                                (conj bs env `(-with ~env ~s (~invoke ~e (with-env ~ctx ~env)))))
                              `[~env (new-local-env)]
                              (map vector syms exprs))
                    body `(let [~@bindings] ~env)]]
          `(do
             (defrecord ~rec [~@(interleave syms exprs)]
               IContext
               (~invoke [~'this ~ctx]
                ~body))
             (swap! binding-builders assoc ~n ~constructor)))]
    `(do
       ~@defs)))

(def-bindings)

(defn ->bindings
  [args]
  (let [n (/ (count args) 2)
        c (get @binding-builders n)]
    (if c
      (apply c args)
      (->Bindings (into [] (partition-all 2) args)))))

(defrecord Let [bindings expr]
  IContext
  (-invoke [this ctx]
    (let [e (-invoke bindings ctx)]
      (-invoke expr (with-env ctx e)))))

(defn ->let
  [bindings expr]
  (->Let (->bindings bindings) expr))

(def symbols-registry
  {'if ->if
   'or ->or
   'and ->and
   'str ->str
   'join ->join
   'path ->path
   'let ->let})

(defn maybe-resolve
  [s]
  (when-let [v (resolve s)]
    (deref v)))

(defn expand-symbol
  [lookup s]
  (or
   (and (symbols-registry s) s)
   (maybe-resolve s)
   (and (binding-symbol? s) s)
   (get lookup s (->lookup s))))

(defn assemble
  ([expr]
   (assemble expr {}))
  ([expr lookup]
   (walk/postwalk
    (fn [expr]
      (cond
        (seq? expr)
        (let [[f & args] expr]
          (if-let [f (symbols-registry f)]
            (apply f args)
            (apply ->fn f args)))
        (symbol? expr) (expand-symbol lookup expr)
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

(defn -compile
  ([expr]
   (-compile expr {}))
  ([expr lookup]
   (->
    expr
    ssa-bindings
    (assemble lookup))))

(comment
  (def c
    (-compile
     '(if (path :x :y)
        (let [x (path :a :b)]
          (+ x 2))
        (str (path :y :z) "blah" (path :u :w)))))

  (def ctx {:x {:y true}
            :a {:b 3}
            :y {:z "foo"}
            :u {:w "bar"}})

  (-invoke c ctx)
  )
