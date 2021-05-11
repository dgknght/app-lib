(ns dgknght.app-lib.web-test
  (:require [clojure.test :refer [deftest is]]
            [clj-time.core :as t]
            [dgknght.app-lib.web :as web]))

(deftest build-a-path
  (is (= "/api/organizations/123/invitations"
         (web/path :api
                      :organizations
                      123
                      :invitations))
      "The path is assembled correctly."))

(deftest serialize-a-local-date
  (is (= "2019-03-02"
         (web/serialize-date (t/local-date 2019 3 2)))))

(deftest unserialize-a-local-date
  (is (= (t/local-date 2019 3 2)
         (web/unserialize-date "2019-03-02"))))

(deftest serialize-a-date-time
  (is (= "2019-03-02T12:34:56Z"
         (web/serialize-date-time (t/date-time 2019 3 2 12 34 56)))))

(deftest unserialize-a-date-time
  (is (= (t/date-time 2019 3 2 12 34 56)
         (web/unserialize-date-time "2019-03-02T12:34:56Z"))))

(deftest format-a-date
  (is (= "3/2/2019" (web/format-date (t/local-date 2019 3 2))))
  (is (= nil (web/format-date nil))))

(deftest format-a-date-time
  (is (= "3/2/2019 12:34 PM" (web/format-date-time (t/date-time 2019 3 2 12 34 56))))
  (is (= nil (web/format-date-time nil))))

(deftest format-a-decimal
  (is (= "1,234.50" (web/format-decimal 1234.5M))
      "The default format uses a comma, no currency symbol, and 2 decimal places"))

(deftest format-a-percent
  (is (= "25.4%" (web/format-percent 0.254M))))
