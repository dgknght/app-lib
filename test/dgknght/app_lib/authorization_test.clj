(ns dgknght.app-lib.authorization-test
  (:require [clojure.test :refer [deftest is]]
            [stowaway.core :as storage]
            [dgknght.app-lib.authorization :as auth]))

(def model (storage/tag {} ::user))

(def admin-user {:role :admin :id 101})
(def other-user {:role :user :id 201})

(defmethod auth/allowed? [::user ::auth/manage]
  [_model _action user]
  (= :admin (:role user)))

(defmethod auth/scope ::user
  [_model user]
  (when-not (= :admin (:role user))
    {:parent-id (:id user)}))

(deftest check-for-permission
  (is (auth/allowed? model ::auth/update admin-user)
      "Yields true if the user has permission")
  (is (not (auth/allowed? model ::auth/update other-user))
      "Yields false if the user does not have permission")
  (is (thrown-with-msg?  clojure.lang.ExceptionInfo
                        #"No authorization rules defined"
                        (auth/allowed? (storage/tag {} ::order)
                                       ::auth/update
                                       admin-user))
      "If no authorization rules are defined for the model type, an exception is thrown")
  (is (auth/allowed? ::user ::auth/index admin-user)
      "Permission may be checked by tag instead of model"))

(deftest authorize-an-action
  (is (auth/authorize model ::auth/update admin-user)
      "No exception is thrown if the user is authorized")
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"^not found$"
                        (auth/authorize model ::auth/update other-user))
      "An exception is thrown if the user is not authorized"))

(deftest add-scope-to-a-query
  (is (= [:and
          {:name "Fred"}
          {:parent-id 201}]
         (auth/+scope (storage/tag {:name "Fred"} ::user)
                      other-user))
      "The criteria is narrowed based on the user")
  (is (= {:parent-id 201}
         (auth/+scope (storage/tag {} ::user)
                      other-user))
      "The scope is returned directly if the criteria is empty")
  (is (= {:name "Fred"}
         (auth/+scope (storage/tag {:name "Fred"} ::user)
                      admin-user))
      "The criteria is returned directly if the scope is nil")
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"No scope defined"
                        (auth/+scope (storage/tag {:name "Fred"} ::customer)
                                     admin-user))
      "An exception is thrown if no scope fn is defined"))
