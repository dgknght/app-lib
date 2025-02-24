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

(defn- callbacks
  [state]
  {:on-success (fn [x]
                 (swap! state
                        update-in
                        [:callbacks :on-success]
                        conj
                        x)
                 ; NB: what we return here will be the final result
                 x)
   :on-error (fn [e]
               (swap! state
                      update-in
                      [:callbacks :on-error]
                      conj
                      e)
               e) ; NB: returning the error here triggers on-failure instead of on-success
   :on-failure (fn [e]
                 (swap! state
                        update-in
                        [:callbacks :on-failure]
                        conj
                        e)
                 nil)
   :callback #(swap! state update-in [:callbacks :callback] inc)})

(defn- invoke-get
  [url state & {:as opts}]
  (api/get url (merge opts (callbacks state))))

(defn- invoke-post
  [url resource state & {:as opts}]
  (api/post url resource (merge opts (callbacks state))))

(defn- mock-request
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

(defn- assert-successful-get
  [calls callbacks]
  (is (= 1 (count calls))
      "cljs-http/get is called one time")
  (is (= "https://myapp.com/"
         (ffirst calls))
      "The URI is the 1st arg passed to cljs-http/get")

  (is (= 1 (:callback callbacks))
      "The :callback callback is invoked")

  (let [[x :as xs] (:on-success callbacks)]
    (is (= 1 (count xs))
        "The :on-success callback is invoked once")
    (is (= "OK" x)
        "The :on-success callback is invoked with the body of the response"))

  (is (= 0 (count (:on-failure callbacks)))
      "The :on-failure callback is not invoked"))

(defn- assert-successful-get-with-json
  [state done]
  (let [{[call :as calls] :calls
         callbacks :callbacks} @state]
    (assert-successful-get calls callbacks)

    (is (= "application/json"
           (get-in (second call) [:headers "Content-Type"]))
        "The GET content-type header is application/json")
    (is (= "application/json"
           (get-in (second call) [:headers "Accept"]))
        "The GET Accept header is application/json")

    (done)))

(deftest get-a-resource-with-json
  (async
    done
    (let [state (atom blank-state)
          t (delay (assert-successful-get-with-json state done))]
      (with-redefs [http/get (mock-request state
                                       {:status 200
                                        :body "OK"})]
        (let [returned (invoke-get "https://myapp.com/" state)]
          (is (satisfies? Channel returned)
              "An async channel is returned")
          (a/go (let [final-result (a/<! returned)]
                  (is (= "OK" final-result)
                      "The body is the final result")
                  (deref t))))))))

(defn- assert-successful-get-with-edn
  [state done]
  (let [{[call :as calls] :calls
         callbacks :callbacks} @state]

    (assert-successful-get calls callbacks)

    (is (= "application/edn"
           (get-in (second call) [:headers "Content-Type"]))
        "The GET content-type header is application/edn")
    (is (= "application/edn"
           (get-in (second call) [:headers "Accept"]))
        "The GET Accept header is application/edn")

    (done)))

(deftest get-a-resource-with-edn
  (async
    done
    (let [state (atom blank-state)
          t (delay (assert-successful-get-with-edn state done))]
      (with-redefs [http/get (mock-request state
                                       {:status 200
                                        :body "OK"})]
        (let [returned (invoke-get "https://myapp.com/" state :encoding :edn)]
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

    (is (= 1 (:callback callbacks))
        "The :callback callback is invoked")

    (let [[e :as es] (:on-failure callbacks)]
      (is (= 1 (count es))
          "The :on-failure callback is invoked once")
      (is (= "Not found" (ex-message e))
          "The :on-failure callback is invoked with the body of the response"))

    (is (= 0 (count (:on-success callbacks)))
        "The :on-success callback is not invoked")

    (done)))

(deftest resource-not-found
  (async
    done
    (let [state (atom blank-state)
          t (delay (assert-not-found state done))]
      (with-redefs [http/get (mock-request state
                                       {:status 404
                                        :body "Not found"})]
        (let [returned (invoke-get "https://myapp.com/" state)]
          (is (satisfies? Channel returned)
              "An async channel is returned")
          (a/go (a/<! returned)
                (deref t)))))))

(defn- assert-successful-post
  [calls callbacks]
  (is (= 1 (count calls))
      "cljs-http/post is called one time")
  (is (= "https://myapp.com/things"
         (ffirst calls))
      "The URI is the 1st arg passed to cljs-http/post")

  (is (= 1 (:callback callbacks))
      "The :callback callback is invoked one time")

  (let [[x :as xs] (:on-success callbacks)]
    (is (= 1 (count xs))
        "The :on-success callback is invoked once")
    (is (= {:name "Albert"} x)
        "The :on-success callback is invoked with the body of the response"))

  (is (= 0 (count (:on-failure callbacks)))
      "The :on-failure callback is not invoked"))

(defn- assert-successful-post-with-json
  [state done]
  (let [{[c :as cs] :calls
         callbacks :callbacks} @state]
    (assert-successful-post cs callbacks)

    (is (= "application/json"
           (get-in (second c) [:headers "Content-Type"]))
        "The POST content-type header is application/json")
    (is (= "application/json"
           (get-in (second c) [:headers "Accept"]))
        "The POST Accept header is application/json")

    (done)))

(deftest post-a-resource-with-default-content-type
  (async
    done
    (let [state (atom blank-state)
          t (delay (assert-successful-post-with-json state done))]
      (with-redefs [http/post (mock-request state
                                            {:status 200
                                             :body {:name "Albert"}})]
        (let [res (invoke-post "https://myapp.com/things" {:name "Albert"} state)]
          (is (satisfies? Channel res)
              "An async channel is returned")
          (a/go (a/<! res)
                (deref t)))))))

(deftest post-a-resource-with-json
  (async
    done
    (let [state (atom blank-state)
          t (delay (assert-successful-post-with-json state done))]
      (with-redefs [http/post (mock-request state
                                            {:status 200
                                             :body {:name "Albert"}})]
        (let [res (invoke-post "https://myapp.com/things"
                               {:name "Albert"}
                               state
                               :encoding :json)]
          (is (satisfies? Channel res)
              "An async channel is returned")
          (a/go (a/<! res)
                (deref t)))))))

(defn- assert-successful-post-with-edn
  [state done]
  (let [{[c :as cs] :calls
         callbacks :callbacks} @state]
    (assert-successful-post cs callbacks)

    (is (= "application/edn"
           (get-in (second c) [:headers "Content-Type"]))
        "The POST content-type header is application/edn")
    (is (= "application/edn"
           (get-in (second c) [:headers "Accept"]))
        "The POST Accept header is application/edn")

    (done)))

(deftest post-a-resource-with-edn
  (async
    done
    (let [state (atom blank-state)
          t (delay (assert-successful-post-with-edn state done))]
      (with-redefs [http/post (mock-request state
                                            {:status 200
                                             :body {:name "Albert"}})]
        (let [res (invoke-post "https://myapp.com/things"
                               {:user/name "Albert"}
                               state
                               :encoding :edn)]
          (is (satisfies? Channel res)
              "An async channel is returned")
          (a/go (a/<! res)
                (deref t)))))))

(defn- assert-error-post
  [state done]
  (let [{[c :as cs] :calls
         callbacks :callbacks} @state]
    (is (= 1 (count cs))
        "cljs-http/post is called one time")
    (is (= "https://myapp.com/things"
           (first c))
        "The URI is the 1st arg passed to cljs-http/get")

    (is (= 1 (:callback callbacks))
        "The :callback callback is invoked")

    (let [[e :as es] (:on-failure callbacks)]
      (is (= 1 (count es))
          "The :on-failure callback is invoked once")
      (is (= "Not found" (ex-message e))
          "The :on-failure callback is invoked with the message from the body"))

    (is (= 0 (count (:on-success callbacks)))
        "The :on-success callback is not invoked")

    (done)))

(deftest post-a-resource-and-get-an-error
  (async
    done
    (let [state (atom blank-state)
          t (delay (assert-error-post state done))]
      (with-redefs [http/post (mock-request state
                                            {:status 404
                                             :body {:message "not found"}})]
        (let [res (invoke-post "https://myapp.com/things" {:name "Albert"} state)]
          (is (satisfies? Channel res)
              "An async channel is returned")
          (a/go (a/<! res)
                (deref t)))))))

(defn- assert-invalid-post
  [state done]
  (let [{[c :as cs] :calls
         callbacks :callbacks} @state]
    (is (= 1 (count cs))
        "cljs-http/post is called one time")
    (is (= "https://myapp.com/things"
           (first c))
        "The URI is the 1st arg passed to cljs-http/get")

    (is (= 1 (:callback callbacks))
        "The :callback callback is invoked")

    (let [[e :as es] (:on-failure callbacks)]
      (is (= 1 (count es))
          "The :on-failure callback is invoked once")
      (is (= "Unprocessable entity"
             (ex-message e))
          "The :on-failure callback is invoked with a message about the error")
      (is (= {:errors {:name ["Name is already in use"]}}
             (ex-data e))
          "The :on-failure callback is invoked with data from the body"))

    (is (= 0 (count (:on-success callbacks)))
        "The :on-success callback is not invoked")

    (done)))

(deftest post-a-resource-and-get-an-validation-error
  (async
    done
    (let [state (atom blank-state)
          t (delay (assert-invalid-post state done))]
      (with-redefs [http/post (mock-request state
                                            {:status 422
                                             :body {:errors {:name ["Name is already in use"]}}})]
        (let [res (invoke-post "https://myapp.com/things" {:name "Albert"} state)]
          (is (satisfies? Channel res)
              "An async channel is returned")
          (a/go (a/<! res)
                (deref t)))))))
