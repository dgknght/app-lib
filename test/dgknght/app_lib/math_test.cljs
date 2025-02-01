(ns dgknght.app-lib.math-test
  (:require [cljs.test :refer-macros [deftest is are]]
            [dgknght.app-lib.decimal]
            [dgknght.app-lib.math :as m])
  (:import goog.i18n.NumberFormat))

(defn- format-number
  [n]
  (.format (doto (NumberFormat. (.-DECIMAL (.-Format NumberFormat)))
             (.setMaximumFractionDigits 2)
             (.setMinimumFractionDigits 2))
           n))

(deftest evaluate-a-simple-number
  (are [in e] (= (format-number e)
                 (format-number (m/eval in)))
       "1"       "1.00"
       "1.05"    "1.05"
       " 1.05"   "1.05"
       "-1.05"  "-1.05"
       ".05"     "0.05"
       "0.05"    "0.05"
       "-.05"   "-0.05"
       "-0.05"  "-0.05"))

(deftest evaluate-a-partial-statement
  (is (nil? (m/eval "1 +"))
        "An unbalanced expression returns nil"))

(deftest evaluate-a-simple-statement
  (are [in e] (= (format-number e)
                 (format-number (m/eval in)))
       "1+1"        "2.00"
       "1 + 1"      "2.00"
       "1.0 + 1.1"  "2.10"
       "2 - 1"      "1.00"
       "2 * 5"      "10.00"
       "2*5"        "10.00"
       "10 / 5"     "2.00"))

(deftest evaluate-a-multi-step-statement
  (are [in e] (= (format-number e)
                 (format-number (m/eval in)))
       "1 + 2 + 3"      "6.00"
       "1 + 2 * 3"      "7.00"
       "1 + 6 / 2"      "4.00"
       "1 + 2 * 3 + 4"  "11.00"
       "3 * (2 + 1)"    "9.00"))

(deftest handle-unprocessable-expressions
  (is (nil? (m/eval "-"))))
