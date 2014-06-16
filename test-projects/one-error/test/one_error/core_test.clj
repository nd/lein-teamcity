(ns one-error.core-test
  (:require [clojure.test :refer :all]
            [one-error.core :refer :all]))

(deftest an-erroring-test
  (testing "I throw exceptions"
    (throw (Exception. "ERROR"))))
