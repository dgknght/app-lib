(ns dgknght.app-lib.test-assertions-test
  (:require [clojure.test :refer [deftest is]]
            [clj-time.core :as t]
            [dgknght.app-lib.test-assertions]))

(deftest assert-comparability
  (is (comparable? {:one 1}
                   {:one 1
                    :id 100})
      "Attributes with the same key are compared, extra attributes are ignored."))

(deftest assert-seq-of-similar-maps
  (is (seq-of-maps-like? [{:one 1}
                          {:two 2}]
                         [{:one 1
                           :id 100}
                          {:two 2
                           :id 102}])
      "Attributes with the same key are compared, extra attributes are ignored."))

(deftest assert-seq-contains-a-similar-map
  (is (seq-with-map-like? {:one 1}
                          [{:one 1
                            :id 100}
                           {:two 2
                            :id 102}])
      "If at least on similar map exists, it succeeds"))

(deftest assert-seq-does-not-contain-a-similar-map
  (is (seq-with-no-map-like? {:one 2}
                             [{:one 1
                               :id 100}
                              {:two 2
                               :id 102}])
      "If at least no similar map exists, it succeeds"))

(deftest assert-seq-contains-a-value
  (is (seq-containing-value? :one
                             [:one :two])
      "If the value exists, it succeeds"))

(deftest assert-seq-does-not-contain-a-value
  (is (seq-excluding-value? :three
                            [:one :two])
      "If the value exists, it succeeds"))

(deftest assert-seq-contains-a-model
  (is (seq-containing-model? 101
                             [{:id 100}
                              {:id 101}])
      "If a map containing the specified :id attribute exists, it succeeds"))

(deftest assert-seq-excludes-a-model
  (is (seq-excluding-model?
        201
        [{:id 100}
         {:id 101}])
      "If a map containing the specified :id attribute exists, it fails"))

(deftest assert-an-http-response-is-successful
  (is (http-success? {:status 200})
      "Status 200 is successful")
  (is (http-success? {:status 201})
      "Status 201 is successful")
  (is (http-success? {:status 204})
      "Status 204 is successful"))

(deftest assert-an-http-response-is-successful-creation
  (is (http-created? {:status 201})
      "Status 201 is successfully created"))

(deftest assert-an-http-response-has-no-content
  (is (http-no-content? {:status 204})
      "Status 204 is no content"))

(deftest assert-an-http-response-indicates-bad-request
  (is (http-bad-request? {:status 400})
      "Status 400 is bad request"))

(deftest assert-an-http-response-indicates-unauthorized
  (is (http-unauthorized? {:status 401})
      "Status 401 is unauthorized"))

(deftest assert-an-http-response-indicates-forbidden
  (is (http-forbidden? {:status 403})
      "Status 403 is forbidden"))

(deftest assert-an-http-response-indicates-not-found
  (is (http-not-found? {:status 404})
      "Status 404 is not-found"))

(deftest assert-an-http-response-indicates-teapot
  (is (http-teapot? {:status 418})
      "Status 418 is teapot"))

(deftest assert-an-http-response-indicates-unprocessable
  (is (http-unprocessable? {:status 422})
      "Status 422 is unprocessable"))

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

(def ^:private mime-msg
  {:from "john@sender.com"
   :to "john@recipient.com"
   :subject "This is the subject"
   :body [:alternative
          {:type "text/html"
           :content "<html><body><h1>Hello!</h1></body></html>"}
          {:type "text/plain"
           :content "Hello, texter!"}]})

(deftest assert-content-in-a-mime-message
  (is (mime-msg-containing?  "text/html" #"Hello\!" mime-msg)
      "Text can be matched in the HTML content")
  (is (mime-msg-containing?  "text/plain" #"texter" mime-msg)
      "Text can be matched in the text content"))

(def ^:private http-response
  {:status 200
   :headers {"Set-Cookie" ["session-id=abc123"]}})

(deftest assert-http-response-with-cookie
  (is (http-response-with-cookie?  "session-id" "abc123" http-response))
  (is (http-response-without-cookie?  "user-id" http-response)))

(deftest assert-same-date
  (is (same-date? (t/local-date 2000 3 2)
                  (t/local-date 2000 3 2))
      "Two local-date instances initialized with the same values are the same"))
