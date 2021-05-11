(ns dgknght.app-lib.decimal-test
  (:require [cljs.test :refer [deftest is are]]
            ["decimal.js" :as Decimal]
            [dgknght.app-lib.decimal :as d]))

(deftest parse-a-decimal
  (is (instance? Decimal (d/->decimal "1.5"))
      "It returns an instance of Decimal")
  (is (= (d/->decimal "1.5") 1.5)
      "It returns the correct value"))

(deftest get-an-absolute-value
  (are [in e] (= (d/abs (d/->decimal in))
                           e)
       "-10" 10
       "10"  10))

(deftest get-zero
  (is (= (d/zero) 0)))

(deftest check-for-zero?
  (are [in e] (= e (d/zero? in))
       (d/zero)          true 
       (d/->decimal "1") false))

(deftest add-numbers
  (are [i1 i2 e] (= (d/+ i1 i2) e)
       (d/->decimal "1") 1                 2
       (d/->decimal "1") (d/->decimal 1)   2
       (d/->decimal "1") nil               1
       nil               (d/->decimal "1") 1
       nil               nil               nil))

(deftest subtract-numbers
  (are [i1 i2 e] (= (d/- i1 i2) e)
       (d/->decimal "4") 3                 1
       4                 (d/->decimal "1") 3
       (d/->decimal "4") nil               nil
       nil               (d/->decimal "4") nil
       nil               nil               nil))

(deftest multiply-numbers
  (are [i1 i2 e] (= (d/* i1 i2) e)
       (d/->decimal 2) 3               6
       2               (d/->decimal 3) 6
       (d/->decimal 2) nil            nil
       nil            (d/->decimal 3) nil
       nil            nil             nil))

(deftest divide-numbers
  (are [i1 i2 e] (= (d// i1 i2) e)
       (d/->decimal 8) 2               4
       8               (d/->decimal 2) 4
       (d/->decimal 8) nil             nil
       nil             (d/->decimal 2) nil
       nil             nil             nil))

(deftest sum-multiple-numbers
  (are [inputs e] (= (d/sum inputs) e)
       [1 2]        3
       [1 "2" nil]  3))

(deftest truncate-a-number
  (is (= (d/trunc (d/->decimal "1.234")) 1.0)))

(deftest round-a-number
  (are [i e] (= (d/round i) e)
       (d/->decimal 1.4) 1.0
       (d/->decimal 1.5) 2.0
       (d/->decimal 1.6) 2.0))

(deftest compare-values
  (is (= (d/->decimal 1.0) (d/->decimal 1.0)))
  (is (not (= (d/->decimal 1.0) "not a number"))))

(deftest identify-a-decimal
  (are [in e] (= e (d/decimal? in))
       (d/->decimal "1")  true
       1                  false
       1.0                false
       "1"                false))
