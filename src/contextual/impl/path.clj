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
