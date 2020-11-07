(ns contextual.bench
  (:require
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

(defn bench-vs
  [expr]
  (println "Benchmarking expr:" expr)
  (println "Contextual")
  (let [e' (c/-compile expr)]
    (cc/bench
     (p/-invoke e' {})))
  (println "SCI")
  (cc/bench
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

(comment
  (all-benchmarks))

;;; Benchmarking expr: (+ 1 2)
;;; Contextual
;;; Evaluation count : 988088280 in 60 samples of 16468138 calls.
;;;              Execution time mean : 52.216172 ns
;;;     Execution time std-deviation : 0.831800 ns
;;;    Execution time lower quantile : 51.455915 ns ( 2.5%)
;;;    Execution time upper quantile : 54.726135 ns (97.5%)
;;;                    Overhead used : 9.326739 ns
;;;
;;; Found 4 outliers in 60 samples (6.6667 %)
;;; 	low-severe	 1 (1.6667 %)
;;; 	low-mild	 3 (5.0000 %)
;;;  Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
;;; SCI
;;; Evaluation count : 6654240 in 60 samples of 110904 calls.
;;;              Execution time mean : 9.090925 µs
;;;     Execution time std-deviation : 171.491182 ns
;;;    Execution time lower quantile : 8.870779 µs ( 2.5%)
;;;    Execution time upper quantile : 9.461837 µs (97.5%)
;;;                    Overhead used : 9.326739 ns
;;;
;;; Found 1 outliers in 60 samples (1.6667 %)
;;; 	low-severe	 1 (1.6667 %)
;;;  Variance from outliers : 7.8251 % Variance is slightly inflated by outliers


;;; Benchmarking expr: (let [x 1 y 2] (+ x y))
;;; Contextual
;;; Evaluation count : 42638760 in 60 samples of 710646 calls.
;;;              Execution time mean : 1.408208 µs
;;;     Execution time std-deviation : 19.053441 ns
;;;    Execution time lower quantile : 1.379938 µs ( 2.5%)
;;;    Execution time upper quantile : 1.441522 µs (97.5%)
;;;                    Overhead used : 9.326739 ns
;;;
;;; Found 1 outliers in 60 samples (1.6667 %)
;;; 	low-severe	 1 (1.6667 %)
;;;  Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
;;; SCI
;;; Evaluation count : 1918560 in 60 samples of 31976 calls.
;;;              Execution time mean : 31.283480 µs
;;;     Execution time std-deviation : 539.940171 ns
;;;    Execution time lower quantile : 30.556673 µs ( 2.5%)
;;;    Execution time upper quantile : 32.722681 µs (97.5%)
;;;                    Overhead used : 9.326739 ns
;;;
;;; Found 4 outliers in 60 samples (6.6667 %)
;;; 	low-severe	 4 (6.6667 %)
;;;  Variance from outliers : 6.2781 % Variance is slightly inflated by outliers


;;; Benchmarking expr: (let [x 1] (let [y 2] (+ x y)))
;;; Contextual
;;; Evaluation count : 33180600 in 60 samples of 553010 calls.
;;;              Execution time mean : 1.828433 µs
;;;     Execution time std-deviation : 27.547059 ns
;;;    Execution time lower quantile : 1.795495 µs ( 2.5%)
;;;    Execution time upper quantile : 1.887780 µs (97.5%)
;;;                    Overhead used : 9.326739 ns
;;;
;;; Found 2 outliers in 60 samples (3.3333 %)
;;; 	low-severe	 2 (3.3333 %)
;;;  Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
;;; SCI
;;; Evaluation count : 1466940 in 60 samples of 24449 calls.
;;;              Execution time mean : 40.829540 µs
;;;     Execution time std-deviation : 510.452734 ns
;;;    Execution time lower quantile : 40.044852 µs ( 2.5%)
;;;    Execution time upper quantile : 41.971123 µs (97.5%)
;;;                    Overhead used : 9.326739 ns
;;;
;;; Found 1 outliers in 60 samples (1.6667 %)
;;; 	low-severe	 1 (1.6667 %)
;;;  Variance from outliers : 1.6389 % Variance is slightly inflated by outliers

;;; Benchmarking expr: (str 1)
;;; Contextual
;;; Evaluation count : 419614800 in 60 samples of 6993580 calls.
;;;              Execution time mean : 133.643437 ns
;;;     Execution time std-deviation : 2.299522 ns
;;;    Execution time lower quantile : 130.555985 ns ( 2.5%)
;;;    Execution time upper quantile : 138.839620 ns (97.5%)
;;;                    Overhead used : 9.326739 ns
;;;
;;; Found 2 outliers in 60 samples (3.3333 %)
;;; 	low-severe	 2 (3.3333 %)
;;;  Variance from outliers : 6.2757 % Variance is slightly inflated by outliers
;;; SCI
;;; Evaluation count : 6991380 in 60 samples of 116523 calls.
;;;              Execution time mean : 8.826380 µs
;;;     Execution time std-deviation : 179.829383 ns
;;;    Execution time lower quantile : 8.586937 µs ( 2.5%)
;;;    Execution time upper quantile : 9.153574 µs (97.5%)
;;;                    Overhead used : 9.326739 ns

;;; Benchmarking expr: (str 1 2 3)
;;; Contextual
;;; Evaluation count : 283245060 in 60 samples of 4720751 calls.
;;;              Execution time mean : 207.236294 ns
;;;     Execution time std-deviation : 3.584536 ns
;;;    Execution time lower quantile : 202.201279 ns ( 2.5%)
;;;    Execution time upper quantile : 214.044644 ns (97.5%)
;;;                    Overhead used : 9.326739 ns
;;;
;;; Found 1 outliers in 60 samples (1.6667 %)
;;; 	low-severe	 1 (1.6667 %)
;;;  Variance from outliers : 6.2798 % Variance is slightly inflated by outliers
;;; SCI
;;; Evaluation count : 6415860 in 60 samples of 106931 calls.
;;;              Execution time mean : 9.367703 µs
;;;     Execution time std-deviation : 110.356482 ns
;;;    Execution time lower quantile : 9.217665 µs ( 2.5%)
;;;    Execution time upper quantile : 9.540217 µs (97.5%)
;;;                    Overhead used : 9.326739 ns
;;;
;;; Found 1 outliers in 60 samples (1.6667 %)
;;; 	low-severe	 1 (1.6667 %)
;;;  Variance from outliers : 1.6389 % Variance is slightly inflated by outliers

;;; Benchmarking expr: (str 1 2 3 4 5 6 7 8 9 10)
;;; Contextual
;;; Evaluation count : 122713620 in 60 samples of 2045227 calls.
;;;              Execution time mean : 490.966915 ns
;;;     Execution time std-deviation : 10.678093 ns
;;;    Execution time lower quantile : 479.602991 ns ( 2.5%)
;;;    Execution time upper quantile : 512.620201 ns (97.5%)
;;;                    Overhead used : 9.326739 ns
;;;
;;; Found 1 outliers in 60 samples (1.6667 %)
;;; 	low-severe	 1 (1.6667 %)
;;;  Variance from outliers : 9.4495 % Variance is slightly inflated by outliers
;;; SCI
;;; Evaluation count : 5389620 in 60 samples of 89827 calls.
;;;              Execution time mean : 11.227449 µs
;;;     Execution time std-deviation : 81.187297 ns
;;;    Execution time lower quantile : 11.118000 µs ( 2.5%)
;;;    Execution time upper quantile : 11.390639 µs (97.5%)
;;;                    Overhead used : 9.326739 ns
;;;
;;; Found 1 outliers in 60 samples (1.6667 %)
;;; 	low-severe	 1 (1.6667 %)
;;;  Variance from outliers : 1.6389 % Variance is slightly inflated by outliers

;;; Benchmarking expr: (str 1 (str 2 (str 3)))
;;; Contextual
;;; Evaluation count : 127902900 in 60 samples of 2131715 calls.
;;;              Execution time mean : 467.069346 ns
;;;     Execution time std-deviation : 9.748048 ns
;;;    Execution time lower quantile : 453.234661 ns ( 2.5%)
;;;    Execution time upper quantile : 488.957407 ns (97.5%)
;;;                    Overhead used : 9.326739 ns
;;;
;;; Found 3 outliers in 60 samples (5.0000 %)
;;; 	low-severe	 3 (5.0000 %)
;;;  Variance from outliers : 9.4021 % Variance is slightly inflated by outliers
;;; SCI
;;; Evaluation count : 2688660 in 60 samples of 44811 calls.
;;;              Execution time mean : 22.616603 µs
;;;     Execution time std-deviation : 352.588449 ns
;;;    Execution time lower quantile : 22.088745 µs ( 2.5%)
;;;    Execution time upper quantile : 23.539851 µs (97.5%)
;;;                    Overhead used : 9.326739 ns
;;;
;;; Found 4 outliers in 60 samples (6.6667 %)
;;; 	low-severe	 4 (6.6667 %)
;;;  Variance from outliers : 1.6389 % Variance is slightly inflated by outliers


;;; Machine

;;; OS: Ubuntu 20.04 focal
;;; Kernel: x86_64 Linux 5.4.0-52-generic
;;; CPU: Intel Core i7-6820HQ @ 8x 3.6GHz [60.0°C]

;;; Java

;;; openjdk version "1.8.0_272"
;;; OpenJDK Runtime Environment (build 1.8.0_272-8u272-b10-0ubuntu1~20.04-b10)
;;; OpenJDK 64-Bit Server VM (build 25.272-b10, mixed mode)
