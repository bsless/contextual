(ns contextual.impl.control
  (:require
   [contextual.impl.protocols :as p]))

(defrecord If [p t e]
  p/IContext
  (-invoke [this ctx]
    (if (p/-invoke p ctx)
      (p/-invoke t ctx)
      (p/-invoke e ctx)))
  p/IStringBuild
  (-invoke-with-builder [this ctx sb]
    (if (p/-invoke p ctx)
      (p/-invoke-with-builder t ctx sb)
      (p/-invoke-with-builder e ctx sb))))

(defn ->if
  ([p t]
   (->If p t nil))
  ([p t e]
   (->If p t e)))

(defmacro ^:private def-ors []
  (let [invoke '-invoke
        ctx 'ctx
        name "Or"
        defs
        (for [n (range 21)
              :let [args (map (comp symbol #(str "k" %)) (range n))
                    rec (symbol (str name n))
                    ors (map (fn [arg] `(p/-invoke ~arg ~ctx)) args)
                    constructor (symbol (str "->" rec))
                    body `(or ~@ors)]]
          {:rec
           `(defrecord ~rec [~@args]
              p/IContext
              (~invoke [~'this ~ctx]
               ~body))
           :call `([~@args] (~constructor ~@args))})
        constructor `(defn ~'->or ~@(map :call defs))]
    `(do
       ~@(map :rec defs)
       ~constructor)))

(def-ors)

(defmacro ^:private def-ands []
  (let [invoke '-invoke
        ctx 'ctx
        name "And"
        defs
        (for [n (range 21)
              :let [args (map (comp symbol #(str "k" %)) (range n))
                    rec (symbol (str name n))
                    constructand (symbol (str "->" rec))
                    ands (map (fn [arg] `(p/-invoke ~arg ~ctx)) args)
                    body `(and ~@ands)]]
          {:rec
           `(defrecord ~rec [~@args]
                p/IContext
                (~invoke [~'this ~ctx]
                 ~body))
           :call `([~@args] (~constructand ~@args))})
        constructor `(defn ~'->and ~@(map :call defs))]
    `(do
       ~@(map :rec defs)
       ~constructor)))

(def-ands)

(defonce ^:private cond-builders (atom {}))

(defmacro ^:private def-conds []
  (let [invoke '-invoke
        ctx 'ctx
        name "Cond"
        defs
        (for [n (range 1 13)
              :let [ks (map (comp symbol #(str "k" %)) (range n))
                    vs (map (comp symbol #(str "v" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    args (interleave ks vs)
                    body `(cond ~@(mapv
                                   (fn [v]
                                     `(p/-invoke ~v ~ctx))
                                   args))]]
          `(do
             (defrecord ~rec [~@(interleave ks vs)]
               p/IContext
               (~invoke [~'this ~ctx]
                ~body))
             (swap! cond-builders assoc ~n ~constructor)))]
    `(do
       ~@defs)))

(def-conds)

(defn ->cond
  [& args]
  (let [n (quot (count args) 2)
        c (get @cond-builders n)]
    (if c
      (apply c args)
      (throw (new IllegalArgumentException "Too many arguments to cond")))))

(defonce ^:private condp-builders (atom {}))

(defmacro ^:private def-condps []
  (let [invoke '-invoke
        ctx 'ctx
        name "Condp"
        pred 'pred
        expr 'expr
        defs
        (for [n (range 1 21)
              :let [args (map (comp symbol #(str "x" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    body `(condp ~pred (p/-invoke ~expr ~ctx)
                            ~@(mapv
                               (fn [v]
                                 `(p/-invoke ~v ~ctx))
                               args))]]
          `(do
             (defrecord ~rec [~pred ~expr ~@args]
               p/IContext
               (~invoke [~'this ~ctx]
                ~body))
             (swap! condp-builders assoc ~n ~constructor)))]
    `(do
       ~@defs)))

(def-condps)

(defn ->condp
  [pred expr & args]
  (let [n (count args)
        c (get @condp-builders n)]
    (if c
      (apply c pred expr args)
      (throw (new IllegalArgumentException "Too many arguments to condp")))))

