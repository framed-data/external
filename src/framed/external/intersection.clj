(ns ^:no-doc framed.external.intersection
  (:require [framed.external.sort :as external.sort]))

(defn distinct-sorted
  ([coll]
   (distinct-sorted external.sort/default-batchsize compare coll))
  ([batchsize compfn coll]
   (->> (external.sort/sort batchsize compfn coll)
        dedupe)))

(defn intersection-pre
  "Given two pre-sorted, pre-distinct'd sequences, return the sequence
   of intersections between the sequences."
  [s0 s1]
  (if (or (empty? s0) (empty? s1))
    '()
    (let [e0 (first s0)
          e1 (first s1)]
      (lazy-seq
        (if (= e0 e1)
          (cons e0 (intersection-pre (rest s0) (rest s1)))
          (if (neg? (compare e0 e1))
            (intersection-pre (rest s0) s1)
            (intersection-pre s0 (rest s1))))))))

(defn intersection
  "Return the intersection of two comparable sequences as a lazy seq
   (Also works for ExternalSets and any seq-able objects)"
  ([s0 s1]
   (intersection external.sort/default-batchsize s0 s1))
  ([batchsize s0 s1]
   (intersection-pre
     (distinct-sorted batchsize compare s0)
     (distinct-sorted batchsize compare s1))))
