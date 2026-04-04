(ns dgknght.app-lib.math-test
  (:require #?(:clj  [clojure.test :refer [deftest is are]]
               :cljs [cljs.test :refer-macros [deftest is are]])
            #?(:cljs [dgknght.app-lib.decimal])
            [dgknght.app-lib.math :as m])
  #?(:cljs (:import goog.i18n.NumberFormat)))

#?(:cljs
   (defn- format-number
     [n]
     (.format (doto (NumberFormat. (.-DECIMAL (.-Format NumberFormat)))
                (.setMaximumFractionDigits 2)
                (.setMinimumFractionDigits 2))
              n)))

(deftest evaluate-a-simple-number
  #?(:clj
     (are [in e] (= e (m/eval in))
          "1"       1M
          "1.05"    1.05M
          " 1.05"   1.05M
          "-1.05"  -1.05M
          ".05"     0.05M
          "0.05"    0.05M
          "-.05"   -0.05M
          "-0.05"  -0.05M)
     :cljs
     (are [in e] (= (format-number e)
                    (format-number (m/eval in)))
          "1"       "1.00"
          "1.05"    "1.05"
          " 1.05"   "1.05"
          "-1.05"  "-1.05"
          ".05"     "0.05"
          "0.05"    "0.05"
          "-.05"   "-0.05"
          "-0.05"  "-0.05")))

(deftest evaluate-a-partial-statement
  (is (nil? (m/eval "1 +"))
        "An unbalanced expression returns nil"))

(deftest evaluate-a-simple-statement
  #?(:clj
     (are [in e] (= e (m/eval in))
          "1+1"        2M
          "1 + 1"      2M
          "1.0 + 1.1"  2.1M
          "2 - 1"      1M
          "2 * 5"      10M
          "2*5"        10M
          "10 / 5"     2M)
     :cljs
     (are [in e] (= (format-number e)
                    (format-number (m/eval in)))
          "1+1"        "2.00"
          "1 + 1"      "2.00"
          "1.0 + 1.1"  "2.10"
          "2 - 1"      "1.00"
          "2 * 5"      "10.00"
          "2*5"        "10.00"
          "10 / 5"     "2.00")))

(deftest evaluate-a-multi-step-statement
  #?(:clj
     (are [in e] (= e (m/eval in))
          "1 + 2 + 3"      6M
          "1 + 2 * 3"      7M
          "1 + 6 / 2"      4M
          "1 + 2 * 3 + 4"  11M
          "3 * (2 + 1)"    9M)
     :cljs
     (are [in e] (= (format-number e)
                    (format-number (m/eval in)))
          "1 + 2 + 3"      "6.00"
          "1 + 2 * 3"      "7.00"
          "1 + 6 / 2"      "4.00"
          "1 + 2 * 3 + 4"  "11.00"
          "3 * (2 + 1)"    "9.00")))

(deftest handle-unprocessable-expressions
  (is (nil? (m/eval "-"))))
