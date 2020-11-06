(ns contextual.impl.control
  (:require
   [contextual.impl.protocols :as p]))

(defrecord If [p t e]
  p/IContext
  (-invoke [this ctx]
    (if (p/-invoke p ctx)
      (p/-invoke t ctx)
      (p/-invoke e ctx)))
  p/IStringBuild
  (-invoke-with-builder [this ctx sb]
    (if (p/-invoke p ctx)
      (p/-invoke-with-builder t ctx sb)
      (p/-invoke-with-builder e ctx sb))))

(defn ->if
  ([p t]
   (->If p t nil))
  ([p t e]
   (->If p t e)))

(defrecord Or [a b]
  p/IContext
  (-invoke [this ctx]
    (or
     (p/-invoke a ctx)
     (p/-invoke b ctx))))

(defn ->or
  ([])
  ([a] a)
  ([a b] (->Or a b)))

(defrecord And [a b]
  p/IContext
  (-invoke [this ctx]
    (and
     (p/-invoke a ctx)
     (p/-invoke b ctx))))

(defn ->and
  ([])
  ([a] a)
  ([a b] (->And a b)))
