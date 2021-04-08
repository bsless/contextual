(ns contextual.validate
  (:require
   [contextual.impl.compile :as c]
   [contextual.impl.utils :as u]))

(defn- find-symbols
  "Find all symbols in `expr`"
  [expr]
  (cond
    (symbol? expr) expr
    (coll? expr) (->Eduction
                  (comp
                   (map find-symbols)
                   u/maybe-cat
                   (remove nil?))
                  expr)
    :else nil))

(defn unresolvable-symbols
  "Find all symbols in `expr` which cannot be resolved"
  [expr lookup registry]
  (into
   []
   (remove (some-fn lookup registry c/symbols-registry))
   (find-symbols expr)))
