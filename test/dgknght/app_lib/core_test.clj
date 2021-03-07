(ns dgknght.app-lib.core-test
  (:require [clojure.test :refer [deftest is]]
            [dgknght.app-lib.core :as lib])
  (:import java.util.UUID))

(deftest parse-a-boolean
  (is (lib/parse-bool "true"))
  (is (lib/parse-bool "True"))
  (is (lib/parse-bool "TRUE"))
  (is (lib/parse-bool "1"))
  (is (not (lib/parse-bool "false")))
  (is (not (lib/parse-bool "False")))
  (is (not (lib/parse-bool "FALSE")))
  (is (not (lib/parse-bool "0"))))

(deftest parse-an-integer
  (is (= 1 (lib/parse-int "1")))
  (is (nil? (lib/parse-int nil)))
  (is (nil? (lib/parse-int ""))
      "An empty string yeilds nil")
  (is (= 1000 (lib/parse-int "1,000"))
      "A formatted interger is parsed correctly")
  (is (= 123 (lib/parse-int 123))
      "An integer is returned as-is")
  (is (thrown? NumberFormatException
               (lib/parse-int "notanumber"))))

(deftest parse-a-floating-point-number
  (is (= 1.0 (lib/parse-float "1")))
  (is (= 1.23 (lib/parse-float "1.23")))
  (is (nil? (lib/parse-float nil))
      "Nil yield nil")
  (is (nil? (lib/parse-float ""))
      "An empty string returns nil")
  (is (= 1.23 (lib/parse-float 1.23))
      "A float is returned as-is"))

(deftest conditionally-update-in
  (is (= {:value 2}
         (lib/update-in-if {:value 1}
                           [:value]
                           inc))
      "The fn is applied if the value is present")
  (is (= {}
         (lib/update-in-if {}
                           [:value]
                           inc))
      "The map is not modified if the key is not present"))

(deftest conditionally-assoc-in
  (is (= {:value 1}
         (lib/assoc-if {} :value 1)))
  (is (= {}
         (lib/assoc-if {} :value nil))))

(deftest test-for-deeply-contained-key
  (is (lib/deep-contains? {:one 1} :one))
  (is (lib/deep-contains? [:and {:one 1}] :one)))

(deftest find-deeply-contained-value
  (is (= 1 (lib/deep-get {:one 1} :one)))
  (is (= 1 (lib/deep-get [:and {:one 1}] :one))))

(deftest dissoc-deeply-contained-value
  (is (= [:or [{:account-id 1}
               {:description "test"}]]
         (lib/deep-dissoc [:or [{:account-id 1
                                  :reconciled true}
                                 {:description "test"}]]
                           :reconciled))))

(deftest get-a-uuid
  (let [str-id "10000000-0000-0000-0000-000000000000"
        id (UUID/fromString str-id)]
    (is (= id (lib/uuid str-id))
        "A string is parsed into a uuid")
    (is (= id (lib/uuid id))
        "A UUID is returned as-is")
    (is (instance? UUID (lib/uuid))
        "With no args, a new UUID is returned")))
