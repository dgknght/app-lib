(ns dgknght.app-lib.api-3
  (:refer-clojure :exclude [get])
  (:require [cljs.core.async :as a]
            [cljs.pprint :refer [pprint]]
            [cljs-http.client :as http]
            [dgknght.app-lib.api :as og]))

(defrecord Error [message data])

(def default-opts
  {:headers {"Content-Type" "application/json"
             "Accept" "application/json"}})

(def path og/path)

(defn- singular?
  [v]
  (and v
       (not (sequential? v))))

(defn- pluralize
  [x]
  (if x
    (if (singular? x)
      [x]
      x)
    []))

(def ^:private status-msgs
  {404 "Not found"
   403 "Forbidden"
   401 "Unauthorized"
   422 "Unprocessable entity"
   500 "Server error"})

(defn- extract-msg
  [{:keys [body status]}]
  (or (:message body)
      (:error body)
      (status-msgs status)
      "Unknown"))

(defn- handle-non-success-status
  [{:keys [status] :as res}]
  (case status
    (<= 200 status 299) res
    422                 (throw (ex-info "Unprocessable entity" (:body res)))
    (throw (ex-info (extract-msg res) {}))))

(defn- build-xf
  [{:keys [pre-xf post-xf]}]
  (apply comp
         (concat (pluralize pre-xf)
                 [(map handle-non-success-status)
                  (map #(or (:body %) %))]
                 (pluralize post-xf))))

(defn- ex-handler
  [{:keys [on-error]}]
  (fn [e]
    (if on-error
      (on-error e)
      (->Error (ex-message e) (ex-data e)))))

(defn- build-chan
  [opts]
  (a/chan 1 (build-xf opts) (ex-handler opts)))

(defn- wait-and-callback
  [ch {:keys [on-success
              on-failure
              callback]
       :or {on-success identity
            on-failure identity
            callback identity}}]
  (a/go
    (let [res (a/<! ch)]
      (callback)
      (if (instance? Error res)
        (on-failure res)
        (on-success res)))))

(defn- build-req
  [opts]
  (-> default-opts
      (merge opts)
      (update-in [:channel] #(or % (build-chan opts)))))

(defn get
  ([uri] (get uri {}))
  ([uri opts]
   (wait-and-callback
     (http/get uri (build-req opts))
     opts)))

(defn post
  ([uri resource] (post uri resource {}))
  ([uri resource opts]
   (wait-and-callback
     (http/post uri (assoc (build-req opts) :json-params resource))
     opts)))

(defn patch
  ([uri resource] (patch uri resource {}))
  ([uri resource opts]
   (wait-and-callback
     (http/patch uri (assoc (build-req opts) :json-params resource))
     opts)))

(defn delete
  ([uri] (delete uri {}))
  ([uri opts]
   (wait-and-callback
     (http/delete uri (build-req opts))
     opts)))
