(ns dgknght.app-lib.api-test
  (:require [clojure.test :refer [deftest is #_testing]]
            [clojure.tools.logging :as log]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.validation :as v]
            [dgknght.app-lib.api :as api]))

(deftest create-a-response-for-a-create-action
  (is (comparable? {:status 201
                    :body {:name "John Doe"}}
                   (api/creation-response {:name "John Doe"}))
      "A valid model gets 201 status")
  #_(testing "an invalid model"
    (let [logged (atom [])]
      (with-redefs [logging/log* (fn [args]
                                   (swap! logged conj args))]
        (is (comparable? {:status 400
                          :body {:name "John Doe"
                                 ::v/errors {:age ["age is required"]}}}
                         (api/creation-response {:name "John Doe"
                                                 ::v/errors {:age ["age is required"]}}))
            "An invalid model gets 400 status")
        (let [[m & ms] @logged]
          (is (= 1 (count ms))
              "One message is logged")
          (is (= [[:debug]]
                 m)
              "The error details are logged"))))))

(deftest create-a-response-for-an-update-action
  (is (comparable? {:status 200
                    :body {:name "John Doe"}}
                   (api/update-response {:name "John Doe"}))
      "A valid model gets 200 status")
  (is (comparable? {:status 400
                    :body {:name "John Doe"
                           ::v/errors ["age is required"]}}
                   (api/update-response {:name "John Doe"
                                         ::v/errors ["age is required"]}))
      "An invalid model gets 400 status"))

(deftest create-a-no-content-response
  (is (comparable? {:status 204
                    :body nil}
                   (api/response))))

(deftest authenticate-a-request
  (let [passed-req (atom nil)
        handler (fn [req]
                  (reset! passed-req req)
                  {:status 200
                   :body "OK"})
        wrapped (api/wrap-authentication
                  handler
                  {:authenticate-fn (fn [req]
                                      (when (= "Basic abc"
                                               (get-in req [:headers "Authorization"]))
                                        {:name "John Doe"}))})]
    (let [res (wrapped {:headers {"Authorization" "Basic abc"}})]
      (is (http-success? res)
          "A request with valid Authorization succeeds")
      (is (= {:name "John Doe"}
             (:authenticated @passed-req))
          "The authenticated entity is added to the request map"))
    (is (http-unauthorized? (wrapped {}))
        "A request without Authorization is unauthorized")
    (is (http-unauthorized? (wrapped {:headers {"Authorization" "Basic def"}}))
        "A request witn invalid Authorization is unauthorized")))

(deftest handle-request-errrors
  (let [logged (atom [])
        handler (fn [_req]
                  (throw (ex-info "induced error" {:my :data})))
        wrapped (api/wrap-api-exception handler)
        res (with-redefs [log/log* (fn [& args]
                                     (swap! logged conj args))]
              (wrapped {}))]
    (is (= 500 (:status res))
        "The response indicates an internal error")
    (is (= :error
           (second (first @logged)))
        "The error details are logged")))
