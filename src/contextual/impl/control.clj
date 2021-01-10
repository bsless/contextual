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


(defonce ^:private or-builders (atom {}))

(defmacro ^:private def-ors []
  (let [invoke '-invoke
        ctx 'ctx
        name "Or"
        defs
        (for [n (range 23)
              :let [args (map (comp symbol #(str "k" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    ors (map (fn [arg] `(p/-invoke ~arg ~ctx)) args)
                    body `(or ~@ors)]]
          `(do
             (defrecord ~rec [~@args]
               p/IContext
               (~invoke [~'this ~ctx]
                ~body))
             (swap! or-builders assoc ~n ~constructor)))]
    `(do
       ~@defs)))

(def-ors)

(defn ->or
  [& args]
  (let [n (count args)
        c (get @or-builders n)]
    (if c
      (apply c args)
      (throw "Too many arguments for or"))))

(defonce ^:private and-builders (atom {}))

(defmacro ^:private def-ands []
  (let [invoke '-invoke
        ctx 'ctx
        name "And"
        defs
        (for [n (range 23)
              :let [args (map (comp symbol #(str "k" %)) (range n))
                    rec (symbol (str name n))
                    constructand (symbol (str "->" rec))
                    ands (map (fn [arg] `(p/-invoke ~arg ~ctx)) args)
                    body `(and ~@ands)]]
          `(do
             (defrecord ~rec [~@args]
               p/IContext
               (~invoke [~'this ~ctx]
                ~body))
             (swap! and-builders assoc ~n ~constructand)))]
    `(do
       ~@defs)))

(def-ands)

(defn ->and
  [& args]
  (let [n (count args)
        c (get @and-builders n)]
    (if c
      (apply c args)
      (throw "Too many arguments for and"))))
