(ns dgknght.app-lib.notifications
  (:require [cljs.core.async :as a]
            [reagent.core :as r]
            [goog.string :as gstr]
            [cljs-time.core :as t]))

(defonce notifications (r/atom []))
(defonce toasts (r/atom []))

(def ^:const toast-delay 2000)
(def ^:const toast-sweep-interval 2500)

(defn notify
  ([message] (notify message :info))
  ([message severity]
   (let [notification {:message message
                       :severity severity
                       :id (random-uuid)}]
     (swap! notifications #(conj % notification)))))

(defn success [message] (notify message :success))
(defn info    [message] (notify message :info))
(defn warning [message] (notify message :warning))
(defn warn    [message] (notify message :warning))
(defn danger  [message] (notify message :danger))

(defn successf
  [message & args]
  (notify (apply gstr/format message args) :success))

(defn infof
  [message & args]
  (notify (apply gstr/format message args) :info))

(defn warningf
  [message & args]
  (notify (apply gstr/format message args) :warning))

(defn warnf
  [message & args]
  (notify (apply gstr/format message args) :warning))

(defn dangerf
  [message & args]
  (notify (apply gstr/format message args) :danger))

(defn danger-fn
  [msg-format]
  (fn [msg]
    (danger (gstr/format msg-format (or (:message msg) msg)))))

(defn unnotify
  [notification]
  (swap! notifications (fn [notifications]
                         (remove #(= (:id %) (:id notification))
                                 notifications))))

(declare sweep-toasts)
(defn queue-toast-sweep []
  (a/go
    (a/<! (a/timeout toast-sweep-interval))
    (sweep-toasts)))

(defn toast
  ([body] (toast nil body))
  ([title body]
   (swap! toasts conj (cond-> {:body body
                               :expires-at (t/from-now (t/seconds 2))
                               :id (str (random-uuid))}
                        title (assoc :title title)))
   (queue-toast-sweep)))

(defn toastf
  [title body-pattern & args]
  (toast title (apply gstr/format body-pattern args)))

(defn untoast
  [id]
  (swap! toasts (fn [toasts]
                  (remove #(= id (:id %)) toasts))))

(defn- expired-toast?
  [{:keys [expires-at]}]
  (t/after? (t/now) expires-at))

(defn sweep-toasts
  ([]
   (swap! toasts sweep-toasts))
  ([toasts]
   (let [result (remove expired-toast? toasts)]
     (when (seq result)
       (queue-toast-sweep))
     result)))
