(ns
  ^{:slow-tests true}
  framed.external.intersection-test
  (:require [clojure.test :refer :all]
            (framed.external
              [core :as external]
              [intersection :as ixn])
            [clojure.test.check :as tc]
            [clojure.set :as set]
            (clojure.test.check
              [clojure-test :refer [defspec]]
              [generators :as gen]
              [properties :as prop])))

(defspec test-distinct-sorted-check
  100
  (prop/for-all [s0 (gen/vector gen/int)]
    (= (sort (clojure.core/distinct s0))
       (ixn/distinct-sorted s0))))

(deftest test-intersersection-pre
  (is (= [2] (ixn/intersection-pre [0 1 2 3] [-1 2 4]))
      "It finds intersections.")
  (is (= '() (ixn/intersection-pre [0 1 2 3] [4 5 6]))
      "It finds nothing in with disjoint sequences")
  (is (= [3] (ixn/intersection-pre [2 3 4] [3 5 7]))
      "It finds intersections when the first seq is ahead")
  (is (= [5] (ixn/intersection-pre [3 5 7] [4 5 8]))
      "It find intersections when the second seq is ahead")
  (is (= [3] (ixn/intersection-pre [3 5 7] [3 6 9]))
      "It finds intersections when the heads are aligned")
  (testing "Empty or nil seqs"
    (is (= '() (ixn/intersection-pre [] [3 5 6])))
    (is (= '() (ixn/intersection-pre [3 5 6] [])))
    (is (= '() (ixn/intersection-pre [] [])))
    (is (= '() (ixn/intersection-pre nil [3 5 6])))
    (is (= '() (ixn/intersection-pre [3 5 6] nil)))
    (is (= '() (ixn/intersection-pre nil nil)))
    (is (= '() (ixn/intersection-pre [] nil)))
    (is (= '() (ixn/intersection-pre nil [])))
    (is (= '() (ixn/intersection-pre '() nil)))
    (is (= '() (ixn/intersection-pre nil '())))))

(defspec test-intersection-pre-check
  100
  (prop/for-all [s0 (gen/vector gen/int)
                 s1 (gen/vector gen/int)]
    (= (sort (set/intersection (into #{} s0) (into #{} s1)))
       (ixn/intersection-pre (sort (external/distinct s0)) (sort (external/distinct s1))))))

(deftest test-intersection
  (is (= '(1 2 3)
         (external/intersection
           [4 5 1 6 7 8 2 9 10 11 3 12 13 4]
           [20 30 40 50 1 60 70 80 90 2 100 110 120 130 3 140 150 160 170])))
  (is (= '(1 2 3)
         (external/intersection
           [4 4 5 5 1 6 6 7 7 8 8 2 2 9 9 10 10 11 11 3 3 12 12 13 13  4 4 14 14]
           [40 40 50 50 1 1 60 60 70 70 80 80 2 2 90 90 100 100 110 110 3 3 120 120 130 130]))
      "It ignores duplicates and only treats the seqs as distinct sets")
  (is (= '("c" "d")
         (external/intersection
           (external/set ["a" "b" "c" "d"])
           (external/set ["c" "d" "e" "f"])))
      "It works on strings")
  (is (= '(3)
         (external/intersection
           (external/set [1 2 3])
           (external/set [3 4 5 ])))
      "It works on ExternalSets"))

(defspec test-intersection-check
  100
  (prop/for-all [s0 (gen/vector gen/int)
                 s1 (gen/vector gen/int)]
    (= (sort (set/intersection (into #{} s0) (into #{} s1)))
       (external/intersection s0 s1))))
