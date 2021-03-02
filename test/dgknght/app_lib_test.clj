(ns dgknght.app-lib-test
  (:require [clojure.test :refer [deftest is]]
            [dgknght.app-lib :as lib]))

(deftest parse-an-integer
  (is (= 1 (lib/parse-int "1")))
  (is (nil? (lib/parse-int nil)))
  (is (thrown? NumberFormatException
               (lib/parse-int "notanumber"))))
