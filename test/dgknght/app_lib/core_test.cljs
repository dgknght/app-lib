(ns dgknght.app-lib.core-test
  (:require [cljs.test :refer-macros [deftest testing are is]]
            [dgknght.app-lib.decimal :refer [->decimal]]
            [dgknght.app-lib.core :as lib]))

(deftest parse-a-boolean
  (are [input expected] (= expected (lib/parse-bool input))
       true    true
       false   false
       "true"  true
       "True"  true
       "TRUE"  true
       "1"     true
       "false" false
       "False" false
       "FALSE" false
       "0"     false))

(deftest parse-an-integer
  (are [input expected] (= expected (lib/parse-int input))
       "1"     1
       nil     nil
       ""      nil
       "1,000" 1000
       123     123))

(deftest parse-a-floating-point-number
  (are [input expected] (= expected (lib/parse-float input))
       "1"    1.0
       "1.23" 1.23
       nil    nil
       ""     nil
       1.23   1.23))

(deftest parse-a-decimal
  (are [in e] (= e (lib/parse-decimal in))
       "10"         (->decimal 10)
       "10.1"       (->decimal 10.1)
       "-10.1"      (->decimal -10.1)
       ""           nil
       "notanumber" nil))

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

(deftest conditionally-update-in-deeply
  (is (= [:or {:one 1} {:two 2}]
         (lib/deep-update-in-if [:or {:one 1} {:two 1}]
                                :two
                                inc)))
  (is (= {:two 2}
         (lib/deep-update-in-if {:two 1}
                                :two
                                inc)))
  (is (= '(:one)
         (lib/deep-update-in-if '(:one)
                                :two
                                inc))))

(deftest assoc-if-value-is-present
  (are [m k v expected] (= expected (lib/assoc-if m k v))
       {} :value 1   {:value 1}
       {} :value nil {}))

(deftest assoc-unless-value-is-already-present
  (are [m k v expected] (= expected (lib/assoc-unless m k v))
       {:value 2}   :value 1 {:value 2}
       {:value nil} :value 1 {:value nil}
       {}           :value 1 {:value 1}))

(deftest test-for-deeply-contained-key
  (are [input expected] (= expected
                           (lib/deep-contains? input :one))
       {}              false
       {:one 1}        true
       [:or
        {:one 1}
        {:two 2}]      true))

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

(deftest check-for-the-presence-of-a-value
  (is (not (lib/present? nil)))
  (is (not (lib/present? "")))
  (is (not (lib/present? [])))
  (is (not (lib/present? {})))
  (is (not (lib/present? #{})))
  (is (not (lib/present? '())))
  (is (lib/present? "1"))
  (is (lib/present? 1))
  (is (lib/present? [1]))
  (is (lib/present? #{1}))
  (is (lib/present? '(1)))
  (is (lib/present? {:one 1})))

(deftest get-the-presence-of-a-value
  (is (nil? (lib/presence ""))
      "An empty string has no presence")
  (is (nil? (lib/presence []))
      "An empty vector has no presence")
  (is (nil? (lib/presence '()))
      "An empty list has no presence")
  (is (nil? (lib/presence {}))
      "An empty map has no presence")
  (is (nil? (lib/presence #{}))
      "An empty set has no presence")
  (is (= "one" (lib/presence "one"))
      "A non-empty string has presence")
  (is (= [:one] (lib/presence [:one]))
      "A non-empty vector has presence")
  (is (= {:one 1} (lib/presence {:one 1}))
      "A non-empty map has presence")
  (is (= #{:one} (lib/presence #{:one}))
      "A non-empty set has presence"))

(deftest ensure-a-string-value
  (is (= "test" (lib/ensure-string "test")))
  (is (= "test" (lib/ensure-string :test)))
  (is (= "1" (lib/ensure-string 1))))

(deftest index-a-collection
  (let [coll [{:id 1 :name "One"}
              {:id 2 :name "Two"}]
        indexed (lib/index-by :id coll)]
    (is (= {:id 1 :name "One"}
           (get-in indexed [1])))))

(deftest prune-data-structures
  (testing "prune a simple map"
    (is (= {:one 1
            :two 2
            :list [:one]}
           (lib/prune-to {:one 1
                          :two 2
                          :three 3
                          :list [:one]}
                         {:one "one"
                          :two "two"
                          :list ["one"]}))
        "The keys that match the target are included"))
  (testing "prune a list of simple maps"
    (is (= [{:one 1}
            {:one 10
             :two 20}]
           (lib/prune-to [{:one 1
                           :three 3}
                          {:one 10
                           :two 20
                           :three 30}]
                         [{:one "one"}
                          {:one "ten"
                           :two "twenty"}]))))
  (testing "prune a map that contains sequences"
    (is (= {:maps [{:one 1}
                   {:one 10
                    :two 20}]}
           (lib/prune-to {:maps [{:one 1
                                  :three 3}
                                 {:one 10
                                  :two 20
                                  :three 30}]}
                         {:maps [{:one "one"}
                                 {:one "ten"
                                  :two "twenty"}]}))
        "The contained sequence is pruned correctly")))

(deftest safely-extract-from-list-at-position
  (are [coll index expected] (= expected (lib/safe-nth coll index))
       [:zero :one] 0 :zero
       [:zero :one] 1 :one
       [:zero :one] 2 nil
       nil          0 nil))

(deftest build-a-string
  (is (= "xxx" (lib/mkstr "x" 3))))

(deftest conj-to-last-in-list
  (is (= '((:new 1 2) (3 4))
         (lib/conj-to-last '((1 2) (3 4))
                           :new))))

(deftest ensure-a-value-is-a-sequence
  (are [input expected] (= expected (lib/->sequential input))
       [:one] [:one]
       :one   [:one]
       {}     [{}]
       [{}]   [{}]))

(deftest create-a-fn-that-ensures-a-sequence
  (is (= {:one [1 2]}
         (update-in {:one 1}
                    [:one]
                    (lib/fscalar conj)
                    2))
      "A scalar value is replaced by a collection containing the value, then the value is added")
  (is (= {:one [2]}
         (update-in {}
                    [:one]
                    (lib/fscalar (fnil conj []))
                    2))
      "The missing attribute is created"))

(deftest decrement-a-value-to-a-minumum
  (let [f (lib/fmin dec 1)]
    (are [input expected] (= expected (f input))
          3 2
          2 1
          1 1
          0 1
         -1 1)))

(deftest increment-a-value-to-a-maximum
  (let [f (lib/fmax inc 2)]
    (are [input expected] (= expected (f input))
         2 2
         1 2
         0 1
         -1 0)))

(deftest identify-a-derefable
  (is (lib/derefable? (atom {})) "An atom is derefable")
  (is (not (lib/derefable? {})) "A map is not derefable"))

(deftest identify-a-collection-with-one-element
  (is (lib/one? [:a])
      "A collection with one element is one")
  (is (not (lib/one? []))
      "An empty collection is not one")
  (is (not (lib/one? [:a :b]))
      "A collection with more than one element is not one"))

(deftest deeply-dissoc-a-value
  (is (= {:value {:one 1}}
         (lib/dissoc-in {:value {:one 1
                                 :two 2}}
                        [:value :two])))
  (is (= {:one 1}
         (lib/dissoc-in {:one 1
                         :two 2}
                        [:two]))))
