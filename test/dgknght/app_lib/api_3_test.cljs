(ns dgknght.app-lib.api-3-test
  (:require [cljs.test :refer [deftest is async]]
            [cljs.core.async :as a]
            [cljs-http.client :as http]
            [dgknght.app-lib.api-3 :as api]))

(deftest get-a-resource
  (async
    done
    (let [calls (atom [])]
      (with-redefs [http/get (fn [& [_uri {:keys [channel]} :as args]]
                               (swap! calls conj args)
                               (a/go
                                 (a/>! channel {:status 200
                                                :body "OK"})))]
        (let [returned (api/get "https://myapp.com/"
                                {:callback (fn [x]
                                             (is (= {:status 200
                                                     :body "OK"}
                                                    x)
                                                 "The HTTP response is passed to the callback fn")
                                             (done))})
              [c :as cs] @calls]
          (is (= 1 (count cs))
              "cljs-http/get is called one time")
          (is (= "https://myapp.com/"
                 (first c))
              "The URI is the 1st arg passed to cljs-http/get")
          (is (= "application/json"
                 (get-in (second c) [:headers "Content-Type"]))
              "The content-type header is application/json")
          (is (= "application/json"
                 (get-in (second c) [:headers "Accept"]))
              "The Accept header is application/json")
          (is (satisfies? cljs.core.async.impl.protocols.Channel returned)
              "An async channel is returned"))))))
