(ns dgknght.app-lib.time-test
  (:require [cljs.test :refer-macros [deftest is]]
            [dgknght.app-lib.time :as tm]))


(deftest format-a-time-instance
  (is (= "12:34 PM" (tm/format-time (tm/time 12 34 56))))
  (is (= "1:45 PM" (tm/format-time (tm/time  13 45)))))

(deftest compare-time-instances
  (is (> 0 (tm/compare (tm/time 12) (tm/time 13))))
  (is (< 0 (tm/compare (tm/time 12 30) (tm/time 12 29)))))
