(ns dgknght.app-lib.html-test
  (:require [clojure.test :refer [deftest is]]
            [dgknght.app-lib.html :as html]))

(deftest make-html-for-a-raw-string
  (is (= [:span {:dangerouslySetInnerHTML {:__html "1 < 2"}}]
         (html/raw-string "1 < 2"))))

(deftest make-a-special-character
  (is (= [:span {:dangerouslySetInnerHTML {:__html "&times;"}}]
         (html/special-char :times))))

(deftest make-an-empty-space
  (is (= [:span {:dangerouslySetInnerHTML {:__html "&nbsp;"}}]
         (html/space))))

(deftest make-a-comment
  (is (= [:span {:dangerouslySetInnerHTML {:__html "<!-- my thoughts -->"}}]
         (html/comment "my thoughts"))))

(deftest add-a-class
  (is (= [:div {:class ["d-none"]}]
         (html/add-class [:div] "d-none"))
      "An attribute map is created if it does not exist")
  (is (= [:div {:class ["d-none"]} "My words"]
         (html/add-class [:div {} "My words"] "d-none"))
      "A :class attribute is added if it doesn't exist")
  (is (= [:div {:class "row d-none"} "My words"]
         (html/add-class [:div {:class "row"} "My words"] "d-none"))
      "The class is added into a string value")
  (is (= [:div {:class ["row" "d-none"]} "My words"]
         (html/add-class [:div {:class ["row"]} "My words"] "d-none"))
      "The class is added into a vector"))

(deftest add-multiple-classes
  (is (= [:div {:class ["row" "d-none"]} "My Words"]
         (html/add-classes [:div "My Words"] "row" "d-none"))
      "An attribute map is created is it does not exist")
  (is (= [:div {:class ["row" "d-none"]} "My words"]
         (html/add-classes [:div {} "My words"] "row" "d-none"))
      "A :class attribute is added if it doesn't exist")
  (is (= [:div {:class "flex row d-none"} "My words"]
         (html/add-classes [:div {:class "flex"} "My words"] "row" "d-none"))
      "The classes are added to an existing string")
  (is (= [:div {:class ["flex" "row" "d-none"]} "My words"]
         (html/add-classes [:div {:class ["flex"]} "My words"] "row" "d-none"))
      "An item is added to the vector if the vector already exists"))

(deftest remove-a-class
  (is (= [:div {:class ["two" "three"]}]
         (html/remove-class [:div {:class ["one" "two" "three"]}] "one"))
      "An element is removed from a vector")
  (is (= [:div {:class "two"}]
         (html/remove-class [:div {:class "one two"}] "one"))
      "A portion of a string is removed")
  (is (= [:div "Some content"]
         (html/remove-class [:div "Some content"] "one"))
      "An element without attributes is returned as is")
  (is (= [:div {:id "abc"} "Some content"]
         (html/remove-class [:div {:id "abc"} "Some content"] "one"))
      "An element without a class attribute is returned as is"))
