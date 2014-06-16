(ns lein-teamcity.test
    (:require [clojure.test :refer :all]
              [clojure.java.shell :refer [sh]]
              [lein-teamcity.plugin :refer :all])
    (:import [java.io File]))

(deftest escaping
  (is (= (tc-msg :msg :name "a'b")  "##teamcity[msg name='a|'b']"))
  (is (= (tc-msg :msg :name "a\nb") "##teamcity[msg name='a|nb']"))
  (is (= (tc-msg :msg :name "a\rb") "##teamcity[msg name='a|rb']"))
  (is (= (tc-msg :msg :name "a|b")  "##teamcity[msg name='a||b']"))
  (is (= (tc-msg :msg :name "a[b")  "##teamcity[msg name='a|[b']"))
  (is (= (tc-msg :msg :name "a]b")  "##teamcity[msg name='a|]b']")))

(defn teamcity-test-messages
  [s]
  (re-seq #"##teamcity\[test.*" s))

(defn lein-test
  [test-project-root]
  (spit (str test-project-root "/.lein-classpath") (.getCanonicalPath (File. "src")))
  (sh (System/getProperty "leiningen.script") "test" :dir test-project-root))

(deftest report-all-passing
  (let [{:keys [exit out]} (lein-test "./test-projects/all-pass")]
    (is (= 0 exit))
    (is (= (teamcity-test-messages out)
           ["##teamcity[testSuiteStarted name='all-pass.core-test']"
            "##teamcity[testStarted name='a-test' captureStandardOutput='true']"
            "##teamcity[testFinished name='a-test']"
            "##teamcity[testSuiteFinished name='all-pass.core-test']"]))))

(deftest report-failure
  (let [{:keys [exit out]} (lein-test "./test-projects/one-failure")]
    (is (= 1 exit))
    (is (= (teamcity-test-messages out)
           ["##teamcity[testSuiteStarted name='one-failure.core-test']"
            "##teamcity[testStarted name='a-failing-test' captureStandardOutput='true']"
            "##teamcity[testFailed name='a-failing-test' message=':expected (= 0 1)|n:actual (not (= 0 1))']"
            "##teamcity[testFinished name='a-failing-test']"
            "##teamcity[testSuiteFinished name='one-failure.core-test']"]))))

(deftest report-error
  (let [{:keys [exit out]} (lein-test "./test-projects/one-error")]
    (is (= 1 exit))
    (is (= (teamcity-test-messages out)
           ["##teamcity[testSuiteStarted name='one-error.core-test']"
            "##teamcity[testStarted name='an-erroring-test' captureStandardOutput='true']"
            "##teamcity[testFailed name='an-erroring-test' message=':message Uncaught exception, not in assertion.|n:expected |n:actual java.lang.Exception: ERROR']"
            "##teamcity[testFinished name='an-erroring-test']"
            "##teamcity[testSuiteFinished name='one-error.core-test']"]))))

(deftest do-not-report-syntax-error
  (let [{:keys [exit out]} (lein-test "./test-projects/syntax-error")]
    (is (= 1 exit))
    (is (= (teamcity-test-messages out) nil))))


(deftest do-not-report-syntax-error-in-test
  (let [{:keys [exit out]} (lein-test "./test-projects/syntax-error-test")]
    (is (= 1 exit))
    (is (= (teamcity-test-messages out) nil))))
