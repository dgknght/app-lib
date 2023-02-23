(ns dgknght.app-lib.calendar-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-time.core :as t]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.calendar :as cal]))

(deftest create-a-calendar-map
  (testing "defaults"
    (is (comparable? {:year 2020
                      :month 3}
                     (t/do-at
                       (t/date-time 2020 3 2 12 30)
                       (cal/init))))
    (is (comparable? {:year 2020
                      :month 3}
                     (t/do-at
                       (t/date-time 2020 3 2 12 30)
                       (cal/init {})))))
  (testing "just the selected date"
    (is (comparable? {:year 2020
                      :month 3}
                     (cal/init {:selected (t/local-date 2020 3 2)}))
        "The month and year is based on the selected date"))
  (testing "a month that starts on Sunday"
    (let [c (cal/init {:year 2020
                       :month 11
                       :first-day-of-week :sunday})]
      (is c "The return value is not nil")
      (is (= (t/local-date 2020 11 1)
             (:date (ffirst (:weeks c))))
          "The first day is the first of the month")))
  (testing "a month that starts on Tuesday"
    (let [c (cal/init {:year 2020
                       :month 9
                       :first-day-of-week :sunday})]
      (is c "The return value is not nil")
      (is (= (t/local-date 2020 8 30)
             (:date (ffirst (:weeks c))))
          "The first day is the penultimate day of the previous month")))
  (testing "a month that starts on Saturday"
    (let [c (cal/init {:year 2020
                       :month 8
                       :first-day-of-week :sunday})]
      (is c "The return value is not nil")
      (is (= (t/local-date 2020 7 26)
             (:date (ffirst (:weeks c))))
          "The first day is 6 days from the end of the previous month"))))

(deftest select-a-date
  (is (comparable? {:selected (t/local-date 2020 3 2)}
                   (cal/select (cal/init {:year 2020
                                          :month 3})
                               (t/local-date 2020 3 2)))))

(deftest advance-the-month
  (is (comparable? {:month 4}
                   (cal/next-month (cal/init {:year 2020
                                              :month 3}))))
  (is (comparable? {:month 1
                    :year 2021}
                   (cal/next-month (cal/init {:year 2020
                                              :month 12})))
      "The end of the year is handled correctly"))

(deftest regress-the-month
  (is (comparable? {:month 2}
                   (cal/prev-month (cal/init {:year 2020
                                              :month 3 }))))
  (is (comparable? {:month 12
                    :year 2019}
                   (cal/prev-month (cal/init {:year 2020
                                              :month 1 })))
      "The begining of the year is handled correctly"))
