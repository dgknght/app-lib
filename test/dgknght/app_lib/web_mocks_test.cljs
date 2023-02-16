(ns dgknght.app-lib.web-mocks-test
  (:require [cljs.test :refer-macros [deftest is async]]
            [cljs.core.async :as a]
            [cljs-http.client :as http] ; it seems the macro doesn't compile correctly without this
            [dgknght.app-lib.web-mocks :refer-macros [with-web-mocks]]))

(def mocks
  {"https://mysite.com"
   (constantly
     {:status 200
      :body "OK"})})

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
          (done))
        (http/get "https://mysite.com" {:channel ch})))))

#_(deftest mock-a-get-request-with-implicit-channel
  (async
    done
    (with-web-mocks [calls] mocks
      (a/go
        (let [res (a/<! (http/get "https://mysite.com"))]
          (is (= {:status 200
                  :body "OK"}
                 res)
              "The mock response is returned")
          (is (dgknght.app-lib.web-mocks/called? :once calls #"mysite")
              "The request can be verified"))
        (done)))))
