(ns ^:no-doc framed.external.sort
  (:require [clojure.data.avl :as avl]
            [clojure.data.fressian :as fressian]
            [clojure.java.io :as io]
            (framed.std
              [io :as std.io]
              [serialization :as serialization]))
  (:refer-clojure :exclude [sort sort-by])
  (:import (java.io EOFException File)
           (org.fressian FressianReader)))

(def default-batchsize 200000)

(defn- fressian-file-seq
  "Return Fressian objects in sequence from file, deleting it when EOF is reached
   x - coercible using clojure.java.io/file"
  [x]
  (letfn [(read-fressian [^FressianReader reader ^File file]
            (lazy-seq
                (try (cons (fressian/read-object reader)
                           (read-fressian reader file))
                  (catch EOFException ex
                    (do (.close reader) (.delete file) nil)))))]
  (let [file (io/file x)
        reader (fressian/create-reader (io/input-stream file))]
    (read-fressian reader file))))

(defn- comp-tuple [compx]
  (fn [x y]
     (let [result (compx (get x 1) (get y 1))]
       (if (zero? result)
         (compare (get x 0) (get y 0))
         result))))

(defn join-seqs'
  "seq-vec is a vector that needs O(1) random access
   head-set is a persisted tree set to store the head element from each batch"
  [seq-vec head-set]
  (lazy-seq
    (when-not (empty? head-set)
      (let [next-vec (first head-set)
            [next-index next-value] next-vec
            next-seq (get seq-vec next-index)
            seq-vec' (assoc! seq-vec next-index (rest next-seq))
            head-set' (if (seq next-seq)
                        (conj (disj head-set next-vec) [next-index (first next-seq)])
                        (disj head-set next-vec))]
        (cons next-value (join-seqs' seq-vec' head-set'))))))

(defn- join-seqs [compx seqs]
  (let [seq-head-pairs (map-indexed vector (map first seqs))
        seq-tails (transient (mapv rest seqs))]
    (join-seqs'
      seq-tails
      (into (avl/sorted-set-by (comp-tuple compx)) seq-head-pairs))))

(defn- partition-and-presort
  "Return a vector of Fressian files, containing sorted
   partitions (specified by batchsize) of coll"
  [batchsize compx coll]
  (->> coll
       (partition-all batchsize)
       (pmap #(serialization/write-fressian
                (std.io/tempfile "external-sort")
                (clojure.core/sort compx %)))))

(defn sort
  "Sorts coll in constant space and return a lazy seq of the results

   batchsize - Integer representing max number of elements to write to a file
   compx - 3-way comparator to sort on (returning int, not boolean)
           (Defaults to clojure.core/compare)"
  ([batchsize coll]
   (sort batchsize compare coll))
  ([batchsize compx coll]
   (->> coll
        (partition-and-presort batchsize compx)
        (map fressian-file-seq)
        (join-seqs compx))))

(defn sort-by
  "Sort a coll in constant space, where sort order is determined
   by (keyfn item)"
  [batchsize keyfn coll]
  (sort batchsize (fn [x y] (compare (keyfn x) (keyfn y))) coll))
