(ns dgknght.app-lib.web-mocks-test
  (:require #?(:clj  [clojure.test :refer [deftest is]]
               :cljs [cljs.test :refer-macros [deftest is async]])
            #?(:clj  [clojure.string :as string])
            #?(:clj  [clojure.pprint :refer [pprint]])
            #?(:clj  [clj-http.client :as http] ; necessary here because referencing it in the ns with the macro itself fails during the clj phase of the cljs compilation
               :cljs [cljs-http.client :as http]) ; it seems the macro doesn't compile correctly without this
            #?(:cljs [cljs.core.async :as a])
            #?(:clj  [dgknght.app-lib.web-mocks :as m]
               :cljs [dgknght.app-lib.web-mocks :refer-macros [with-web-mocks]])))

(def mocks
  {"https://mysite.com"
   (constantly {:status 200
                :body "OK"})})

#?(:clj
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
             "A request can be verified by headers")))))

#?(:clj
   (deftest mock-a-request-with-a-query-string
     (m/with-web-mocks [calls] mocks
       (http/get "https://mysite.com" {:query-string "a=1"})
       (is (called? :once calls #(= "1" (get-in % [:query-params :a])))))))

#?(:clj
   (deftest check-if-called-twice
     (m/with-web-mocks [calls] mocks
       (http/get "https://mysite.com")
       (http/get "https://mysite.com")
       (is (called? :twice calls #"mysite\.com")
           "A call can be checked for multple occurrences"))))

#?(:clj
   (deftest check-if-called-more-than-twice
     (m/with-web-mocks [calls] mocks
       (http/get "https://mysite.com")
       (http/get "https://mysite.com")
       (http/get "https://mysite.com")
       (is (called? {:times 3} calls #"mysite\.com")
           "A call can be checked for multple occurrences"))))

#?(:clj
   (deftest no-mock-is-found
     (m/with-web-mocks [calls] mocks
       (with-redefs [pprint (constantly nil)]
         (is (nil? (http/get "https://othersite.com")) "Nothing is returned")))))

#?(:cljs
   (deftest mock-a-get-request-with-explicit-channel
     (async
       done
       (with-web-mocks [calls] mocks
         (let [ch (a/chan)]
           (a/go
             (let [res (a/<! ch)]
               (is (= {:status 200
                       :body "OK"}
                      res)
                   "The mock response is returned")
               (is (dgknght.app-lib.web-mocks/called? :once calls #"mysite")
                     "The request can be verified by url"))
               (is (dgknght.app-lib.web-mocks/called? :once calls {:url #"mysite"
                                                                   :method :get})
                     "The request can be verified by url and method")
               (is (dgknght.app-lib.web-mocks/called-with-headers?
                     :once
                     calls
                     {"content-type" "application/json"}))
             (done))
           (http/get "https://mysite.com" {:channel ch
                                           :headers {"content-type" "application/json"}
                                           :query-string "a=1"}))))))
