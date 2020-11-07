(ns contextual.core
  (:refer-clojure :exclude [compile])
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.compile :as c]))

(defn compile
  ([expr]
   (compile expr {}))
  ([expr lookup]
   (c/-compile expr lookup)))

(defn invoke
  [expr context]
  (p/-invoke expr context))
