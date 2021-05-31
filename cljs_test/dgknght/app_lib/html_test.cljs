(ns dgknght.app-lib.html-test
  (:require [cljs.test :refer-macros [deftest is]]
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
  (is (= [:div {:class ["row" "d-none"]} "My words"]
         (html/add-class [:div {:class "row"} "My words"] "d-none"))
      "An existing string value is added to a vector along with the new value")
  (is (= [:div {:class ["row" "d-none"]} "My words"]
         (html/add-class [:div {:class ["row"]} "My words"] "d-none"))
      "An item is added to the vector if the vector already exists"))

(deftest add-multiple-classes
  (is (= [:div {:class ["row" "d-none"]} "My Words"]
         (html/add-classes [:div "My Words"] "row" "d-none"))
      "An attribute map is created is it does not exist")
  (is (= [:div {:class ["row" "d-none"]} "My words"]
         (html/add-classes [:div {} "My words"] "row" "d-none"))
      "A :class attribute is added if it doesn't exist")
  (is (= [:div {:class ["flex" "row" "d-none"]} "My words"]
         (html/add-classes [:div {:class "flex"} "My words"] "row" "d-none"))
      "An existing string value is added to a vector along with the new value")
  (is (= [:div {:class ["flex" "row" "d-none"]} "My words"]
         (html/add-classes [:div {:class ["flex"]} "My words"] "row" "d-none"))
      "An item is added to the vector if the vector already exists"))
