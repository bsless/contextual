(ns contextual.impl.box
  (:require
   [contextual.impl.protocols :as p]))

(deftype Box [x]
  Object
  (toString [this]
    (str "#<Box " (pr-str x) ">"))
  (equals [this that]
    (if (instance? Box that)
      (= x (p/-get that))
      false))
  p/IBox
  (-get [this] x)
  (-boxed? [this] true)
  p/IJoin
  (-join [this] x)
  p/IContext
  (-invoke [this ctx] x)
  p/IStringBuild
  (-invoke-with-builder [this ctx sb]
    (if (nil? x)
      nil
      (.append ^StringBuilder sb x))))

(defmethod print-method Box [v ^java.io.Writer w]
  (.write w (str v)))

(defn ->box [x] (->Box x))
(comment
  (pr-str
   (->box 1)))

(defn unbox
  [x]
  (if (p/-boxed? x)
    (p/-get x)
    x))
