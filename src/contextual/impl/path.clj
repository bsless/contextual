(ns contextual.impl.path
  (:require
   [contextual.impl.protocols :as p]))

(defrecord Path [ks]
  p/IContext
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
               p/IContext
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

