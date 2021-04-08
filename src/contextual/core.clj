(ns contextual.core
  (:refer-clojure :exclude [compile])
  (:require
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
  defined symbols to their corresponding vars by way of [[ns-publics]]."
  ([namespaces]
   (into
    {}
    (comp
     (map ns-publics)
     cat)
    namespaces)))

(defn invoke
  [expr context]
  (p/-invoke expr context))
