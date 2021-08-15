(ns dgknght.app-lib.models-test
  (:require [clojure.test :refer [deftest is are]]
            [dgknght.app-lib.models :as models]))

(deftest get-a-model-id
  (is (= 42 (models/->id 42)))
  (is (= 42 (models/->id {:id 42})))
  (is (thrown? clojure.lang.ExceptionInfo #"Model has no :id" (models/->id {:name "John Doe"}))))

(deftest create-an-index-map
  (let [records [{:id 1 :name "One"}
                 {:id 2 :name "Two"}]]
    (is (= {1 {:id 1 :name "One"}
            2 {:id 2 :name "Two"}}
           (models/map-index records))
        "The default key-fn is :id")
    (is (= {"One" {:id 1 :name "One"}
            "Two" {:id 2 :name "Two"}}
           (models/map-index :name records))
        "The key-fn can be specified")))

(def ^:private flat-list
  [{:id 1
    :name "Group 1"}
   {:id 2
    :name "Group 1.1"
    :parent 1}
   {:id 3
    :name "Group 1.2"
    :parent 1
    :value 40M}
   {:id 4
    :name "Group 2"
    :value 30M}
   {:id 5
    :name "Group 2.1"
    :parent 4
    :value 20M}
   {:id 6
    :name "Group 1.2.1"
    :parent 3
    :value 10M}])

(def ^:private nested-list
  [{:id 1
    :name "Group 1"
    :children-value 50M
    :children [{:id 2
                :name "Group 1.1"
                :parent 1}
               {:id 3
                :name "Group 1.2"
                :parent 1
                :value 40M
                :children-value 10M
                :children [{:id 6
                            :name "Group 1.2.1"
                            :value 10M
                            :parent 3}]}]}
   {:id 4
    :name "Group 2"
    :value 30M
    :children-value 20M
    :children [{:id 5
                :parent 4
                :name "Group 2.1"
                :value 20M}]}])

(def ^:private unnested-list
  [{:id 1
    :name "Group 1"
    :path ["Group 1"]
    :child-count 2
    :children-value 50M
    :parent-ids []}
   {:id 2
    :name "Group 1.1"
    :parent 1
    :path ["Group 1" "Group 1.1"]
    :child-count 0
    :parent-ids [1]}
   {:id 3
    :name "Group 1.2"
    :parent 1
    :path ["Group 1" "Group 1.2"]
    :child-count 1
    :children-value 10M
    :value 40M
    :parent-ids [1]}
   {:id 6
    :name "Group 1.2.1"
    :parent 3
    :path ["Group 1" "Group 1.2" "Group 1.2.1"]
    :child-count 0
    :value 10M
    :parent-ids [3 1]}
   {:id 4
    :name "Group 2"
    :path ["Group 2"]
    :child-count 1
    :value 30M
    :children-value 20M
    :parent-ids []}
   {:id 5
    :parent 4
    :name "Group 2.1"
    :path ["Group 2" "Group 2.1"]
    :child-count 0
    :value 20M
    :parent-ids [4]}])

(deftest nest-a-list
  (is (= nested-list
         (models/nest {:id-fn :id
                       :parent-fn :parent
                       :decorate-parent-fn (fn [group]
                                             (assoc group
                                                    :children-value
                                                    (->> (:children group)
                                                         (mapcat (juxt :value :children-value))
                                                         (filter identity)
                                                         (reduce +))))}
                      flat-list))
      "The children are moved to the correct parent"))

(deftest unnest-a-list
  (is (= unnested-list
         (models/unnest {:id-fn :id
                            :path-segment-fn :name}
                           nested-list))))

(deftest extract-a-nested-model
  (are [input expected] (= expected (models/extract-nested input :customer))
       {:id 1
        :order-number "001"
        :customer-id 2
        :customer-first-name "John"
        :customer-last-name "Doe"}
       {:id 1
        :order-number "001"
        :customer-id 2
        :customer {:id 2
                   :first-name "John"
                   :last-name "Doe"}}

       {:id 1
        :order-number "001"
        :customer-id 2}
       {:id 1
        :order-number "001"
        :customer-id 2}))
