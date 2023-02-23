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
          "The request can be verified by partial url")
      (is (called? :once calls "https://mysite.com")
          "The request can be verified by exact url")
      (is (called? :once calls {:url #"mysite" :method :get})
          "The request can be verified by url and method")
      (is (called? :once calls #(string/ends-with? (:url %) "mysite.com"))
          "A request can be verified with a function")
      (is (not-called? calls {:url #"othersite"})
          "A non-call can be verified by url")
      (is (called-with-headers? :once calls {"content-type" "application/json"})
          "A request can be verified by headers"))))

(deftest mock-a-request-with-a-query-string
  (m/with-web-mocks [calls] mocks
    (http/get "https://mysite.com" {:query-string "a=1"})
    (is (called? :once calls #(= "1" (get-in % [:query-params :a]))))))

(deftest check-if-called-twice
  (m/with-web-mocks [calls] mocks
    (http/get "https://mysite.com")
    (http/get "https://mysite.com")
    (is (called? :twice calls #"mysite\.com")
        "A call can be checked for multple occurrences")))

(deftest check-if-called-more-than-twice
  (m/with-web-mocks [calls] mocks
    (http/get "https://mysite.com")
    (http/get "https://mysite.com")
    (http/get "https://mysite.com")
    (is (called? {:times 3} calls #"mysite\.com")
        "A call can be checked for multple occurrences")))

(deftest no-mock-is-found
  (let [printed (atom [])]
    (with-redefs [println (fn [& args]
                            (swap! printed conj args))]
      (m/with-web-mocks [calls] mocks
        (let [res (http/get "https://othersite.com")
              [m1 m2 :as msgs] @printed]
          (is (nil?  res) "Nothing is returned")
          (is (= 2 (count msgs))
              "A message is printed")
          (is (= ["Unable to match the request: "
                  "{:method :get, :url \"https://othersite.com\"}\n"]
                 m1)
              "The request is printed to standard out")
          (is (= "Mocks " (first m2))
              "The mocks are printed to standard out"))))))
