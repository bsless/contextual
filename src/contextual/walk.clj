(ns contextual.walk)

(defprotocol IWalk
  (-walk [coll f]
    "If coll is a collection, applies f to each element of the
  collection and returns a collection of the results, of the same type
  and order as coll. If coll is not a collection, returns it unchanged.
  \"Same type\" means a type with the same behavior. For example, a
  hash-map may be returned as an array-map, but a a sorted-map will be
  returned as a sorted-map with the same comparator."))

(extend-protocol IWalk
  nil
  (-walk [coll f] nil)
  java.lang.Object  ; default: not a collection
  (-walk [x f] x)
  clojure.lang.IMapEntry
  (-walk [coll f]
    (clojure.lang.MapEntry. (f (.key coll)) (f (.val coll))))
  clojure.lang.ISeq  ; generic sequence fallback
  (-walk [coll f]
    (map f coll))
  clojure.lang.PersistentList  ; special case to preserve type
  (-walk [coll f]
    (apply list (map f coll)))
  clojure.lang.PersistentList$EmptyList  ; special case to preserve type
  (-walk [coll f] '())
  clojure.lang.IRecord  ; any defrecord
  (-walk [coll f]
    (reduce (fn [r x] (conj r (f x))) coll coll)))

(defn- -walk-default [coll f]
  (into (empty coll) (map f) coll))

;; Persistent collections that don't support transients
(doseq [type [clojure.lang.PersistentArrayMap
              clojure.lang.PersistentHashMap
              clojure.lang.PersistentVector
              clojure.lang.PersistentHashSet
              clojure.lang.PersistentQueue
              clojure.lang.PersistentStructMap
              clojure.lang.PersistentTreeMap
              clojure.lang.PersistentTreeSet]]
  (extend type IWalk {:-walk -walk-default}))

(defn walk
  "Traverses form, an arbitrary data structure.  inner and outer are
  functions.  Applies inner to each element of form, building up a
  data structure of the same type, then applies outer to the result.
  Recognizes all Clojure data structures. Consumes seqs as with doall."
  {:added "1.1"}
  [inner outer form]
  (outer (-walk form inner)))

(defn postwalk
  "Performs a depth-first, post-order traversal of form.  Calls f on
  each sub-form, uses f's return value in place of the original.
  Recognizes all Clojure data structures. Consumes seqs as with doall."
  {:added "1.1"}
  [f form]
  (walk (partial postwalk f) f form))
