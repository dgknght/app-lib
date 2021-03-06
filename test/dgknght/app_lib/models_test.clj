(ns dgknght.app-lib.models-test
  (:require [clojure.test :refer [deftest is]]
            [dgknght.app-lib.models :as models]))

(def ^:private flat-list
  [{:id 1
    :name "Group 1"}
   {:id 2
    :name "Group 1.1"
    :parent 1}
   {:id 3
    :name "Group 1.2"
    :parent 1}
   {:id 4
    :name "Group 2"}
   {:id 5
    :name "Group 2.1"
    :parent 4}
   {:id 6
    :name "Group 1.2.1"
    :parent 3}])

(def ^:private nested-list
  [{:id 1
    :name "Group 1"
    :children [{:id 2
                :name "Group 1.1"
                :parent 1}
               {:id 3
                :name "Group 1.2"
                :parent 1
                :children [{:id 6
                            :name "Group 1.2.1"
                            :parent 3}]}]}
   {:id 4
    :name "Group 2"
    :children [{:id 5
                :name "Group 2.1"
                :parent 4}]}])

(def ^:private unnested-list
  [{:id 1
    :name "Group 1"
    :path ["Group 1"]
    :child-count 2
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
    :parent-ids [1]}
   {:id 6
    :name "Group 1.2.1"
    :parent 3
    :path ["Group 1" "Group 1.2" "Group 1.2.1"]
    :child-count 0
    :parent-ids [3 1]}
   {:id 4
    :name "Group 2"
    :path ["Group 2"]
    :child-count 1
    :parent-ids []}
   {:id 5
    :name "Group 2.1"
    :parent 4
    :path ["Group 2" "Group 2.1"]
    :child-count 0
    :parent-ids [4]}])

(deftest nestify-a-list
  (is (= nested-list
         (models/nest :id :parent flat-list))
      "The children are moved to the correct parent"))

(deftest unnest-a-list
  (is (= unnested-list
         (models/unnest {:id-fn :id
                            :path-segment-fn :name}
                           nested-list))))
