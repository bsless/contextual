(ns contextual.http
  (:require
   [contextual.core :as c])
  (:import
   [java.net URLEncoder]))

(defn ^String url-encode
  [s]
  (URLEncoder/encode (str s) "utf8"))

(def ^:const path-sep \/)

(defrecord QueryParams [m]
  c/IContext
  (-invoke [this ctx]
    (persistent!
     (reduce-kv
      (fn [m k v]
        (let [k (c/-invoke k ctx)]
          (if (nil? k)
            m
            (let [v (c/-invoke v ctx)]
              (assoc! m k v)))))
      (transient {})
      m))))

(defrecord Request
    [url path query-params body form method]
  c/IContext
  (-invoke [this ctx]
    (let [url (c/-invoke url ctx)
          path (c/-invoke path ctx)
          query-params (c/-invoke query-params ctx)
          body (c/-invoke body ctx)
          form (c/-invoke form ctx)
          method (c/invoke method ctx)])
    (cond->
        {:url url}
      )))

(comment
  '(->request
    {:url (->url (->str ,,,))
     }))
