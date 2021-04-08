(ns contextual.impl.invoke
  (:require
   [contextual.impl.protocols :as p]))

(defrecord Fn* [f args]
  p/IContext
  (-invoke [this ctx]
    (apply f (map #(p/-invoke % ctx) args))))

(defmacro ^:private def-fns []
  (let [f 'f
        invoke '-invoke
        name "Fn"
        ctx 'ctx
        defs
        (for [n (range 20)
              :let [args (map (comp symbol #(str "a" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    body (list* f (map (fn [arg] `(p/-invoke ~arg ~ctx)) args))]]
          {:rec
           `(defrecord ~rec [~f ~@args]
              p/IContext
              (~invoke [~'this ~ctx]
               ~body))
           :call
           (if (= n 20)
             `([~f ~@args] (->Fn* ~f [~@args]))
             `([~f ~@args] (~constructor ~f ~@args)))})]
    `(do
       ~@(map :rec defs)
       (defn ~'->fn ~@(map :call defs)))))

(def-fns)

(comment
  (p/-invoke
   (->fn inc 1)
   nil))
