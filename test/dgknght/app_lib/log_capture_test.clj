(ns dgknght.app-lib.log-capture-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.log-capture :refer [with-log-capture]]))

(deftest capture-a-log-entry
  (with-log-capture [logs]
    (log/debug "This is a test")
    (log/log* :x :info nil "WTF?")
    (testing "exact body match"
      (is (logged? "This is a test" logs)))))
