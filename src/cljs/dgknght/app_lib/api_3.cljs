(ns dgknght.app-lib.api-3
  (:refer-clojure :exclude [get])
  (:require [cljs.core.async :as a]
            [cljs.pprint :refer [pprint]]
            [cljs-http.client :as http]
            [dgknght.app-lib.api :as og]))

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
   401 "Unauthorized"
   403 "Forbidden"
   404 "Not found"
   405 "Method not allowed"
   422 "Unprocessable entity"
   500 "Server error"})

(defn- handle-non-success-status
  [{:keys [status] :as res}]
  (if (<= 200 status 299)
    res
    (throw
      (ex-info (status-msgs status "Unknown")
               (:body res)))))

(defn- build-xf
  [{:keys [pre-xf post-xf]}]
  (apply comp
         (concat (pluralize pre-xf)
                 [(map handle-non-success-status)
                  (map #(or (:body %) %))]
                 (pluralize post-xf))))

(defn- log-and-return-error
  [e]
  (.error js/console "Unhanlded API error" e)
  e)

(defn- build-chan
  [{:as opts :keys [on-error] :or {on-error log-and-return-error}}]
  (a/chan 1 (build-xf opts) on-error))

(def ^:private error?
  (partial instance? js/Error))

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
      (if (error? res)
        (on-failure res)
        (on-success res)))))

(defn- infer-param-key
  [{:keys [encoding]
    :or {encoding :json}}]
  (keyword (str (name encoding) "-params")))

(defn- build-req
  ([opts] (build-req nil opts))
  ([resource opts]
   (cond-> (-> opts
               (dissoc :encoding)
               (update-in [:channel] #(or % (build-chan opts))))
     resource (assoc (infer-param-key opts) resource))))

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
