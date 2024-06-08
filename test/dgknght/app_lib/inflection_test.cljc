(ns dgknght.app-lib.inflection-test
  (:require [clojure.test :refer [deftest is are]]
            [dgknght.app-lib.inflection :as i]))

(deftest humanize-a-keyword
  (is (= "First name" (i/humanize :first-name)))
  (is (= "Group" (i/humanize :group-id))
      "Trailing _id is ignored"))

(deftest title-case-a-keyword
  (is (= "My Page" (i/title-case :my-page))))

(deftest title-case-a-phrase
  (is (= "My Important ABC Thing" (i/title-case "my important ABC thing"))))

(deftest get-an-ordinal
  (are [input expected] (= expected (i/ordinal input))
       1 "1st"    
       2 "2nd"  
       3 "3rd"  
       4 "4th"  
       10 "10th"
       11 "11th"
       12 "12th"
       13 "13th"
       14 "14th"
       21 "21st"
       22 "22nd"
       23 "23rd"
       24 "24th"))

(deftest pluralize-a-word
  (are [input expected] (= expected (i/plural input))
       "pony"       "ponies"
       "invitation" "invitations"
       "child"      "children"
       "Child"      "Children"
       "man"         "men"
       "woman"       "women"
       :pony        :ponies
       :invitation  :invitations
       :child       :children
       nil          nil))

(deftest singularize-a-word
  (are [input expected] (= expected (i/singular input))
       "ponies"      "pony"       
       "invitations" "invitation" 
       "children"    "child"      
       "Children"    "Child"
       "men"         "man"
       "women"       "woman"
       :ponies       :pony        
       :invitations  :invitation  
       :children     :child       
       nil           nil))

(deftest conjoin-items-in-a-list
  (are [oper list expected] (= expected (i/conjoin oper list))
       "or"  ["one"]               "one"
       "or"  ["one" "two"]         "one or two"
       "and" ["one" "two" "three"] "one, two, and three"))
