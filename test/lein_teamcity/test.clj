(ns lein-teamcity.test
    (:require [clojure.test :refer :all]
              [lein-teamcity.plugin :refer :all]))

(deftest escaping
  (is (= (tc-msg :msg :name "a'b")  "##teamcity[msg name='a|'b']"))
  (is (= (tc-msg :msg :name "a\nb") "##teamcity[msg name='a|nb']"))
  (is (= (tc-msg :msg :name "a\rb") "##teamcity[msg name='a|rb']"))
  (is (= (tc-msg :msg :name "a|b")  "##teamcity[msg name='a||b']"))
  (is (= (tc-msg :msg :name "a[b")  "##teamcity[msg name='a|[b']"))
  (is (= (tc-msg :msg :name "a]b")  "##teamcity[msg name='a|]b']")))
