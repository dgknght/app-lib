(ns dgknght.app-lib.api-3-test
  (:require [cljs.test :refer [deftest is async]]
            [cljs.pprint :refer [pprint]]
            [cljs.core.async :as a]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [cljs-http.client :as http]
            [dgknght.app-lib.api-3 :as api]))

; - on 2XX, call on-success
; - on 4XX and 5XX call on-failure
; - on exception call on-error. If on-error returns nil
;   - call on-failure
;   - call on-success


; - :on-success is called when the call succeeds (2XX status)
; - :on-failure is called when the call fails
;   - because of a 4XX or 5XX status
;   - because of an unhandled exception
; - :on-error allows for an exception to be handled and a workable value supplied

(def blank-state
  {:calls []
   :callbacks {:on-success []
               :on-failure []
               :callback 0}})

(defn- assert-successful-get
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

    (is (= 0 (count (:on-failure callbacks)))
        "The :on-failure callback is not invoked")

    (done)))

(defn- invoke-get
  [url state]
  (api/get url
           {:on-success (fn [x]
                          (swap! state
                                 update-in
                                 [:callbacks :on-success]
                                 conj
                                 x)
                          ; NB: what we return here will be the final result
                          x)
            :on-failure (fn [x]
                          (swap! state
                                 update-in
                                 [:callbacks :on-failure]
                                 conj
                                 x)
                          nil)
            :callback #(swap! state update-in [:callbacks :callback] inc)}))

(defn- mock-get
  [state response]
  (fn [& [_uri {:keys [channel]} :as args]]
    (swap! state update-in [:calls] conj args)
    ; This is what cljs-http does
    (let [c (a/chan)
          res (if channel
                (a/pipe c channel)
                c)]
      (a/go (a/>! c response))
      res)))

(deftest get-a-resource
  (async
    done
    (let [state (atom blank-state)
          t (delay (assert-successful-get state done))]
      (with-redefs [http/get (mock-get state
                                       {:status 200
                                        :body "OK"})]
        (let [returned (invoke-get "https://myapp.com/" state)]
          (is (satisfies? Channel returned)
              "An async channel is returned")
          (a/go (let [final-result (a/<! returned)]
                  (is (= "OK" final-result)
                      "The body is the final result")
                  (deref t))))))))

(defn- assert-not-found
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

    (let [[x :as xs] (:on-failure callbacks)]
      (is (= 1 (count xs))
          "The :on-failure callback is invoked once")
      (is (= "Not found" x)
          "The :on-failure callback is invoked with the body of the response"))

    (is (= 0 (count (:on-success callbacks)))
        "The :on-success callback is not invoked")

    (done)))

(deftest resource-not-found
  (async
    done
    (let [state (atom blank-state)
          t (delay (assert-not-found state done))]
      (with-redefs [http/get (mock-get state
                                       {:status 404
                                        :body "Not found"})]
        (let [returned (invoke-get "https://myapp.com/" state)]
          (is (satisfies? Channel returned)
              "An async channel is returned")
          (a/go (a/<! returned)
                (deref t)))))))
