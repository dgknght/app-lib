(ns dgknght.app-lib.test-assertions-test
  (:require #?(:clj  [clojure.test :refer [deftest is]]
               :cljs [cljs.test :refer [deftest is]])
            #?(:clj [clj-time.core :as t])
            [dgknght.app-lib.test-assertions]))

(deftest assert-comparability
  (is #?(:clj  (comparable? {:one 1}
                             {:one 1
                              :id 100})
         :cljs (dgknght.app-lib.test-assertions/comparable?
                 {:one 1}
                 {:one 1
                  :id 100}))
      "Attributes with the same key are compared, extra attributes are ignored."))

(deftest assert-seq-of-similar-maps
  (is #?(:clj  (seq-of-maps-like? [{:one 1}
                                   {:two 2}]
                                  [{:one 1
                                    :id 100}
                                   {:two 2
                                    :id 102}])
         :cljs (dgknght.app-lib.test-assertions/seq-of-maps-like?
                 [{:one 1}
                  {:two 2}]
                 [{:one 1
                   :id 100}
                  {:two 2
                   :id 102}]))
      "Attributes with the same key are compared, extra attributes are ignored."))

(deftest assert-seq-contains-a-similar-map
  (is #?(:clj  (seq-with-map-like? {:one 1}
                                   [{:one 1
                                     :id 100}
                                    {:two 2
                                     :id 102}])
         :cljs (dgknght.app-lib.test-assertions/seq-with-map-like?
                 {:one 1}
                 [{:one 1
                   :id 100}
                  {:two 2
                   :id 102}]))
      "If at least on similar map exists, it succeeds"))

(deftest assert-seq-does-not-contain-a-similar-map
  (is #?(:clj  (seq-with-no-map-like? {:one 2}
                                      [{:one 1
                                        :id 100}
                                       {:two 2
                                        :id 102}])
         :cljs (dgknght.app-lib.test-assertions/seq-with-no-map-like?
                 {:one 2}
                 [{:one 1
                   :id 100}
                  {:two 2
                   :id 102}]))
      "If at least no similar map exists, it succeeds"))

(deftest assert-seq-contains-a-value
  (is #?(:clj  (seq-containing-value? :one
                                      [:one :two])
         :cljs (dgknght.app-lib.test-assertions/seq-containing-value?
                 :one
                 [:one :two]))
      "If the value exists, it succeeds"))

(deftest assert-seq-does-not-contain-a-value
  (is #?(:clj  (seq-excluding-value? :three
                                     [:one :two])
         :cljs (dgknght.app-lib.test-assertions/seq-excluding-value?
                 :three
                 [:one :two]))
      "If the value exists, it succeeds"))

(deftest assert-seq-contains-a-model
  (is #?(:clj  (seq-containing-model? 101
                                      [{:id 100}
                                       {:id 101}])
         :cljs (dgknght.app-lib.test-assertions/seq-containing-model?
                 101
                 [{:id 100}
                  {:id 101}]))
      "If a map containing the specified :id attribute exists, it succeeds"))

(deftest assert-seq-excludes-a-model
  (is #?(:clj  (seq-excluding-model? 201
                                     [{:id 100}
                                      {:id 101}])
         :cljs (dgknght.app-lib.test-assertions/seq-excluding-model?
                 201
                 [{:id 100}
                  {:id 101}]))
      "If a map containing the specified :id attribute exists, it fails"))

(deftest assert-an-http-response-is-successful
  (is #?(:clj  (http-success? {:status 200})
         :cljs (dgknght.app-lib.test-assertions/http-success?
                 {:status 200}))
      "Status 200 is successful")
  (is #?(:clj  (http-success? {:status 201})
         :cljs (dgknght.app-lib.test-assertions/http-success?
                 {:status 201}))
      "Status 201 is successful")
  (is #?(:clj  (http-success? {:status 204})
         :cljs (dgknght.app-lib.test-assertions/http-success?
                 {:status 204}))
      "Status 204 is successful"))

(deftest assert-an-http-response-is-successful-creation
  (is #?(:clj  (http-created? {:status 201})
         :cljs (dgknght.app-lib.test-assertions/http-created?
                 {:status 201}))
      "Status 201 is successfully created"))

(deftest assert-an-http-response-has-no-content
  (is #?(:clj  (http-no-content? {:status 204})
         :cljs (dgknght.app-lib.test-assertions/http-no-content?
                 {:status 204}))
      "Status 204 is no content"))

(deftest assert-an-http-response-is-a-redirect
  (is #?(:clj  (http-redirect-to?
                 "https://mysite.com/over-here"
                 {:status 302
                  :headers {"Location" "https://mysite.com/over-here"}})
         :cljs (dgknght.app-lib.test-assertions/http-redirect-to?
                 "https://mysite.com/over-here"
                 {:status 302
                  :headers {"Location" "https://mysite.com/over-here"}}))
      "Status 302 is a redirect")
  (is #?(:clj  (http-redirect-to?
                 #"over-here$"
                 {:status 302
                  :headers {"Location" "https://mysite.com/over-here"}})
         :cljs (dgknght.app-lib.test-assertions/http-redirect-to?
                 #"over-here$"
                 {:status 302
                  :headers {"Location" "https://mysite.com/over-here"}}))
      "A regular expression can be used to match the redirect location"))

(deftest assert-an-http-response-indicates-bad-request
  (is #?(:clj  (http-bad-request? {:status 400})
         :cljs (dgknght.app-lib.test-assertions/http-bad-request?
                 {:status 400}))
      "Status 400 is bad request"))

(deftest assert-an-http-response-indicates-unauthorized
  (is #?(:clj  (http-unauthorized? {:status 401})
         :cljs (dgknght.app-lib.test-assertions/http-unauthorized?
                 {:status 401}))
      "Status 401 is unauthorized"))

(deftest assert-an-http-response-indicates-forbidden
  (is #?(:clj  (http-forbidden? {:status 403})
         :cljs (dgknght.app-lib.test-assertions/http-forbidden?
                 {:status 403}))
      "Status 403 is forbidden"))

(deftest assert-an-http-response-indicates-not-found
  (is #?(:clj  (http-not-found? {:status 404})
         :cljs (dgknght.app-lib.test-assertions/http-not-found?
                 {:status 404}))
      "Status 404 is not-found"))

(deftest assert-an-http-response-indicates-teapot
  (is #?(:clj  (http-teapot? {:status 418})
         :cljs (dgknght.app-lib.test-assertions/http-teapot?
                 {:status 418}))
      "Status 418 is teapot"))

(deftest assert-an-http-response-indicates-unprocessable
  (is #?(:clj  (http-unprocessable? {:status 422})
         :cljs (dgknght.app-lib.test-assertions/http-unprocessable?
                 {:status 422}))
      "Status 422 is unprocessable"))

(deftest assert-uri-equality
  (is #?(:clj  (url-like? "https://www.mysite.com/?one=1&two=2"
                           "https://www.mysite.com/?one=1&two=2")
         :cljs (dgknght.app-lib.test-assertions/url-like?
                 "https://www.mysite.com/?one=1&two=2"
                 "https://www.mysite.com/?one=1&two=2"))
      "Exact matches yield success")
  (is #?(:clj  (url-like? "https://www.mysite.com/?one=1&two=2"
                           "https://www.mysite.com/?two=2&one=1")
         :cljs (dgknght.app-lib.test-assertions/url-like?
                 "https://www.mysite.com/?one=1&two=2"
                 "https://www.mysite.com/?two=2&one=1"))
      "The same query values in a different order yield success")
  (is #?(:clj  (not-url-like? "https://www.mysite.com/?one=1&two=2"
                               "https://www.mysite.com/?one=uno&two=2")
         :cljs (dgknght.app-lib.test-assertions/not-url-like?
                 "https://www.mysite.com/?one=1&two=2"
                 "https://www.mysite.com/?one=uno&two=2"))
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
  (is #?(:clj  (mime-msg-containing? mime-msg "text/html" #"Hello\!")
         :cljs (dgknght.app-lib.test-assertions/mime-msg-containing?
                 mime-msg "text/html" #"Hello\!"))
      "Text can be matched in the HTML content")
  (is #?(:clj  (mime-msg-containing? mime-msg "text/plain" #"texter")
         :cljs (dgknght.app-lib.test-assertions/mime-msg-containing?
                 mime-msg "text/plain" #"texter"))
      "Text can be matched in the text content"))

(deftest assert-content-not-in-a-mime-message
  (is #?(:clj  (mime-msg-not-containing? mime-msg "text/html" #"texter")
         :cljs (dgknght.app-lib.test-assertions/mime-msg-not-containing?
                 mime-msg "text/html" #"texter"))
      "Text can be asserted NOT to be in the HTML content")
  (is #?(:clj  (mime-msg-not-containing? mime-msg "text/plain" #"Hello\!")
         :cljs (dgknght.app-lib.test-assertions/mime-msg-not-containing?
                 mime-msg "text/plain" #"Hello\!"))
      "Text can be asserted NOT to be in the text content"))

(def ^:private http-response
  {:status 200
   :headers {"Set-Cookie" ["session-id=abc123"]}})

(deftest assert-http-response-with-cookie
  (is #?(:clj  (http-response-with-cookie? "session-id" "abc123" http-response)
         :cljs (dgknght.app-lib.test-assertions/http-response-with-cookie?
                 "session-id"
                 "abc123"
                 http-response)))
  (is #?(:clj  (http-response-without-cookie? "user-id" http-response)
         :cljs (dgknght.app-lib.test-assertions/http-response-without-cookie?
                 "user-id"
                 http-response))))

#?(:clj
   (deftest assert-same-date
     (is (same-date? (t/local-date 2000 3 2)
                     (t/local-date 2000 3 2))
         "Two local-date instances initialized with the same values are the same")))

(deftest assert-spec-validation
  (is #?(:clj  (conformant? #{:one :two}
                             :one)
         :cljs (dgknght.app-lib.test-assertions/conformant?
                 #{:one :two}
                 :one))
      "Conformance to a spec can be asserted"))

#?(:clj
   (deftest assert-an-exception-with-ex-data
     (is (thrown-with-ex-data?
           "Something went wrong"
           {:one 1}
           (throw (ex-info "Something went wrong" {:one 1}))))))
