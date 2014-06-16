(ns one-failure.core-test
  (:require [clojure.test :refer :all]
            [one-failure.core :refer :all]))

(deftest a-failing-test
  (testing "I fail."
    (is (= 0 1))))
