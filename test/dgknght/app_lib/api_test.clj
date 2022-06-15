(ns dgknght.app-lib.api-test
  (:require [clojure.test :refer [deftest is]]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.validation :as v]
            [dgknght.app-lib.api :as api]))

(deftest create-a-response-for-a-create-action
  (is (comparable? {:status 201
                    :body {:name "John Doe"}}
                   (api/creation-response {:name "John Doe"}))
      "A valid model gets 201 status")
  (is (comparable? {:status 400
                    :body {:name "John Doe"
                           ::v/errors ["age is required"]}}
                   (api/creation-response {:name "John Doe"
                                           ::v/errors ["age is required"]}))
      "An invalid model gets 400 status"))

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
