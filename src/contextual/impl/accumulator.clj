(ns contextual.impl.accumulator
  (:require
   [contextual.impl.protocols :as p]))

(defrecord Accumulator [factory finish args]
  p/IContext
  (-invoke [this ctx]
    (let [app (factory)]
      (p/-invoke-with-appendable this ctx app)
      (finish app)))
  p/IAppend
  (-invoke-with-appendable [this ctx a]
    (doseq [arg args]
      (p/-invoke-with-appendable arg ctx a))))

(defn- new-string-builder
  []
  (StringBuilder.))

(defn finalize-string-builder
  [^StringBuilder sb]
  (.toString sb))

(defn ->str!
  [args]
  (->Accumulator new-string-builder finalize-string-builder args))

(defn ->join!
  [delim args]
  (->str! (interpose delim args)))

(comment
  (p/-invoke
   (->str! [1 2 3])
   {}))

(defonce ^:private accum-builders (atom {}))

(defmacro ^:private def-accum []
  (let [ctx 'ctx
        name "Str"
        appendable (with-meta 'appendable {:tag "Appendable"})
        defs
        (for [n (range 1 23)
              :let [args (map (comp symbol #(str "a" %)) (range n))
                    rec (symbol (str name n))
                    constructor (symbol (str "->" rec))
                    parts (map (fn [arg]
                                 `(p/-default-invoke-with-appendable ~arg ~ctx ~appendable))
                               args)]]
          `(do
             (defrecord ~rec [~'factory ~'finish ~@args]
               p/IAppend
               (-invoke-with-appendable [~'this ~ctx ~'appendable]
                 ~@parts)
               p/IContext
               (-invoke [~'this ~ctx]
                (let [~appendable (~'factory)]
                  (p/-invoke-with-appendable ~'this ~ctx ~appendable)
                  (~'finish ~appendable))))
             (swap! accum-builders assoc ~n ~constructor)))]
    `(do
       ~@defs)))

(def-accum)

(defn ->str
  [& args]
  (let [n (count args)
        c (get @accum-builders n)]
    (if c
      (apply c new-string-builder finalize-string-builder args)
      (->Accumulator new-string-builder finalize-string-builder args))))

(comment
  (p/-invoke (->str 1 2 3) nil))

(defn ->join
  [delim & args]
  (let [args (interpose delim args)]
    (apply ->str args)))
