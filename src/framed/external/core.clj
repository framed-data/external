(ns framed.external.core
  (:require [framed.std.serialization :as serialization]
            (framed.external
              [intersection :as external.intersection]
              [set :as external.set]
              [sort :as external.sort]))
  (:refer-clojure :exclude [distinct set shuffle sort sort-by]))

(def
  ^{:doc "Sorts coll in constant space and return a lazy seq of the results
       batchsize - Integer representing max number of elements to write to a file
       compx - 3-way comparator to sort on (returning int, not boolean)
               (Defaults to clojure.core/compare)"
    :arglists '([batchsize coll] [batchsize compx coll])}
  sort
  external.sort/sort)

(def
  ^{:doc "Sort a coll in constant space, where sort order is determined
     by (keyfn item)"
    :arglists '([batchsize keyfn coll])}
  sort-by
  external.sort/sort-by)

(defn keyed-partition-by
  "Applies f to each value in coll, splitting on each different f value.
   Returns a lazy seq of pairs, each containing the split value and the seq
   of values in the partition

   Note: lazy in the partitions produced. To accomplish this, f is applied
   twice to each element in partitions, so is unsuitable when f is expensive or
   has side-effects.

   See http://stackoverflow.com/questions/24738261/lazy-partition-by"
  [f coll]
  (lazy-seq
    (when (seq coll)
      (let [fst (first coll)
            v (f fst)
            run (lazy-seq (cons fst (take-while #(= v (f %)) (rest coll))))]
        (cons [v run]
              (keyed-partition-by f (drop-while #(= v (f %)) coll)))))))

(defn aggregate-by
  "Groups items in coll according to (key-func item) and uses map-func to
   transform the final partition values. Returns a lazy seq of partitions,
   of the form [k (map-func vs)]

   Invariant: Output will be sorted with respect to key-func

   Ex:
     (aggregate-by
       :user
       (fn [events] (count events))
       10000
       [{:user \"a\" :event \"Login\"}
        {:user \"b\" :event \"Signup\"}
        {:user \"a\" :event \"Purchase\"}
        {:user \"c\" :event \"Add To Cart\"}])
     ; => ([\"a\" 2] [\"b\" 1] [\"c\" 1])"
  ([key-func coll]
   (aggregate-by key-func identity coll))
  ([key-func map-func coll]
   (aggregate-by key-func map-func external.sort/default-batchsize coll))
  ([key-func map-func batchsize coll]
   (->> (external.sort/sort-by batchsize key-func coll)
        (keyed-partition-by key-func)
        (pmap (fn [[k vs]] [k (map-func vs)])))))

(def
  ^{:doc "Return the intersection of two comparable sequences as a lazy seq
     Ex:
       (def s1 (external/set [1 2 3]))
       (def s2 (external/set [2 3 4 5]))
       (e/intersection s1 s2)
       ; => (2 3)"
    :arglists '([s0 s1] [batchsize s0 s1])}
  intersection
  external.intersection/intersection)

(def
  ^{:doc "Construct an ExternalSet from a file path"
    :arglists '([path])}
  ->set
  external.set/path->ExternalSet)

(def
  ^{:doc "Construct an ExternalSet from coll"
    :arglists '([coll])}
  set
  external.set/coll->ExternalSet)

(defn distinct
  "Return a sequence of distinct elements in coll.
   Order of returned elements is undefined."
  [coll]
  (seq (external.set/coll->ExternalSet coll)))

(defn shuffle
  "Shuffle coll in constant space and return a lazy seq of the results

   rng - optional java.util.Random for deterministic testing"
  ([coll]
   (shuffle (java.util.Random.) coll))
  ([^java.util.Random rng coll]
   (let [batchsize external.sort/default-batchsize
         tagged-vs (->> coll
                        (map (fn [v] [(.nextLong rng) v]))
                        serialization/coll->NippySeq)]
       (->> (seq tagged-vs)
            (external.sort/sort-by batchsize first)
            (map second)))))
