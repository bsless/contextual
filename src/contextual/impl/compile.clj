(ns contextual.impl.compile
  (:require
   [contextual.walk :as walk]
   [contextual.impl.control :as control :refer [->if ->or ->and]]
   [contextual.impl.path :as path :refer [->path]]
   [contextual.impl.let :as l :refer [->let]]
   [contextual.impl.string :as s :refer [->str ->join]]
   [contextual.impl.invoke :as i]
   [contextual.impl.box :as b]
   [contextual.impl.collections :as c]
   [contextual.impl.protocols :as p]))

(def symbols-registry
  "Assembler special forms which are expanded to specially compiled classes."
  {'if ->if
   'or ->or
   'and ->and
   'str ->str
   'join ->join
   'path ->path
   'let ->let
   '->hashmap c/->map
   '->vec c/->vector})

(defn flatten-strings
  [expr]
  (walk/postwalk
   (fn [expr]
     (if (and (seq? expr) (s/strexpr? expr))
       (s/unnest-str1* expr)
       expr))
   expr))

(defn maybe-resolve
  [s]
  (when-let [v (resolve s)]
    (deref v)))

(defn expand-symbol
  [registry lookup s]
  (or
   (and (registry s) s)
   (and (l/binding-symbol? s) s)
   (get lookup s (l/->lookup s))))

(defn- assembly-fn
  [registry lookup]
  (fn [expr]
    (cond
      (seq? expr)
      (let [[f & args] expr]
        (if-let [f' (registry f)]
          (apply f' args)
          (apply i/->fn f args)))
      (symbol? expr) (expand-symbol registry lookup expr)
      (instance? clojure.lang.MapEntry expr) expr
      (map? expr) ((registry '->hashmap) expr)
      (vector? expr) ((registry '->vec) expr)
      (or
       (string? expr)
       (keyword? expr)
       (number? expr)
       (char? expr)
       (nil? expr)
       ) (b/->box expr)
      :else expr)))

(defn assemble
  "Assemble an expression `expr` with the following optional arguments:
  `lookup`: A map from symbol to value. Can contain any type of value.
  Usually constants, Path*s or functions.
  `registry`: a symbol -> special form emitter map. See
  [[symbols-registry]] as a default value."
  ([expr]
   (assemble expr {}))
  ([expr lookup]
   (assemble expr lookup symbols-registry))
  ([expr lookup registry]
   (let [registry (merge symbols-registry registry)]
     (walk/postwalk (assembly-fn registry lookup) expr))))

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

  (p/-invoke c ctx)

  (def --p (path/->path :u :w))
  (def lookup {'--p --p})

  (def c (assemble
          '(if (path :x :y)
             (+ (path :a :b) 2)
             (str (path :y :z) "blah" --p))
          lookup))

  (p/-invoke c ctx)

  )

(defn -compile
  ([expr]
   (-compile expr {}))
  ([expr lookup]
   (-compile expr lookup {}))
  ([expr lookup registry]
   (->
    expr
    l/ssa-bindings
    flatten-strings
    (assemble lookup registry))))

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

  (p/-invoke c ctx)
  )
