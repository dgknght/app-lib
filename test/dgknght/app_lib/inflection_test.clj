(ns dgknght.app-lib.inflection-test
  (:require [clojure.test :refer [deftest is]]
            [dgknght.app-lib.inflection :refer [plural
                                                humanize
                                                title-case
                                                ordinal
                                                conjoin]]))

(deftest humanize-a-keyword
  (is (= "First name" (humanize :first-name)))
  (is (= "Group" (humanize :group-id))
      "Trailing _id is ignored"))

(deftest title-case-a-keyword
  (is (= "My Page" (title-case :my-page))))

(deftest title-case-a-phrase
  (is (= "My Important ABC Thing" (title-case "my important ABC thing"))))

(deftest get-an-ordinal
  (doseq [[input exp] [[1 "1st"]
                       [2 "2nd"]
                       [3 "3rd"]
                       [4 "4th"]
                       [10 "10th"]
                       [11 "11th"]
                       [12 "12th"]
                       [13 "13th"]
                       [14 "14th"]
                       [21 "21st"]
                       [22 "22nd"]
                       [23 "23rd"]
                       [24 "24th"]]]
    (is (= exp (ordinal input)))))

(deftest pluralize-a-word
  (is (= "ponies" (plural "pony"))
      "The corret ies form is returned for words ending in y")
  (is (= "invitations" (plural "invitation"))
      "The correct plural string form is returned for a string")
  (is (= :invitations (plural :invitation))
      "The correct plural keyword form is returned for a keyword")
  (is (thrown-with-msg?
        java.lang.AssertionError #"^Assert failed: word"
        (plural nil))))

(deftest conjoin-items-in-a-list
  (is (= "one" (conjoin "or" ["one"])))
  (is (= "one or two"
         (conjoin "or" ["one" "two"])))
  (is (= "one, two, and three"
         (conjoin "and" ["one" "two" "three"]))))
