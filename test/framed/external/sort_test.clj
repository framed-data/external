(ns
  ^{:slow-tests true}
  framed.external.sort-test
  (:require [clojure.test :refer :all]
            [framed.external.sort :as external.sort]
            [clojure.test.check :as tc]
            [clojure.set :as set]
            (clojure.test.check
              [clojure-test :refer [defspec]]
              [generators :as gen]
              [properties :as prop]))
  (:refer-clojure :exclude [distinct]))

(defn tuple-comparator [x y]
  ; Since we're using TreeSet, the tuples are being
  ; cast to ArrayLists behind the scenes, so the comparator
  ; needs to use .get instead of get
  (compare (.get x 1) (.get y 1)))

(defn fake-event-log []
  (lazy-seq
    (cons {:user-id (rand-int 10) :value (rand)} (fake-event-log))))

(defn map-comparator [x y]
  (compare (:user-id x) (:user-id y)))

(deftest test-basic-sorts
  (is (= (external.sort/sort 1 compare [])        []))
  (is (= (external.sort/sort 1 compare [1])       [1]))
  (is (= (external.sort/sort 5 compare [1])       [1]))
  (is (= (external.sort/sort 2 compare [2 1])     [1 2]))
  (is (= (external.sort/sort 2 compare [2 3 1])   [1 2 3]))
  (is (= (external.sort/sort 4 compare [2 3 1])   [1 2 3]))
  (is (= (external.sort/sort 2 compare [1 5 7 2]) [1 2 5 7])))

(deftest test-dups
  (is (= (external.sort/sort 1 compare [1 2 3 1 2 3]) [1 1 2 2 3 3]))
  (is (= (external.sort/sort 1 compare [1 1 1 1 1 1]) [1 1 1 1 1 1]))
  (is (= (external.sort/sort 2 compare [1 2 3 1 2 3]) [1 1 2 2 3 3]))
  (is (= (external.sort/sort 2 compare [1 1 1 1 1 1]) [1 1 1 1 1 1]))
  (is (= (external.sort/sort 3 compare [1 2 3 1 2 3]) [1 1 2 2 3 3]))
  (is (= (external.sort/sort 3 compare [1 1 1 1 1 1]) [1 1 1 1 1 1])))

(deftest test-large
  (is (= (external.sort/sort 10 compare (shuffle (range 500))) (range 500)))
  (is (= (external.sort/sort 1000 compare (shuffle (range 50000))) (range 50000)))
  (is (= (external.sort/sort 100000 compare (range 500000 -1 -1)) (range 500001))))

(deftest test-tuples
  (is (= (external.sort/sort 2 tuple-comparator [[1 "Hello"] [2 "World"] [3 "Test"]])
         [[1 "Hello"] [3 "Test"] [2 "World"]])))

(deftest test-event-logs
  (let [small-log (take 10 (fake-event-log))
        large-log (take 1000000 (fake-event-log))]
    (is (= (external.sort/sort 2 map-comparator small-log) (sort map-comparator small-log)))
    (is (= (external.sort/sort 10000 map-comparator large-log) (sort map-comparator large-log)))))

(defspec test-generative-int
  15
  (prop/for-all [vs (gen/vector gen/int)
                 batchsize (gen/choose 5000 10000)]
    (is (= (sort vs)
           (external.sort/sort batchsize compare vs)))))

(deftest test-sort-by
  (let [tups [[2 "bar"] [1 "foo"] [3 "baz"]]]
    (is (= [[1 "foo"] [2 "bar"] [3 "baz"]]
           (external.sort/sort-by 1000 first tups)))))
