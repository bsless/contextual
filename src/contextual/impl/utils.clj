(ns contextual.impl.utils
  "Utility functions which don't belong anywhere else.")

(def preserving-reduced @#'clojure.core/preserving-reduced)

(defn maybe-cat
  "Like [[clojure.core/cat]] but falls through on non-sequential elements."
  [rf]
  (let [rrf (preserving-reduced rf)]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result input]
       (if (sequential? input)
         (reduce rrf result input)
         (rrf result input))))))
