(ns contextual.impl.fn
  (:require
   [contextual.walk :refer [postwalk]]
   [contextual.impl.protocols :as p]))

(defrecord Fn* [f args]
  p/IContext
  (-invoke [this ctx]
    (apply f (map #(p/-invoke % ctx) args))))

(defrecord Fn [a body]
  p/IContext
  (-invoke [this ctx]
    (fn [a]
      (-invoke ))))

(defn mkfn
  [name args body compiler]
  (let [arg (first args)
        body (postwalk {name 'self-call} body)
        self (atom nil)
        self-call (fn [& args] (apply @self args))
        compiled (compiler body {'self-call self-call})]
    (fn [a]
      (p/-invoke compiled {arg a}))))



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
                    body (list* f (map (fn [arg] `(p/-invoke ~arg ~ctx)) args))]]
          `(do
             (defrecord ~rec [~f ~@args]
               p/IContext
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
  (p/-invoke
   (->fn inc 1)
   nil))
