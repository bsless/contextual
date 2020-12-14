(ns contextual.util)

(defn ^:private preserving-reduced
  [rf]
  #(let [ret (rf %1 %2)]
     (if (reduced? ret)
       (reduced ret)
       ret)))

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

(defn trim-sb
  [^StringBuilder sb]
  (.setLength sb (unchecked-dec-int (.length sb))))
