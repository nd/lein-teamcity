(ns lein-teamcity.plugin
  (:require [robert.hooke]
            [leiningen.test]
            [leiningen.jar]
            [leiningen.uberjar]
            [clojure.test]
            [leiningen.core.main]
            [leiningen.core.project :as project]
            [clojure.string :as str]))

(defn escape
  [s]
  (str/replace s #"['|\n\r\[\]]"
               (fn [x]
                 (cond (= x "\n") "|n"
                       (= x "\r") "|r"
                       :else (str "|" x)))))

(defn tc-msg-attrs
  [attrs]
  (if (seq (rest attrs))
    (->> attrs
         (partition 2)
         (map (fn [[n v]] (str (name n) "='" (escape v) "'")))
         (str/join " "))
    (str "'" (first attrs) "'")))

(defn tc-msg
  [message & attrs]
  (str "##teamcity[" (name message) " " (tc-msg-attrs attrs) "]"))

(defn add-teamcity-jar-artifact-reporting [f & [_ out-file :as args]]
  (apply f args)
  (println (tc-msg :publishArtifacts out-file)))

(defn add-teamcity-uberjar-artifact-reporting [f & args]
  (let [artifact (apply f args)]
    (println (tc-msg  :publishArtifacts artifact))))

(defn add-teamcity-task-reporting [f & [name :as args]]
  (println (tc-msg :blockOpened :name name))
  (apply f args)
  (println (tc-msg :blockClosed :name name)))

(defn add-teamcity-test-reporting [f & args]
  (if (boolean (resolve 'leiningen.test/*monkeypatch?*))
    `(binding [~'clojure.test/report
               (fn [m#]
                 (let [test-name#
                       (fn []
                         (let [names# (reverse (map #(:name (meta %)) clojure.test/*testing-vars*))]
                           (if (= (count names#) 1)
                             (str (first names#))
                             (str names#))))
                       fail-msg#
                       (fn [event#]
                         (str (if (:message event#) (str :message " " (:message event#) "\n") "")
                              :expected " " (:expected event#) "\n"
                              :actual " " (:actual event#)))
                       escape#
                       (fn [s#]
                         (-> s#
                             (.replaceAll "\\|" "||")
                             (.replaceAll "'"   "|'")
                             (.replaceAll "\n"  "|n")
                             (.replaceAll "\r"  "|r")
                             (.replaceAll "\\[" "|[")
                             (.replaceAll "\\]" "|]")))]

                   (when (= (:type m#) :begin-test-ns)
                     (println (str "##teamcity[testSuiteStarted name='" (ns-name (:ns m#)) "']"))
                     ((.getRawRoot #'clojure.test/report) m#))

                   (when (= (:type m#) :end-test-ns)
                     ((.getRawRoot #'clojure.test/report) m#)
                     (println (str "##teamcity[testSuiteFinished name='" (ns-name (:ns m#)) "']")))

                   (when (= (:type m#) :begin-test-var)
                     (println (str "##teamcity[testStarted name='" (test-name#)  "']"))
                     ((.getRawRoot #'clojure.test/report) m#))

                   (when (= (:type m#) :end-test-var)
                     ((.getRawRoot #'clojure.test/report) m#)
                     (println (str "##teamcity[testFinished name='" (test-name#) "']")))

                   (when (= (:type m#) :fail)
                     ((.getRawRoot #'clojure.test/report) m#)
                     (println (str "##teamcity[testFailed name='" (test-name#) "' message='" (escape# (fail-msg# m#)) "']")))))]
       ~(apply f args))
    (apply f args)))

(defn hooks []
  (do
    (robert.hooke/add-hook #'leiningen.test/form-for-testing-namespaces
                           add-teamcity-test-reporting)
    (robert.hooke/add-hook #'leiningen.jar/write-jar
                           add-teamcity-jar-artifact-reporting)
    (robert.hooke/add-hook #'leiningen.uberjar/uberjar
                           add-teamcity-uberjar-artifact-reporting)
    (robert.hooke/add-hook #'leiningen.core.main/apply-task
                           add-teamcity-task-reporting)))
