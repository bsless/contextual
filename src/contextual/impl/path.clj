(ns contextual.impl.path
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.box :as b]))

(defrecord Path [ks]
  p/IContext
  (-invoke [this ctx]
    (reduce get ctx ks)))

(defmacro ^:private def-paths []
  (let [invoke '-invoke
        ctx 'ctx
        name "Path"
        defs
        (for [n (range 21)
              :let [ks (map (comp symbol #(str "k" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    body `(-> ~ctx ~@(map (fn [k] `(get ~k)) ks))]]
          {:rec
           `(defrecord ~rec [~@ks]
              p/IContext
              (~invoke [~'this ~ctx]
               ~body))
           :call `([~@ks] (~constructor ~@(map (fn [k] `(b/unbox ~k)) ks)))})]
    `(do
       ~@(map :rec defs)
       (defn ~'->path ~@(map :call defs)))))

(def-paths)


(defmacro ^:private def-multi-paths []
  (let [invoke '-invoke
        ctx 'ctx
        name "MultiPath"
        defs
        (for [n (range 1 9)
              :let [ps (map (comp symbol #(str "p" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    body `(or ~@(map (fn [p] `(p/-invoke ~p ~ctx)) ps))]]
          {:rec
           `(defrecord ~rec [~@ps]
              p/IContext
              (~invoke [~'this ~ctx]
               ~body))
           :call `([~@ps] (~constructor ~@(map (fn [k] `(apply ~'->path (b/unbox ~k))) ps)))})]
    `(do
       ~@(map :rec defs)
       (defn ~'->multi-path ~@(map :call defs)))))

(def-multi-paths)

(defmacro ^:private def-predicative-multi-paths []
  (let [invoke '-invoke
        ctx 'ctx
        name "PredicativeMultiPath"
        pred 'pred
        defs
        (for [n (range 1 9)
              :let [ps (map (comp symbol #(str "p" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    body `(or ~@(map (fn [p] `(let [~'ret (p/-invoke ~p ~ctx)]
                                               (if (~pred ~'ret)
                                                 ~'ret))) ps))]]
          {:rec
           `(defrecord ~rec [~pred ~@ps]
              p/IContext
              (~invoke [~'this ~ctx]
               ~body))
           :call `([~pred ~@ps] (~constructor ~pred ~@(map (fn [k] `(apply ~'->path (b/unbox ~k))) ps)))})]
    `(do
       ~@(map :rec defs)
       (defn ~'->pred-multi-path ~@(map :call defs)))))

(def-predicative-multi-paths)
