(ns dgknght.app-lib.authorization-test
  (:require [clojure.test :refer [deftest is]]
            [stowaway.core :as storage]
            [dgknght.app-lib.authorization :as auth]))

(def model (storage/tag {} ::user))

(def user {:role :admin :id 101})
(def other-user {:role :user :id 201})

(defmethod auth/allowed? [::user ::auth/manage]
  [_model _action user]
  (= :admin (:role user)))

(defmethod auth/scope ::user
  [_model user]
  {:parent-id (:id user)})

(deftest check-for-permission
  (is (auth/allowed? model ::auth/update user)
      "Yields true if the user has permission")
  (is (not (auth/allowed? model ::auth/update other-user))
      "Yields false if the user does not have permission")
  (is (thrown?
        java.lang.RuntimeException
        (auth/allowed? model ::auth/update (storage/tag {} ::order)))))

(deftest authorize-an-action
  (is (auth/authorize model ::auth/update user)
      "No exception is thrown if the user is authorized")
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"^not found$"
                        (auth/authorize model ::auth/update other-user))
      "An exception is thrown if the user is not authorized"))

(deftest add-scope-to-a-query
  (is (= [:and
          {:name "Fred"}
          {:parent-id 101}]
         (auth/+scope (storage/tag {:name "Fred"} ::user) user))
      "The criteria is narrowed based on the user"))
