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

(defmacro ^:private def-str []
  (let [ctx 'ctx
        name "Str"
        sb (with-meta 'sb {:tag "StringBuilder"})
        defs
        (for [n (range 1 21)
              :let [args (map (comp symbol #(str "a" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    parts (map (fn [a] `(p/-invoke-with-builder ~a ~ctx ~sb)) args)]]
          {:rec
           `(defrecord ~rec [~@args]
              p/IStringBuild
              (-invoke-with-builder [~'this ~ctx ~'sb]
                ~@parts)
              p/IContext
              (-invoke [~'this ~ctx]
                (let [~sb (StringBuilder.)]
                  (p/-invoke-with-builder ~'this ~ctx ~sb)
                  (.toString ~sb))))
           :call (if (= n 20)
                   (let [args (butlast args)
                         argv (into [] (concat args ['& 'args]))]
                     `([~@argv] (~constructor ~@args (apply ~'->str ~'args))))
                   `([~@args] (~constructor ~@args)))})]
    `(do
       ~@(map :rec defs)
       (defn ~'->str ~@(map :call defs)))))

(def-str)

(comment
  (p/-invoke (->str 1 2 3) nil)
  (p/-invoke (apply ->str (range 19)) nil)
  (p/-invoke (apply ->str (range 20)) nil)
  (p/-invoke (apply ->str (range 21)) nil)
  (p/-invoke (apply ->str (range 80)) nil))

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

(defn unnest-str1*
  [expr]
  (assert (strexpr? expr) "must only be called on str expression.")
  (apply
   list
   (transduce
    (comp
     (mapcat
      (fn [expr]
        (if (and (seq? expr) (strexpr? expr))
          (rest expr)
          [expr])))
     compress-string-xf)
    conj
    expr)))

(deftype Sbf [f]
  p/IStringBuild
  (-invoke-with-builder [this ctx sb]
    (f sb)))

(defn ->sbf [f] (Sbf. f))

(defn trim-sb
  [^StringBuilder sb]
  (.setLength sb (unchecked-dec-int (.length sb))))

(def trim (->sbf trim-sb))
