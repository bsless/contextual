(ns contextual.impl.let
  (:require
   [contextual.walk :as walk]
   [contextual.impl.protocols :as p]
   [contextual.impl.box :as b]
   [contextual.impl.environment :as e]))

(defn- symbol-lookup
  [lookup sym]
  (if (symbol? sym) (get lookup sym sym) sym))

(defn binding-symbol
  [s]
  (with-meta s (assoc (meta s) :binding true)))

(defn binding-symbol?
  [s]
  (boolean (:binding (meta s))))

(defn bindings->ssa
  [bindings]
  (let [bs (partition 2 bindings)]
    (loop [bs bs
           seen {}
           ssa []
           trace []]
      (if (seq bs)
        (let [[[b e] & bs] bs
              sym (gensym (str b "__"))
              e (walk/postwalk (partial symbol-lookup seen) e)
              seen (assoc seen b sym)
              ssa (conj ssa (binding-symbol sym) e)
              trace (conj trace seen)]
          (recur bs seen ssa trace))
        {:bindings ssa :seen seen :trace trace}))))

(comment
  (bindings->ssa '[a 1
                   a (+ a 1)
                   b 2
                   c (+ a b)]))

(defn let->ssa
  [[_let bs & body]]
  (let [{:keys [bindings seen]} (bindings->ssa bs)
        body (walk/postwalk (partial symbol-lookup seen) body)]
    (concat (list 'let bindings) body)))

(comment
  (let->ssa
   '(let [a 1
          a (+ a 1)
          b 2
          c (+ a b)]
      (println a b)
      (* c b))))

(defn ssa-bindings
  [expr]
  (walk/postwalk
   (fn [expr]
     (cond
       (seq? expr)
       (let [[f] expr]
         (case f
           let (let->ssa expr)
           expr))
       :else expr))
   expr))

(defn occurances
  [sym expr]
  (get
   (->> expr
        (tree-seq coll? identity)
        (filter
         symbol?)
        frequencies)
   sym
   0))

(defn -inline
  [s lookup expr]
  (walk/postwalk
   (fn [expr]
     (if (= s expr)
       (get lookup s)
       expr))
   expr))

(comment
  (-inline 'a '{a x} '(+ 1 (* 2 a))))

(defn inline-let*
  "Takes let after ssa"
  [bs body]
  (let [m (into {} (partition-all 2) bs)]
    (loop [[s _ & bs] bs
           bs' []
           m m
           body body]
      (if s
        (let [n (+ (occurances s bs)
                   (occurances s body))]
          (if (= 1 n)
            (let [bs (-inline s m bs)
                  body (-inline s m body)
                  m (-inline s m m)]
              (recur bs bs' m body))
            (recur bs (conj bs' s (m s)) m body)))
        [bs' body]))))

(comment
  (let [bs '[x 1 y 2 z (+ x y)]
        body '(+ z 2)]
    (inline-let* bs body))

  (let [bs '[x 1 y 2 z (+ x y)]
        body '(+ z x)]
    (inline-let* bs body)))

(defn inline-let1
  [[_let bs body :as expr]]
  (if (= _let 'let)
    (let [[-bs -body] (inline-let* bs body)]
      (if (seq -bs)
        (list 'let -bs -body)
        -body))
    expr))

(defn inline-let
  [expr]
  (let [expr' (inline-let1 expr)]
    (if (= expr expr')
      expr
      (recur expr'))))

(comment
  (inline-let1
   '(let [x 1
          y 2
          z (+ x y)]
      (+ z 2)))

  (inline-let
   '(let [x 1
          y 2
          z (+ x y)
          d z]
      (+ z 2)))

  (inline-let
   '(let [x 1
          y 2
          z (+ x y)]
      (let [w (+ z 2)]
        (+ w 4)))))

(defrecord Lookup [sym]
  p/IContext
  (-invoke [this ctx]
    (let [e (e/getenv ctx)]
      (e/lookup e sym))))

(defn ->lookup
  [sym]
  (->Lookup sym))

(comment
  (->lookup 's))

(defn- accum-env
  [ctx]
  (fn [e [s expr]]
    (let [v (p/-invoke expr (e/with-env ctx e))]
      (assoc e s v))))

(defrecord Bindings [bindings]
  p/IContext
  (-invoke [this ctx]
    (reduce
     (accum-env ctx)
     {}
     bindings)))

(defonce ^:private binding-builders (atom {}))

(defmacro ^:private def-bindings []
  (let [name "Bindings"
        ctx 'ctx
        env 'e
        defs
        (for [n (range 1 13)
              :let [syms (map (comp symbol #(str "sym" %)) (range n))
                    exprs (map (comp symbol #(str "expr" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    bindings (reduce
                              (fn [bs [s e]]
                                (conj bs env `(assoc ~env ~s (p/-invoke ~e (e/with-env ~ctx ~env)))))
                              `[~env {}]
                              (map vector syms exprs))
                    body `(let [~@bindings] ~env)]]
          `(do
             (defrecord ~rec [~@(interleave syms exprs)]
               p/IContext
               (-invoke [~'this ~ctx]
                ~body))
             (swap! binding-builders assoc ~n ~constructor)))]
    `(do
       ~@defs)))

(def-bindings)

(defn- unparse-bindings
  [args]
  (let [v (b/unbox args)]
    (into [] (comp (partition-all 2) (map (fn [[k v]] [(b/unbox k) v])) cat) v)))

(defn ->bindings
  [args]
  (let [args (unparse-bindings args)
        n (/ (count args) 2)
        c (get @binding-builders n)]
    (if c
      (apply c args)
      (->Bindings (into [] (partition-all 2) args)))))

(defrecord Let [bindings expr]
  p/IContext
  (-invoke [this ctx]
    (let [e (p/-invoke bindings ctx)]
      (p/-invoke expr (e/with-env ctx e)))))

(defn ->let
  [bindings expr]
  (->Let (->bindings bindings) expr))

