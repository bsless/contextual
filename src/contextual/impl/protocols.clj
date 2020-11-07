(ns contextual.impl.protocols
  (:import
   (java.lang Appendable)))

(comment
  (instance? Appendable (StringBuilder.)))

(defprotocol IContext
  (-invoke [this ctx]))

(defprotocol IAppend
  (-invoke-with-appendable [this ctx a]))

(defprotocol IEnv
  (-lookup [this k] [this k nf])
  (-with [this k v] [this k v kvs]))

(extend-protocol IContext
  Object
  (-invoke [this ctx] this)
  nil
  (-invoke [this ctx] nil))

(definline -default-invoke-with-appendable
  [this ctx ^Appendable a]
  `(let [ret# (-invoke ~this ~ctx)]
     (if (nil? ret#)
       nil
       (.append
        ~(with-meta a {:tag "Appendable"})
        ret#))))

(extend-protocol IAppend
  Object
  (-invoke-with-appendable [this ctx a]
    (-default-invoke-with-appendable this ctx a))
  String
  (-invoke-with-appendable [this ctx a]
    (.append ^Appendable a this))
  nil
  (-invoke-with-appendable [this ctx a]))
