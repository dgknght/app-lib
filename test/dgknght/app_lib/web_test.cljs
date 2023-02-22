(ns dgknght.app-lib.web-test
  (:require [cljs.test :refer [deftest is are]]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [dgknght.app-lib.time :as tm]
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
  (is (.equals (t/local-date 2019 3 2)
               (web/unserialize-date "2019-03-02"))
      "A well-formatted string can be converted into a date")
  (is (.equals (t/local-date 2020 3 2)
               (t/do-at*
                 (t/date-time 2020 3 1 12 30)
                 (fn []
                   (web/unserialize-date "tomorrow"))))
      "A relative date can be specified")
  (is (= (t/local-date 2019 3 2)
         (web/unserialize-date (t/local-date 2019 3 2)))
      "A date is returned as-is"))

(deftest serialize-a-date-time
  (is (= "2019-03-02T12:34:56Z"
         (web/serialize-date-time (t/date-time 2019 3 2 12 34 56)))))

(deftest unserialize-a-date-time
  (let [expected (t/date-time 2019 3 2 12 34 56)
        actual (web/unserialize-date-time "2019-03-02T12:34:56Z")]
  (is (.equals expected actual))))

(deftest format-a-date
  (let [date (t/local-date 2019 3 2)]
  (are [input expected] (= expected (web/format-date input))
       date "3/2/2019"
       nil nil)
  (are [input fmt expected] (= expected (web/format-date input fmt))
       date :year-month-day "2019-03-02"
       date "M/d" "3/2"
       date (tf/formatters :year-month) "2019-03")))

(deftest unformat-a-date
  (is (.equals (t/local-date 2020 3 2)
               (web/unformat-date "3/2/2020")))
  (is (nil? (web/unformat-date "notadate"))))

(deftest format-a-date-time
  (is (= "3/2/2019 12:34 pm" (web/format-date-time (t/date-time 2019 3 2 12 34 56))))
  (is (= nil (web/format-date-time nil))))

(deftest reformat-a-date-time
  (is (= "3/2/2020 1:00 am"
         (web/reformat-date-time "03/02/2020 01:00 am"))))

(deftest format-a-decimal
  (is (= "1,234.50" (web/format-decimal 1234.5M))
      "The default format uses a comma, no currency symbol, and 2 decimal places"))

(deftest format-a-percent
  (is (= "25.4%" (web/format-percent 0.254M))))

(deftest format-a-currency
  (is (= "$12.34" (web/format-currency 12.34M))))

(deftest serialize-a-time
  (is (= "12:30:00"
         (web/serialize-time (tm/time 12 30)))))

(deftest unserialize-a-time
  (is (= (tm/time 12 30 15)
         (web/unserialize-time "12:30:15"))))

(deftest format-a-time
  (is (= "12:30"
         (web/format-time (tm/time 12 30 15)))))

(deftest unformat-a-time
  (is (= (tm/time 12 30)
         (web/unformat-time "12:30"))))
