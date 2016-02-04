(ns framed.external.set
  (:require [clojure.java.io :as io]
            [clojure.data.fressian :as fressian]
            (riffle read write)
            [tesser.core :as t]
            (framed.std
              [core :as std]
              [io :as std.io])))

(deftype ExternalSet [riff]
  clojure.lang.IPersistentSet
  (seq [self]
    (let [entries (riffle.read/entries riff)]
      (when (seq entries)
        (->> entries
             (map (fn [[k v]] (fressian/read k)))
             dedupe))))
  (count [this]
    (count (seq this)))
  (get [this k]
    (when (riffle.read/get riff (fressian/write k))
      k))
  (contains [this k]
    (boolean (get this k)))

  clojure.lang.IFn
  (invoke [this n] (get this n))

  Object
  (toString [this] (pr-str this))

  clojure.java.io/Coercions
  (as-file [rs] (io/file riff)))

(alter-meta! #'->ExternalSet assoc :private true)

(defmethod print-method ExternalSet [this ^java.io.Writer w]
  (.write w (format "#<ExternalSet %s>" (.. this riff file))))

(defn ^:no-doc path->ExternalSet [path]
  (-> path riffle.read/riffle ->ExternalSet))

(defn ^:no-doc coll->ExternalSet [coll]
  (let [f (std.io/tempfile "riffle-set" ".riffle")
        path (.getPath f)]
    (->> coll
         (map (fn [x] [(fressian/write x) ""]))
         (std/flip riffle.write/write-riffle path)
         path->ExternalSet)))

(defn union'
  "Set union assuming all sets are ExternalSets; uses Riffle's
   efficient merge procedure and returns a new ExternalSet"
  [& sets]
  (let [f (std.io/tempfile)]
    (->> f
         (riffle.write/merge-riffles
           (fn [x y] x) ; this merge fn is for merging maps, irrelevant for sets
           (map io/file sets))
         (path->ExternalSet))))

(defn union
  "Construct a new ExternalSet that is the union of set-like s1 and s2
   Taken from http://aphyr.github.io/tesser/tesser.core.html#var-fold"
  [s1 s2]
  (->> (t/filter identity)
       (t/fold {:identity sorted-set
                :reducer conj
                :combiner into})
       (t/tesser (t/chunk 8192 (concat s1 s2)))
       (coll->ExternalSet)))

(defn difference
  "Construct a new ExternalSet that is the elements of the first set
   without the elements of the following sets"
  [s1 & sets]
  (->> (seq s1)
       (filter (fn [x] (not-any? #(contains? % x) sets)))
       coll->ExternalSet))
