(ns dgknght.app-lib.api-3-test
  (:require [cljs.test :refer [deftest is async]]
            [cljs.pprint :refer [pprint]]
            [cljs.core.async :as a]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [cljs-http.client :as http]
            [dgknght.app-lib.api-3 :as api]))

; - on 2XX, call on-success
; - on 4XX and 5XX call raise exception (which calls on-error and triggers on-failure)
; - on exception call on-error, which triggers on-failure

(defn- test-get-resource
  [state done]
  (let [{[c :as cs] :calls
         callbacks :callbacks} @state]
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

    (is (= 1 (:callback callbacks))
        "The :callback callback is invoked")

    (let [[x :as xs] (:on-success callbacks)]
      (is (= 1 (count xs))
          "The :on-success callback is invoked once")
      (is (= "OK" x)
          "The :on-success callback is invoked with the body of the response"))

    (done)))

(deftest get-a-resource
  (async
    done
    (let [state (atom {:calls []
                       :callbacks {:on-success []
                                   :on-failure []
                                   :on-error []
                                   :callback 0}})
          t (delay (test-get-resource state done))]
      (with-redefs [http/get (fn [& [_uri {:keys [channel]} :as args]]
                               (swap! state update-in [:calls] conj args)
                               ; This is what cljs-http does
                               (let [c (a/chan)
                                     res (if channel
                                           (a/pipe c channel)
                                           c)]
                                 (a/go
                                   (a/>! c {:status 200
                                            :body "OK"}))
                                 res))]
        (let [returned (api/get "https://myapp.com/"
                                {:on-success (fn [x]
                                               (swap! state
                                                      update-in
                                                      [:callbacks :on-success]
                                                      conj
                                                      x)
                                               ; NB: what we return here will be the final result
                                               x)
                                 :callback #(swap! state update-in [:callbacks :callback] inc)})]
          (is (satisfies? Channel returned)
              "An async channel is returned")
          (a/go (let [final-result (a/<! returned)]
                  (is (= "OK" final-result)
                      "The body is the final result")
                  (deref t))))))))
