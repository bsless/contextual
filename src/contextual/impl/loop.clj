(ns contextual.impl.loop
  (:require
   [contextual.walk :refer [postwalk]]))

(defn trampoline-loop
  [[_ bindings & body]]
  (let [bindings (partition 2 bindings)
        params (mapv first bindings)
        fname (gensym)
        body' (postwalk
               (fn [expr]
                 (if (and (seq? expr) (= 'recur (first expr)))
                   `(trampoline ~fname ~@(rest expr))
                   expr))
               body)]
    `(letfn [(~fname [~@params] ~@body')]
       (~fname ~@(map second bindings)))))

(defn transform-loops
  [expr]
  (postwalk
   (fn [expr]
     (if (and (seq? expr) (= 'loop (first expr)))
       (trampoline-loop expr)
       expr))
   expr))

(comment
  (def expr
    '(loop [x 1
            xs []]
       (if (< x 10)
         (recur (inc x) (conj xs (loop [a x
                                        b 2]
                                   (if (> a 10)
                                     a
                                     (recur (+ a b) a)))))
         xs)))

  (eval expr)

  (eval (transform-loops expr)))
