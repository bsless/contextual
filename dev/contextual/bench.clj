(ns contextual.bench
  (:require
   [contextual.impl.compile :as c]
   [contextual.impl.protocols :as p]
   [sci.core :as sci]
   [criterium.core :as cc]
   [sci.impl.analyzer :as ana]
   [sci.impl.evaluator :as eval]
   [clj-async-profiler.core :as prof]))

(def simple-expr '(+ 1 2))
(def simple-let '(let [x 1 y 2] (+ x y)))
(def nested-let '(let [x 1] (let [y 2] (+ x y))))
(def string-1 '(str 1))
(def string-3 '(str 1 2 3))
(def string-10 '(str 1 2 3 4 5 6 7 8 9 10))
(def nested-str3 '(str 1 (str 2 (str 3))))

(def lookup {'+ +})

(def scitx (sci/init {}))

(defn report-mean
  [result]
  (let [{:keys [mean]} result
        [mean] mean
        [factor unit] (cc/scale-time mean)]
    [(* mean factor) unit]))

(defmacro bench
  [& expr]
  `(let [res# (cc/quick-benchmark (do ~@expr) nil)]
     (report-mean res#)))

(defn bench-analysis [expr]
  (println "ANALYSIS:" expr)
  (let [[mean unit] (bench (ana/analyze scitx expr))]
    (println "SCI:" mean unit))
  (let [[mean unit] (bench (c/-compile expr lookup))]
    (println "CONTEXTUAL:" mean unit)))

(defn bench-eval [expr]
  (println "EVAL:" expr)
  (let [ana (ana/analyze scitx expr)
        [mean unit] (bench (eval/eval scitx {} ana))]
    (println "SCI:" mean unit))
  (let [c (c/-compile expr lookup)
        [mean unit] (bench (p/-invoke c {}))]
    (println "CONTEXTUAL:" mean unit)))

(comment
  (bench-analysis simple-expr)
  (bench-eval simple-expr)
  (doseq [expr [simple-expr simple-let nested-let string-1 string-3 string-10 nested-str3]]
    (bench-analysis expr))
  (doseq [expr [simple-expr simple-let nested-let string-1 string-3 string-10 nested-str3]]
    (bench-eval expr)))
