(ns contextual.core
  (:refer-clojure :exclude [compile])
  (:require
   [contextual.impl.protocols :as p]
   [contextual.impl.compile :as c]))

(defn compile
  ([expr]
   (compile expr {}))
  ([expr lookup]
   (compile expr lookup {}))
  ([expr lookup registry]
   (c/-compile expr lookup registry)))

(defn invoke
  [expr context]
  (p/-invoke expr context))
