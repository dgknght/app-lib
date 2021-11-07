(ns dgknght.app-lib.math-test
  (:require [clojure.test :refer [deftest is are]]
            [dgknght.app-lib.math :as m]))

(deftest evaluate-a-simple-number
  (are [in e] (= e (m/eval in))
       "1"       1M
       "1.05"    1.05M
       " 1.05"   1.05M
       "-1.05"  -1.05M
       ".05"     0.05M
       "0.05"    0.05M
       "-.05"   -0.05M
       "-0.05"  -0.05M))

(deftest evaluate-a-partial-statement
  (is (nil? (m/eval "1 +"))
        "An unbalanced expression returns nil"))

(deftest evaluate-a-simple-statement
  (are [in e] (= e (m/eval in))
       "1+1"        2M
       "1 + 1"      2M
       "1.0 + 1.1"  2.1M
       "2 - 1"      1M
       "2 * 5"      10M
       "2*5"        10M
       "10 / 5"     2M))

(deftest evaluate-a-multi-step-statement
  (are [in e] (= e (m/eval in))
       "1 + 2 + 3"      6M
       "1 + 2 * 3"      7M
       "1 + 6 / 2"      4M
       "1 + 2 * 3 + 4"  11M
       "3 * (2 + 1)"    9M))
