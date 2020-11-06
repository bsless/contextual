(ns contextual.impl.protocols
  (:import
   (java.lang StringBuilder Appendable)))

(defprotocol IContext
  (-invoke [this ctx]))

(defprotocol IStringBuild
  (-invoke-with-builder [this ctx sb]))

(defprotocol IEnv
  (-lookup [this k] [this k nf])
  (-with [this k v] [this k v kvs]))

(extend-protocol IContext
  Object
  (-invoke [this ctx] this)
  nil
  (-invoke [this ctx] nil))

(definline ^:private -default-invoke-with-builder
  [this ctx ^StringBuilder sb]
  `(let [ret# (-invoke ~this ~ctx)]
     (if (nil? ret#)
       nil
       (.append
        ~(with-meta sb {:tag "StringBuilder"})
        ret#))))

(extend-protocol IStringBuild
  Object
  (-invoke-with-builder [this ctx sb]
    (-default-invoke-with-builder this ctx sb))
  String
  (-invoke-with-builder [this ctx sb]
    (.append ^StringBuilder sb this))
  nil
  (-invoke-with-builder [this ctx sb]))
