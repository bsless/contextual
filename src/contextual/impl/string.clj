(ns contextual.impl.string
  (:require
   [contextual.impl.protocols :as p]))

(set! *warn-on-reflection* true)

(defrecord Str [args]
  p/IContext
  (-invoke [this ctx]
    (let [^StringBuilder sb (StringBuilder.)]
      (p/-invoke-with-builder this ctx sb)
      (.toString sb)))
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
                    parts (map (fn [a] `(p/-invoke-with-builder ~a ~ctx ~sb)) args)]]
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

(def compress-string-xf
  (comp
   (remove nil?)
   (partition-by (some-fn string? char?))
   (mapcat
    (fn [xs]
      (if ((some-fn string? char?) (first xs))
        [(apply str xs)]
        xs)))))

(comment
  (transduce compress-string-xf conj [] '[a b "c" d "e" "f" g])
  (transduce compress-string-xf conj [] '["0" a b "c" d "e" "f" g])
  (transduce compress-string-xf conj [] '["0" a b "c" d "e" \= "f" g])
  (transduce compress-string-xf conj [] '["0" a b "c" (path :x y) "e" \= "f" g]))

(defn strexpr?
  [expr]
  (= (first expr) 'str))

(defn unnest-str1
  [expr]
  (assert (= 'str (first expr)) "must only be called on str expression.")
  (mapcat
   (fn [expr]
     (if (and (seq? expr) (strexpr? expr))
       (rest expr)
       [expr]))
   expr))

(defn unnest-str
  [expr]
  (let [expr' (unnest-str1 expr)]
    (if (= expr expr')
      expr
      (recur expr'))))

