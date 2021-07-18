(ns dgknght.app-lib.test-test
  (:require [clojure.test :refer [deftest is are testing]]
            [dgknght.app-lib.test :as t]))

(deftest test-uri-equality
  (testing "equality"
    (are [uri-1 uri-2] (t/uri= uri-1 uri-2)
         "https://www.mysite.com/"       "https://www.mysite.com/"
         "https://www.mysite.com/?one=1" "https://www.mysite.com/?one=1"
         "https://www.mysite.com/?one=1&two=2" "https://www.mysite.com/?one=1&two=2"
         "https://www.mysite.com/?two=2&one=1" "https://www.mysite.com/?one=1&two=2"))
  (testing "inequality"
    (are [uri-1 uri-2] (not (t/uri= uri-1 uri-2))
         "https://www.mysite.com/"       "https://www.mysite.edu"
         "https://www.mysite.com/?one=1" "https://www.mysite.com/"
         "https://www.mysite.com/?one=1" "https://www.mysite.com/?one=2"
         "https://www.mysite.com/?one=1&two=2" "https://www.mysite.com/?one=1"
         "https://www.mysite.com/?two=2&one=1" "https://www.mysite.org/?one=1&two=2")))

(deftest assert-uri-equality
  (is (url-like? "https://www.mysite.com/?one=1&two=2"
                 "https://www.mysite.com/?one=1&two=2")
      "Exact matches yield success")
  (is (url-like? "https://www.mysite.com/?one=1&two=2"
                 "https://www.mysite.com/?two=2&one=1")
      "The same query values in a different order yield success")
  (is (not-url-like? "https://www.mysite.com/?one=1&two=2"
                     "https://www.mysite.com/?one=uno&two=2")
      "A different query value yields failure"))

(deftest compare-maps
  (is (comparable? {:one 1}
                   {:one 1
                    :two 2})))
