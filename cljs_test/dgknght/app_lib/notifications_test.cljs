(ns dgknght.app-lib.notifications-test
  (:require [cljs.test :refer-macros [deftest is use-fixtures]]
            [dgknght.app-lib.notifications :as notify]))

(use-fixtures :each {:before (fn []
                               (reset! notify/notifications [])
                               (reset! notify/toasts []))})

(deftest add-a-notification
  (notify/notify "Done!")
  (let [notification (first @notify/notifications)]
    (is (= "Done!" (:message notification)) "The notification has the correct message")
    (is (= :info (:severity notification)) "The notification has the correct severity")
    (is (:id notification) "The notification has an id")))

(deftest add-success-notification
  (notify/success "Done!")
  (let [notification (first @notify/notifications)]
    (is (= "Done!" (:message notification)) "The notification has the correct message")
    (is (= :success (:severity notification)) "The notification has the correct severity")
    (is (:id notification) "The notification has an id")))

(deftest add-warning-notification
  (notify/warn "Not done!")
  (let [notification (first @notify/notifications)]
    (is (= "Not done!" (:message notification)) "The notification has the correct message")
    (is (= :warning (:severity notification)) "The notification has the correct severity")
    (is (:id notification) "The notification has an id")))

(deftest add-danger-notification
  (notify/danger "Not done!")
  (let [notification (first @notify/notifications)]
    (is (= "Not done!" (:message notification)) "The notification has the correct message")
    (is (= :danger (:severity notification)) "The notification has the correct severity")
    (is (:id notification) "The notification has an id")))

(deftest create-a-notifying-fn
  (let [f (notify/danger-fn "Operation failed: %s")
        _ (f "Unknown error")
        notification (first @notify/notifications)]
    (is (= "Operation failed: Unknown error" (:message notification)) "The notification has the correct message")
    (is (= :danger (:severity notification)) "The notification has the correct severity")
    (is (:id notification) "The notification has an id")))

(deftest clear-a-notification
  (notify/info "This is a test")
  (is (= 1 (count @notify/notifications)))
  (notify/unnotify (first @notify/notifications))
  (is (empty? @notify/notifications)))

(deftest create-a-toast
  (let [sweeps (atom 0)]
  (with-redefs [notify/queue-toast-sweep (fn [] (swap! sweeps inc))]
    (notify/toast "Success!" "The operation completed successfully.")
    (let [toast (first @notify/toasts)]
      (is (= "Success!" (:title toast)) "The title is set correctly.")
      (is (= "The operation completed successfully." (:body toast)) "The body is set correctly.")
      (is (:id toast) "And id is set")
      (is (:expires-at toast) "An expiration is set")
      (is (= 1 @sweeps) "A sweep of toasts is queued")))))
