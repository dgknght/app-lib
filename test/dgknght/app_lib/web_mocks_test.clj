(ns dgknght.app-lib.web-mocks-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.string :as string]
            [clj-http.client :as http] ; it seems the macro doesn't compile correctly without this
            [dgknght.app-lib.web-mocks :as m]))

(def mocks
  {"https://mysite.com"
   (constantly {:status 200
                :body "OK"})})

(deftest mock-a-get-request
  (m/with-web-mocks [calls] mocks
    (let [res (http/get "https://mysite.com"
                        {:headers {"content-type" "application/json"}})]
      (is (= {:status 200
              :body "OK"}
             res)
          "The mocked response is returned")
      (is (called? :once calls #"mysite")
          "The request can be verified by url")
      (is (called? :once calls {:url #"mysite" :method :get})
          "The request can be verified by url and method")
      (is (called? :once calls #(string/ends-with? (:url %) "mysite.com"))
          "A request can be verified with a function")
      (is (not-called? calls {:url #"othersite"})
          "A non-call can be verified by url")
      (is (called-with-headers? :once calls {"content-type" "application/json"})
          "A request can be verified by headers"))))
