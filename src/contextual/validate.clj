(ns contextual.validate
  (:require
   [contextual.impl.compile :as c]
   [contextual.impl.validate :as v]))

(defn validate-expression
  [expr lookup registry]
  (let [reg (merge c/symbols-registry registry)
        unresolvable-symbols (v/unresolvable-symbols
                              expr
                              lookup
                              (merge c/symbols-registry registry))
        bad-calls (v/bad-calls expr (merge reg lookup))]
    (cond-> {}
      (seq unresolvable-symbols)
      (assoc :unresolvable-symbols unresolvable-symbols)
      (seq bad-calls)
      (assoc :bad-function-calls bad-calls))))
