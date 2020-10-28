(ns contextual.core
  (:import
   (java.lang StringBuilder)))

(defprotocol IContext
  (-invoke [this ctx]))

(defprotocol IStringBuild
  (-invoke-with-builder [this ctx sb]))

(extend-protocol IContext
  Object
  (-invoke [this ctx] this)
  nil
  (-invoke [this ctx] nil))

(extend-protocol IStringBuild
  Object
  (-invoke-with-builder [this ctx sb] (.append ^StringBuilder sb (.toString this)))
  String
  (-invoke-with-builder [this ctx sb] (.append ^StringBuilder sb this))
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
  [f & args]
  (let [n (count args)
        c (get @path-builders n)]
    (if c
      (apply c f args)
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
                                         (if (nil? ~'a) nil (.append ~sb ~'a)))) args)
                    body `(let [~sb (StringBuilder.)]
                            ~@parts
                            (.toString ~sb))]]
          `(do
             (defrecord ~rec [~@args]
               IContext
               (~invoke [~'this ~ctx]
                ~body))
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
