(ns
  ^{:slow-tests true}
  framed.external.set-test
  (:require [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.set :as set]
            (clojure.test.check
              [clojure-test :refer [defspec]]
              [generators :as gen]
              [properties :as prop])
            (framed.external
              [core :as external]
              [set :as external.set])))

(defspec test-external-set-seq
  100
  (prop/for-all [s0 (gen/vector gen/int)]
    (= (sort (seq (external/set s0)))
       (sort (external/distinct s0)))))

(defspec test-external-set-get
  100
  (prop/for-all [v0 (gen/vector gen/int)]
    (let [s0 (external/set v0)
          s1 (into #{} v0)]
      (prop/for-all [s gen/int]
        (= (get s0 s)
           (get s1 s))))))

(defspec test-external-set-union
  15
  (prop/for-all [s0 (gen/vector gen/int)
                 s1 (gen/vector gen/int)]
    (= (-> (concat s0 s1) distinct sort)
       (sort (seq (external.set/union (external/set s0)
                                      (external/set s1)))))))

(deftest test-difference
  (let [s1 (external.set/coll->ExternalSet [1 2 3 4 5])
        s2 (external.set/coll->ExternalSet [2 3])
        s3 (external.set/coll->ExternalSet [3 4 5])]
    (is (= [1 4 5] (sort (seq (external.set/difference s1 s2)))))
    (is (= [1] (sort (seq (external.set/difference s1 s2 s3)))))))
