(ns contextual.impl.validate)

(defn missing-symbols
  ([expr context]
   (missing-symbols expr context #{}))
  ([expr context found]
   (cond
     (symbol? expr)
     (if (contains? context expr)
       found
       (conj found expr))

     (list? expr)
     (let [[h & t] expr]
       (if (= 'let h)
         (let [[bindv & body] t
               bindv (partition 2 bindv)]
           (loop [bindv bindv
                  context context
                  found found]
             (if (seq bindv)
               (let [[[sym expr]] bindv
                     context (assoc context sym sym)]
                 (recur (rest bindv) context (missing-symbols expr context found)))
               (missing-symbols body context found))))
         (reduce
          (fn [found expr]
            (missing-symbols expr context found))
          found
          expr)))

     (map? expr)
     (reduce
      (fn [found expr]
        (missing-symbols expr context found))
      found
      (mapcat identity expr))

     (coll? expr)
     (reduce
      (fn [found expr]
        (missing-symbols expr context found))
      found
      expr)

     :else found)))

(defn unresolvable-symbols
  "Find all symbols in `expr` which cannot be resolved"
  [expr lookup registry]
  (missing-symbols expr (merge registry lookup)))

(defn arity-check-fn
  "Derive a function to validate the arity of an expression based on a var's metadata"
  [v]
  (when-let [arglists (:arglists (meta v))]
    (let [arglists (into [] (sort-by count arglists))
          biggest (peek arglists)
          restargs? (and (> (count biggest) 1) (= '& (peek (pop biggest))))]
      (if restargs?
        (let [allowed (into #{} (map count) (pop arglists))
              restargs (dec (count biggest))]
          (fn [n]
            (or
             (contains? allowed n)
             (>= n restargs))))
        (let [allowed (into #{} (map count) arglists)]
          (fn [n] (contains? allowed n)))))))

(defn callable?
  [v]
  (if (var? v)
    (callable? @v)
    (ifn? v)))

(defn with-validatation-meta
  "Annotate symbol `s` with a validation function returned
  from [[arity-check-fn]] and all other metadata on `v`"
  [s v]
  (if-let [f (arity-check-fn v)]
    (with-meta s (assoc (meta v) :validate-arity f))
    s))

(comment
  (with-validatation-meta '+ #'+))

(defn enrich-lookup-map
  [m]
  (into {} (map (fn [[k v]] [(with-validatation-meta k v) v])) m))

(comment
  (filter list? (tree-seq coll? seq '(+ 1 (- 2 (* 3 4))))))

(defn bad-calls
  "Try to find bad calls in expression `expr`. Symbols will be looked up in `m`.
  For maximal benefit, enrich the symbols with metadata using
  `with-validatation-meta`"
  [expr m]
  (let [calls (filter list? (tree-seq coll? seq expr))]
    (reduce
     (fn [failures call]
       (let [[sym & args] call
             [s _] (find m sym)
             f (:validate-arity (meta s))
             n (count args)]
         (cond-> failures
           (and f (not (f n)))
           (conj {:name sym :cause (str "Wrong number of arguments: " n)})
           (and f (not (callable? f)))
           (conj {:name sym :cause "Not a function"})
           (nil? f) (conj {:name sym :cause "Does not exist!"}))))
     []
     calls)))

(comment
  (def m (enrich-lookup-map {'+ #'+ '- #'- '* #'* '/ #'/}))
  (bad-calls '(+ 1 (- 2 (* 3 4))) m)
  (bad-calls '(+ 1 (- 2 (* 3 4))) (dissoc m '+))
  (bad-calls '(+ 1 (- 2 (* 3 4 (/ )))) m)
  )
