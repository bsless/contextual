(ns contextual.bench
  (:require
   [jmh.core :as jmh]
   [contextual.impl.compile :as c]
   [contextual.impl.protocols :as p]
   [sci.core :as sci]
   [criterium.core :as cc]
   [clj-async-profiler.core :as prof]))


(def simple-expr '(+ 1 2))
(def simple-let '(let [x 1 y 2] (+ x y)))
(def nested-let '(let [x 1] (let [y 2] (+ x y))))
(def string-1 '(str 1))
(def string-3 '(str 1 2 3))
(def string-10 '(str 1 2 3 4 5 6 7 8 9 10))
(def nested-str3 '(str 1 (str 2 (str 3))))

(def scitx (sci/init {}))

(defn bench-full-vs
  [expr]
  (println "Benchmarking expr:" expr)
  (println "\nContextual")
  (cc/quick-bench
   (p/-invoke (c/-compile expr) {}))
  (println "\nSCI")
  (cc/quick-bench
   (sci/eval-form scitx expr))
  (println))

(defn bench-vs
  [expr]
  (println "Benchmarking expr:" expr)
  (println "\nContextual")
  (let [e' (c/-compile expr)]
    (cc/quick-bench
     (p/-invoke e' {})))
  (println "\nSCI")
  (cc/quick-bench
   (sci/eval-form scitx expr))
  (println))

(defn all-benchmarks
  []
  (bench-vs simple-expr)
  (bench-vs simple-let)
  (bench-vs nested-let)
  (bench-vs string-1)
  (bench-vs string-3)
  (bench-vs string-10)
  (bench-vs nested-str3)
  )

(defn all-full-benchmarks
  []
  (bench-full-vs simple-expr)
  (bench-full-vs simple-let)
  (bench-full-vs nested-let)
  (bench-full-vs string-1)
  (bench-full-vs string-3)
  (bench-full-vs string-10)
  (bench-full-vs nested-str3)
  )

(comment
  (all-full-benchmarks)
  (all-benchmarks))

;;; Benchmarking expr: (+ 1 2)
;;; Contextual
;;; Evaluation count : 1120118280 in 60 samples of 18668638 calls.
;;;              Execution time mean : 44.699888 ns
;;;     Execution time std-deviation : 0.898814 ns
;;;    Execution time lower quantile : 43.714308 ns ( 2.5%)
;;;    Execution time upper quantile : 47.216891 ns (97.5%)
;;;                    Overhead used : 9.615146 ns
;;;
;;; Found 5 outliers in 60 samples (8.3333 %)
;;; 	low-severe	 3 (5.0000 %)
;;; 	low-mild	 2 (3.3333 %)
;;;  Variance from outliers : 9.3559 % Variance is slightly inflated by outliers
;;; SCI
;;; Evaluation count : 6264540 in 60 samples of 104409 calls.
;;;              Execution time mean : 9.762819 µs
;;;     Execution time std-deviation : 334.990842 ns
;;;    Execution time lower quantile : 9.405957 µs ( 2.5%)
;;;    Execution time upper quantile : 10.590253 µs (97.5%)
;;;                    Overhead used : 9.615146 ns
;;;
;;; Found 5 outliers in 60 samples (8.3333 %)
;;; 	low-severe	 4 (6.6667 %)
;;; 	low-mild	 1 (1.6667 %)
;;;  Variance from outliers : 20.6283 % Variance is moderately inflated by outliers
;;;
;;; Benchmarking expr: (let [x 1 y 2] (+ x y))
;;; Contextual
;;; Evaluation count : 40425060 in 60 samples of 673751 calls.
;;;              Execution time mean : 1.493608 µs
;;;     Execution time std-deviation : 28.630845 ns
;;;    Execution time lower quantile : 1.462026 µs ( 2.5%)
;;;    Execution time upper quantile : 1.571459 µs (97.5%)
;;;                    Overhead used : 9.615146 ns
;;;
;;; Found 7 outliers in 60 samples (11.6667 %)
;;; 	low-severe	 6 (10.0000 %)
;;; 	low-mild	 1 (1.6667 %)
;;;  Variance from outliers : 7.8412 % Variance is slightly inflated by outliers
;;; SCI
;;; Evaluation count : 1895700 in 60 samples of 31595 calls.
;;;              Execution time mean : 32.030505 µs
;;;     Execution time std-deviation : 673.357839 ns
;;;    Execution time lower quantile : 31.339457 µs ( 2.5%)
;;;    Execution time upper quantile : 33.727690 µs (97.5%)
;;;                    Overhead used : 9.615146 ns
;;;
;;; Found 2 outliers in 60 samples (3.3333 %)
;;; 	low-severe	 2 (3.3333 %)
;;;  Variance from outliers : 9.4107 % Variance is slightly inflated by outliers
;;;
;;; Benchmarking expr: (let [x 1] (let [y 2] (+ x y)))
;;; Contextual
;;; Evaluation count : 31178340 in 60 samples of 519639 calls.
;;;              Execution time mean : 1.948790 µs
;;;     Execution time std-deviation : 33.639915 ns
;;;    Execution time lower quantile : 1.907390 µs ( 2.5%)
;;;    Execution time upper quantile : 2.034007 µs (97.5%)
;;;                    Overhead used : 9.615146 ns
;;;
;;; Found 5 outliers in 60 samples (8.3333 %)
;;; 	low-severe	 5 (8.3333 %)
;;;  Variance from outliers : 6.2782 % Variance is slightly inflated by outliers
;;; SCI
;;; Evaluation count : 1469340 in 60 samples of 24489 calls.
;;;              Execution time mean : 41.373357 µs
;;;     Execution time std-deviation : 957.141300 ns
;;;    Execution time lower quantile : 40.438712 µs ( 2.5%)
;;;    Execution time upper quantile : 44.146026 µs (97.5%)
;;;                    Overhead used : 9.615146 ns
;;;
;;; Found 5 outliers in 60 samples (8.3333 %)
;;; 	low-severe	 2 (3.3333 %)
;;; 	low-mild	 3 (5.0000 %)
;;;  Variance from outliers : 11.0044 % Variance is moderately inflated by outliers
;;;
;;; Benchmarking expr: (str 1)
;;; Contextual
;;; Evaluation count : 417181620 in 60 samples of 6953027 calls.
;;;              Execution time mean : 135.819063 ns
;;;     Execution time std-deviation : 3.508690 ns
;;;    Execution time lower quantile : 132.156693 ns ( 2.5%)
;;;    Execution time upper quantile : 142.767954 ns (97.5%)
;;;                    Overhead used : 9.615146 ns
;;;
;;; Found 2 outliers in 60 samples (3.3333 %)
;;; 	low-severe	 2 (3.3333 %)
;;;  Variance from outliers : 12.6396 % Variance is moderately inflated by outliers
;;; SCI
;;; Evaluation count : 6627720 in 60 samples of 110462 calls.
;;;              Execution time mean : 9.274174 µs
;;;     Execution time std-deviation : 267.538610 ns
;;;    Execution time lower quantile : 9.015513 µs ( 2.5%)
;;;    Execution time upper quantile : 9.991612 µs (97.5%)
;;;                    Overhead used : 9.615146 ns
;;;
;;; Found 9 outliers in 60 samples (15.0000 %)
;;; 	low-severe	 4 (6.6667 %)
;;; 	low-mild	 5 (8.3333 %)
;;;  Variance from outliers : 15.7974 % Variance is moderately inflated by outliers
;;;
;;; Benchmarking expr: (str 1 2 3)
;;; Contextual
;;; Evaluation count : 282818520 in 60 samples of 4713642 calls.
;;;              Execution time mean : 206.056654 ns
;;;     Execution time std-deviation : 2.778767 ns
;;;    Execution time lower quantile : 202.658145 ns ( 2.5%)
;;;    Execution time upper quantile : 211.704606 ns (97.5%)
;;;                    Overhead used : 9.615146 ns
;;;
;;; Found 2 outliers in 60 samples (3.3333 %)
;;; 	low-severe	 2 (3.3333 %)
;;;  Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
;;; SCI
;;; Evaluation count : 5991120 in 60 samples of 99852 calls.
;;;              Execution time mean : 10.084744 µs
;;;     Execution time std-deviation : 287.457252 ns
;;;    Execution time lower quantile : 9.754388 µs ( 2.5%)
;;;    Execution time upper quantile : 10.832135 µs (97.5%)
;;;                    Overhead used : 9.615146 ns
;;;
;;; Found 5 outliers in 60 samples (8.3333 %)
;;; 	low-severe	 2 (3.3333 %)
;;; 	low-mild	 3 (5.0000 %)
;;;  Variance from outliers : 15.7763 % Variance is moderately inflated by outliers
;;;
;;; Benchmarking expr: (str 1 2 3 4 5 6 7 8 9 10)
;;; Contextual
;;; Evaluation count : 126166620 in 60 samples of 2102777 calls.
;;;              Execution time mean : 472.380161 ns
;;;     Execution time std-deviation : 10.396655 ns
;;;    Execution time lower quantile : 461.260238 ns ( 2.5%)
;;;    Execution time upper quantile : 500.942020 ns (97.5%)
;;;                    Overhead used : 9.615146 ns
;;;
;;; Found 8 outliers in 60 samples (13.3333 %)
;;; 	low-severe	 3 (5.0000 %)
;;; 	low-mild	 5 (8.3333 %)
;;;  Variance from outliers : 9.4624 % Variance is slightly inflated by outliers
;;; SCI
;;; Evaluation count : 4949760 in 60 samples of 82496 calls.
;;;              Execution time mean : 12.283282 µs
;;;     Execution time std-deviation : 269.201358 ns
;;;    Execution time lower quantile : 11.971501 µs ( 2.5%)
;;;    Execution time upper quantile : 12.884596 µs (97.5%)
;;;                    Overhead used : 9.615146 ns
;;;
;;; Found 7 outliers in 60 samples (11.6667 %)
;;; 	low-severe	 6 (10.0000 %)
;;; 	low-mild	 1 (1.6667 %)
;;;  Variance from outliers : 9.4578 % Variance is slightly inflated by outliers
;;;
;;; Benchmarking expr: (str 1 (str 2 (str 3)))
;;; Contextual
;;; Evaluation count : 220127880 in 60 samples of 3668798 calls.
;;;              Execution time mean : 267.387943 ns
;;;     Execution time std-deviation : 4.567354 ns
;;;    Execution time lower quantile : 259.143600 ns ( 2.5%)
;;;    Execution time upper quantile : 276.919611 ns (97.5%)
;;;                    Overhead used : 9.615146 ns
;;;
;;; Found 3 outliers in 60 samples (5.0000 %)
;;; 	low-severe	 3 (5.0000 %)
;;;  Variance from outliers : 6.2700 % Variance is slightly inflated by outliers
;;; SCI
;;; Evaluation count : 2583300 in 60 samples of 43055 calls.
;;;              Execution time mean : 23.643595 µs
;;;     Execution time std-deviation : 479.989842 ns
;;;    Execution time lower quantile : 23.165250 µs ( 2.5%)
;;;    Execution time upper quantile : 24.715076 µs (97.5%)
;;;                    Overhead used : 9.615146 ns
;;;
;;; Found 5 outliers in 60 samples (8.3333 %)
;;; 	low-severe	 3 (5.0000 %)
;;; 	low-mild	 2 (3.3333 %)
;;;  Variance from outliers : 9.3681 % Variance is slightly inflated by outliers

(comment)

;;; Machine

;;; OS: Ubuntu 20.04 focal
;;; Kernel: x86_64 Linux 5.4.0-52-generic
;;; CPU: Intel Core i7-6820HQ @ 8x 3.6GHz [60.0°C]

(comment)

;;; Java

;;; openjdk version "1.8.0_272"
;;; OpenJDK Runtime Environment (build 1.8.0_272-8u272-b10-0ubuntu1~20.04-b10)
;;; OpenJDK 64-Bit Server VM (build 25.272-b10, mixed mode)

(def bench-env
  '{:benchmarks
    [{:name :contextual, :fn contextual.impl.protocols/-invoke :args [:state/expression :param/empty]}
     {:name :sci, :fn sci.core/eval-form, :args [:state/sci-env :param/expr]}]

    :states
    {:expression {:setup {:fn contextual.impl.compile/-compile
                          :args [:param/expr]}}
     :sci-env {:setup {:fn sci.core/init :args [:param/empty]}}}

    :params
    {:empty {}}
})

(def bench-opts
  {:type :quick
   :params {:expr [
                   '(+ 1 2)
                   '(let [x 1 y 2] (+ x y))
                   '(let [x 1] (let [y 2] (+ x y)))
                   '(str 1)
                   '(str 1 2 3)
                   '(str 1 2 3 4 5 6 7 8 9 10)
                   '(str 1 (str 2 (str 3)))
                   ]}
   :status true
   #_#_
   :profilers ["gc"]})

(jmh/run bench-env bench-opts)
(def res *1)
