(ns dgknght.app-lib.api-3
  (:refer-clojure :exclude [get])
  (:require [cljs.core.async :as a]
            [cljs.pprint :refer [pprint]]
            [cljs-http.client :as http]
            [dgknght.app-lib.api :as og]))

(defrecord Error [message data])

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
  {400 "Bad request"
   404 "Not found"
   403 "Forbidden"
   401 "Unauthorized"
   422 "Unprocessable entity"
   500 "Server error"})

(defn- extract-msg
  [{:keys [body status]}]
  (or (:error body)
      (:message body)
      (status-msgs status)
      "Unknown"))

(defn- handle-non-success-status
  [{:keys [status] :as res}]
  (if (<= 200 status 299)
    res
    (throw
      (case status
        400 (ex-info "Bad request" (:body res))
        422 (ex-info "Unprocessable entity" (:body res))
        (ex-info (extract-msg res) {})))))

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

(defn- resolve-encoding
  [{:keys [encoding]
    :or {encoding :json}}]
  (let [type (name encoding)]
    {:param-key (keyword (str type "-params"))
     :content-type (str "application/" type)}))

(defn- build-req
  ([opts] (build-req nil opts))
  ([resource opts]
   (let [{:keys [param-key content-type]} (resolve-encoding opts)]
     (cond-> (-> opts
                 (dissoc :encoding)
                 (update-in [:channel] #(or % (build-chan opts)))
                 (assoc-in [:headers "Content-Type"] content-type)
                 (assoc-in [:headers "Accept"] content-type))
       resource (assoc param-key resource)))))

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
     (http/post uri (build-req resource opts))
     opts)))

(defn patch
  ([uri resource] (patch uri resource {}))
  ([uri resource opts]
   (wait-and-callback
     (http/patch uri (build-req resource opts))
     opts)))

(defn delete
  ([uri] (delete uri {}))
  ([uri opts]
   (wait-and-callback
     (http/delete uri (build-req opts))
     opts)))
