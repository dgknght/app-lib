(ns dgknght.app-lib.test-test
  (:require [clojure.test :refer [deftest is]]
            [dgknght.app-lib.test :as t]))

(deftest get-a-set-from-an-attribute
  (is (= #{"One" "Two" "Three"}
         (t/attr-set :number
                     [{:number "One"}
                      {:number "Two"}
                      {:number "Three"}]))))
