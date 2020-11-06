(ns contextual.impl.string
  (:require
   [contextual.impl.protocols :as p]))

(defn- string-builder-rf
  "Reducing function to start [[transduce]] with, result is string."
  ([] (StringBuilder.))
  ([^StringBuilder ret] (.toString ret))
  ([^StringBuilder acc in]
   (.append acc in)))

(defrecord Str [args]
  p/IContext
  (-invoke [this ctx]
    (transduce
     (comp
      (map #(p/-invoke % ctx))
      (remove nil?))
     string-builder-rf
     args))
  p/IStringBuild
  (-invoke-with-builder [this ctx sb]
    (doseq [arg args]
      (p/-invoke-with-builder arg ctx sb))
    (.toString ^StringBuilder sb)))

(defn ->str!
  [args]
  (->Str args))

(defn ->join!
  [delim args]
  (->Str (interpose delim args)))

(defonce ^:private str-builders (atom {}))

(defmacro ^:private def-str []
  (let [ctx 'ctx
        name "Str"
        sb (with-meta 'sb {:tag "StringBuilder"})
        defs
        (for [n (range 1 23)
              :let [args (map (comp symbol #(str "a" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    parts (map (fn [a] `(let [~'a (p/-invoke ~a ~ctx)]
                                         (if (nil? ~'a) nil (.append ~sb ~'a)))) args)]]
          `(do
             (defrecord ~rec [~@args]
               p/IStringBuild
               (-invoke-with-builder [~'this ~ctx ~'sb]
                 ~@parts)
               p/IContext
               (-invoke [~'this ~ctx]
                (let [~sb (StringBuilder.)]
                  (p/-invoke-with-builder ~'this ~ctx ~sb)
                  (.toString ~sb))))
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
  (p/-invoke (->str 1 2 3) nil))

(defn ->join
  [delim & args]
  (let [args (interpose delim args)]
    (apply ->str args)))
