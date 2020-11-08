(ns contextual.impl.compile
  (:require
   [clojure.walk :as walk]
   [contextual.impl.control :as control :refer [->if ->or ->and]]
   [contextual.impl.path :as path :refer [->path]]
   [contextual.impl.let :as l :refer [->let]]
   [contextual.impl.string :as s :refer [->str ->join]]
   [contextual.impl.invoke :as i]
   [contextual.impl.protocols :as p]))

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
  [registry lookup s]
  (or
   (and (registry s) s)
   (maybe-resolve s)
   (and (l/binding-symbol? s) s)
   (get lookup s (l/->lookup s))))

(defn assemble
  ([expr]
   (assemble expr {}))
  ([expr lookup]
   (assemble expr lookup symbols-registry))
  ([expr lookup registry]
   (let [registry (merge symbols-registry registry)]
     (walk/postwalk
      (fn [expr]
        (cond
          (seq? expr)
          (let [[f & args] expr]
            (if-let [f (registry f)]
              (apply f args)
              (apply i/->fn f args)))
          (symbol? expr) (expand-symbol registry lookup expr)
          :else expr))
      expr))))

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
