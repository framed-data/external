(ns
  ^{:slow-tests true}
  framed.external.core-test
  (:require [clojure.test :refer :all]
            [framed.external.core :as external]
            [clojure.test.check :as tc]
            [clojure.set :as set]
            (clojure.test.check
              [clojure-test :refer [defspec]]
              [generators :as gen]
              [properties :as prop])))

; Takes a bunch of key value pairs returned by aggregate-by and returns
; a randomized list that would evaluate to that using :user-id as the key
; and count as the mapper function
(defn reverse-aggregate [key-value-pairs]
  (->> key-value-pairs
       (mapcat (fn [[k cnt]] (repeatedly cnt #(hash-map :user-id k :value (rand)))))
       shuffle))

(deftest test-keyed-partition-by
  (is (= [[false [1 1 1 1 1 1 3 3 3]] [true [2 2 2]]]
         (external/keyed-partition-by even? [1 1 1 1 1 1 3 3 3 2 2 2])))
  (is (= [[false [1 1 3 3]] [true [2 2]] [false [5 5]]] ; Doesn't sort for you!
         (external/keyed-partition-by even? [1 1 3 3 2 2 5 5])))
  (is (= [] (external/keyed-partition-by even? [])))
  (is (= [[25 [{:id 25 :v 1} {:id 25 :v 2}]] [26 [{:id 26 :v 3}]]]
         (external/keyed-partition-by :id [{:id 25 :v 1} {:id 25 :v 2} {:id 26 :v 3}]))))

(deftest test-aggregate-by
  (let [probability-key (fn [p] (if (> p 0.7) :high :low))
        input [0.2 0.4 0.6 0.8 0.9 0.1]
        expected [[:high 2] [:low 4]]]
    (is (= expected
           (external/aggregate-by probability-key count input)))))

(deftest test-reverse-aggregate
  (is (=
       (count (reverse-aggregate [[1 1000] [2 9001] [3 50001] [4 123456] [5 999999] [6 5]]))
       (+ 1000 9001 50001 123456 999999 5))))

(deftest test-user-id-and-count
  (let [result-1 [[1 3] [2 2] [3 1]]
        result-2 [[1 10] [2 15] [3 50] [4 1] [5 50]]
        result-3 [[1 5] [15 50] [101 5] [9001 9]]
        result-4 [[1 2000] [2 15000] [3 21000] [4 35000]]]
    (is (= (external/aggregate-by :user-id count 100 (reverse-aggregate result-1)) result-1))
    (is (= (external/aggregate-by :user-id count 1000 (reverse-aggregate result-2)) result-2))
    (is (= (external/aggregate-by :user-id count 100 (reverse-aggregate result-3)) result-3))
    (is (= (external/aggregate-by :user-id count 10000 (reverse-aggregate result-4)) result-4))))

(deftest test-distinct
  (is (= [0 1 2 3 4]
         (->> (repeatedly #(rand-int 5)) (take 10000) external/distinct sort))))

(defspec test-distinct-check
  100
  (prop/for-all [s0 (gen/vector gen/int)]
    (= (sort (clojure.core/distinct s0))
       (sort (external/distinct s0)))))

(defspec test-generative-int-shuffle
  100
  (prop/for-all [v (gen/vector gen/int)]
    (is (= (sort v)
           (sort (external/shuffle v))))))

(deftest test-shuffle-deterministic
  (let [rng (java.util.Random. 1)]
    (is (= [0 4 2 3 1] (external/shuffle rng (range 5))))))
