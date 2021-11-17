(ns dgknght.app-lib.dates-test
  (:require [clojure.test :refer [deftest is are]]
            [clj-time.core :as t]
            [dgknght.app-lib.dates :as dates]))

(deftest parse-a-date-range
  (is (= [(t/local-date 2015 3 2)
          (t/local-date 2015 3 2)]
         (dates/parse-range "2015-03-02"))
      "A single date is both the start and the end")
  (is (= [(t/local-date 2015 3 1)
          (t/local-date 2015 3 31)]
         (dates/parse-range "2015-03"))
      "A month-year is parsed as the first and last dates of the month")
  (is (= [(t/local-date 2015 1 1)
          (t/local-date 2015 12 31)]
         (dates/parse-range "2015"))
         "A year is parsed as the first and last dates of the year"))

(deftest parse-an-interval
  (is (= (t/interval (t/date-time 2015 3 1)
                     (t/date-time 2015 4 1))
         (dates/parse-interval "2015-03"))
      "A year-month yields an interval for that month")
  (is (= (t/interval (t/date-time 2015 1 1)
                     (t/date-time 2016 1 1))
         (dates/parse-interval "2015"))
      "A year yields an interval for that month"))

(deftest get-a-sequence-of-intervals
  (is (= [(t/interval (t/date-time 2015 1 1)
                      (t/date-time 2015 2 1))
          (t/interval (t/date-time 2015 2 1)
                      (t/date-time 2015 3 1))
          (t/interval (t/date-time 2015 3 1)
                      (t/date-time 2015 4 1))]
         (take 3 (dates/intervals (t/date-time 2015 1 1)
                                  (t/months 1))))
      "A sequence of intervals of the given size and within the given range is returned"))

(deftest get-a-sequence-of-date-ranges
  (is (= [[(t/local-date 2015 1 1)
           (t/local-date 2015 1 31)]
          [(t/local-date 2015 2 1)
           (t/local-date 2015 2 28)]
          [(t/local-date 2015 3 1)
           (t/local-date 2015 3 31)]]
         (take 3 (dates/ranges (t/local-date 2015 1 1)
                               (t/months 1))))
      "A sequence of date tuples of the given size and within the given range is returned"))

(deftest get-a-relative-date
  (t/do-at (t/date-time 2020 3 2 0 0 0)
           (are [input expected] (= expected (dates/relative input))
                "today"                   (t/local-date 2020 3 2)
                "yesterday"               (t/local-date 2020 3 1)
                "start-of-this-year"      (t/local-date 2020 1 1)
                "end-of-this-year"        (t/local-date 2020 12 31)
                "start-of-this-month"     (t/local-date 2020 3 1)
                "end-of-this-month"       (t/local-date 2020 3 31)
                "start-of-previous-year"  (t/local-date 2019 1 1)
                "end-of-previous-year"    (t/local-date 2019 12 31)
                "start-of-previous-month" (t/local-date 2020 2 1)
                "end-of-previous-month"   (t/local-date 2020 2 29))))

(deftest get-the-earliest-date
  (let [d1 (t/local-date 2021 1 1)
        d2 (t/local-date 2021 1 2)
        d3 (t/local-date 2021 2 1)]
    (is (= d1 (dates/earliest d2 d3 d1))
        "The earliest of many is returned")
    (is (= d2 (dates/earliest d2))
        "A single date is returned")
    (is (= d2 (dates/earliest d2 nil))
        "Nil values are ignored")
    (is (nil? (dates/earliest))
        "No values returns nil")))

(deftest get-the-latest-date
  (let [d1 (t/local-date 2021 1 1)
        d2 (t/local-date 2021 1 2)
        d3 (t/local-date 2021 2 1)]
    (is (= d3 (dates/latest d2 d3 d1))
        "The latest of many is returned")
    (is (= d2 (dates/latest d2))
        "A single date is returned")
    (is (= d2 (dates/latest nil d2))
        "nil is ignored")
    (is (nil? (dates/latest))
        "No dates yields nil")))

(deftest convert-nominal-comparatives-to-symbolic
  (let [date (t/local-date 2015 1 1)
        other-date (t/local-date 2015 1 31)]
    (are [criteria k expected] (= expected
                                  (dates/symbolic-comparatives criteria k))
         {:start-after date}
         :start
         {:start-on [:> date]}

         {:transaction-date-after date}
         :transaction-date
         {:transaction-date [:> date]}

         {:transaction-date-on-or-after date
          :transaction-date-on-or-before other-date}
         :transaction-date
         {:transaction-date [:between date other-date]}

         {:start-on-or-after date
          :start-on-or-before other-date}
         :start-on
         {:start-on [:between date other-date]})))

(deftest convert-symbolic-comparatives-to-nominal
  (let [date (t/local-date 2015 1 1)
        other-date (t/local-date 2015 1 31)]
    (are [criteria k expected] (= expected
                                  (dates/nominal-comparatives criteria k))
         {:start-on [:> date]}
         :start
         {:start-after date}

         {:transaction-date [:> date]}
         :transaction-date
         {:transaction-date-after date}

         {:transaction-date [:between date other-date]}
         :transaction-date
         {:transaction-date-on-or-after date
          :transaction-date-on-or-before other-date}

         {:start-on [:between date other-date]}
         :start-on
         {:start-on-or-after date
          :start-on-or-before other-date})))
