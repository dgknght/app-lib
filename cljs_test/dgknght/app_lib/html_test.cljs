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
