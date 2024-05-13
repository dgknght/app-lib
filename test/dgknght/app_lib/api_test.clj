(ns dgknght.app-lib.api-test
  (:require [clojure.test :refer [deftest is testing]]
            [dgknght.app-lib.test :refer [with-log-capture]]
            [dgknght.app-lib.authorization :as auth]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.validation :as v]
            [dgknght.app-lib.api :as api]))

(deftest create-a-response-for-a-create-action
  (is (comparable? {:status 201
                    :body {:name "John Doe"}}
                   (api/creation-response {:name "John Doe"}))
      "A valid model gets 201 status")
  (testing "an invalid model"
    (with-log-capture [logged]
      (let [result (api/creation-response (with-meta
                                            {:name "John Doe"
                                             :ssn "do not log this"
                                             ::v/errors {:age ["age is required"]}}
                                            {:model-type :user}))]
        (is (comparable? {:status 400
                          :body {:name "John Doe"
                                 ::v/errors {:age ["age is required"]}}}
                         result)
            "An invalid model gets 400 status")
        (let [[m :as ms] @logged]
          (is (= 1 (count ms))
              "One message is logged")
          (is (= [:info
                  nil
                  "Unable to save the model {:model-type :user}: age is required"]
                 (rest m))
              "The validation failures are logged"))))))

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

(defn- throwing-handler
  [ex]
  (api/wrap-api-exception (fn [_req]
                            (throw ex))))

(deftest handle-request-errrors
  (testing "validation error"
    (with-log-capture [logged]
      (let [h (throwing-handler
                (ex-info "The model is invalid"
                         {::v/errors {:name ["Name is required"]}}))
            res (h {})]
        (is (= 422 (:status res))
            "The response indicates unprocessable entity")
        (is (= 0 (count @logged))
            "Nothing is logged"))))
  (testing "unexpected error"
    (with-log-capture [logged]
      (let [h (throwing-handler (ex-info "induced error" {:my :data}))
            res (h {})]
        (is (= 500 (:status res))
            "The response indicates an internal error")
        (let [[[_ severity] :as ls] @logged]
          (is (= 1 (count ls))
              "One message is logged")
          (is (= :error severity)
              "The error details are logged")))))
  (testing "extra unexpected error"
    (with-log-capture [logged]
      (let [h (throwing-handler (RuntimeException. "lower level java error"))
            res (h {})]
        (is (= 500 (:status res))
            "The response indicates an internal error")
        (let [[[_ severity] :as ls] @logged]
          (is (= 1 (count ls))
              "One message is logged")
          (is (= :error severity)
              "The error details are logged")))))
  (testing "opaque error"
    (with-log-capture [logged]
      (let [h (throwing-handler (ex-info "induced error" {::auth/opaque? true}))
            res (h {})]
        (is (= 404 (:status res))
            "The response indicates not found")
        (is (empty? @logged)
            "Nothing is logged"))))
  (testing "non-opaque error"
    (with-log-capture [logged]
      (let [h (throwing-handler (ex-info "induced error" {::auth/opaque? false}))
            res (h {})]
        (is (= 403 (:status res))
            "The response indicates forbidden")
        (is (empty? @logged)
            "Nothing is logged")))))
