(ns contextual.core
  (:refer-clojure :exclude [compile])
  (:require
   [contextual.impl.utils :as u]
   [contextual.impl.protocols :as p]
   [contextual.impl.compile :as c]
   [contextual.impl.path :as path]))

(defn compile
  "Compile an expression into an [[invoke]]able class structure
  representing its evaluation. Takes two optional arguments:
  - `lookup`: a symbol -> value map for symbol resolution during
  expression compilation. These would typically be constants (strings,
  numbers), predefined paths (see [[path]]) or functions. To get all the
  public symbols in a namespace as valid lookup values, use
  [[namespace->lookup]] for convenience.
  - `registry`: Extension entry point for developers. Allows adding new
  special forms during compilation. Requires familiarity with library
  implementation details, use with care."
  ([expr]
   (compile expr {}))
  ([expr lookup]
   (compile expr lookup {}))
  ([expr lookup registry]
   (c/-compile expr lookup registry)))

(defn path
  [& args]
  (apply path/->path args))

(defn namespaces->lookup
  "Take a coll of namespaces and return a map of all their publicly
  defined symbols to their corresponding vars by way of [[ns-publics]].
  To deref the vars, pass the optional arg `deref?` a truthy value."
  ([namespaces]
   (namespaces->lookup false namespaces))
  ([namespaces deref?]
   (into
    {}
    (comp
     (map ns-publics)
     cat
     (if deref? (map (fn [[k v]] [k (deref v)])) identity))
    namespaces)))

(defn invoke
  [expr context]
  (p/-invoke expr context))

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
