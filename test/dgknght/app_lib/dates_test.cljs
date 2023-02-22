(ns dgknght.app-lib.dates-test
  (:require-macros [cljs-time.macros :refer [do-at]])
  (:require [cljs.test :refer [deftest is are]]
            [cljs-time.core :as t]
            [dgknght.app-lib.web :refer [serialize-date]]
            [dgknght.app-lib.dates :as dates]))

(deftest parse-a-date-range
  (are [input expected] (= expected (map serialize-date (dates/parse-range input)))
       "2015-03-02" ["2015-03-02"
                     "2015-03-02"]
       "2015-03"    ["2015-03-01"
                     "2015-03-31"]
       "2015"       ["2015-01-01"
                     "2015-12-31"]))

(defn- serialize-interval
  [interval]
  (reduce #(assoc %1 %2 (serialize-date (%2 interval)))
          {}
          [:start :end]))

(deftest parse-an-interval
  (are [input expected] (= expected
                           (serialize-interval
                             (dates/parse-interval input)))
       "2015-03-02" {:start "2015-03-02"
                     :end   "2015-03-03"}
       "2015-03"    {:start "2015-03-01"
                     :end   "2015-04-01"}
       "2015"       {:start "2015-01-01"
                     :end   "2016-01-01"}))

(deftest get-a-sequence-of-intervals
  (is (= [{:start "2015-01-01"
           :end   "2015-02-01"}
          {:start "2015-02-01"
           :end   "2015-03-01"}
          {:start "2015-03-01"
           :end   "2015-04-01"}]
         (->> (dates/intervals (t/date-time 2015 1 1)
                               (t/months 1))
              (map serialize-interval)
              (take 3)))
      "A sequence of intervals of the given size and within the given range is returned"))

(deftest get-a-sequence-of-date-ranges
  (is (= [["2015-01-01"
           "2015-01-31"]
          ["2015-02-01"
           "2015-02-28"]
          ["2015-03-01"
           "2015-03-31"]]
         (->> (dates/ranges (t/local-date 2015 1 1)
                            (t/months 1))
              (map #(mapv serialize-date %))
              (take 3)))
      "A sequence of date tuples of the given size and within the given range is returned"))

(deftest get-a-relative-date
  (do-at (t/local-date-time 2020 3 2)
         (are [input expected] (= expected (serialize-date (dates/relative input)))
              "today"                   "2020-03-02"
              "yesterday"               "2020-03-01"
              "start-of-this-year"      "2020-01-01"
              "end-of-this-year"        "2020-12-31"
              "start-of-this-month"     "2020-03-01"
              "end-of-this-month"       "2020-03-31"
              "start-of-previous-year"  "2019-01-01"
              "end-of-previous-year"    "2019-12-31"
              "start-of-previous-month" "2020-02-01"
              "end-of-previous-month"   "2020-02-29")))

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

(deftest create-a-period
  (are [input-type input-count expected] (= expected
                                            (dates/period input-type
                                                          input-count))
       :year  1 (t/years 1)
       :month 2 (t/months 2)
       :week  3 (t/weeks 3)))

(deftest generate-nominal-keys
  (is (= #{:created-before
           :created-at-or-before
           :created-after
           :created-at-or-after
           :created-at}
         (set (dates/nominal-keys :created-at)))))
