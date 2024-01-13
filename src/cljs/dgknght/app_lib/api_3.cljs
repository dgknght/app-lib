(ns dgknght.app-lib.api-3
  (:refer-clojure :exclude [get])
  (:require [cljs.core.async :as a]
            [cljs-http.client :as http]
            [dgknght.app-lib.api :as og]))

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

(defn- handle-non-success-status
  [{:keys [status] :as res}]
  (if (<= 200 status 299)
    res
    (throw (ex-info "non success response" {:response res}))))

(defn- build-xf
  [{:keys [pre-xf post-xf]}]
  (apply comp
         (concat (pluralize pre-xf)
                 [(map handle-non-success-status)
                  (map #(or (:body %) %))]
                 (pluralize post-xf))))

(defn- error
  [x]
  (vary-meta x assoc ::error true))

(defn- error?
  [x]
  (-> x meta ::error))

(defn- error-fn
  [{:keys [callback
           on-error]
    :or {callback identity
         on-error identity}}]
  (fn [e]
    (callback)
    (error (on-error e))))

(defn- build-chan
  [opts]
  (a/chan 1 (build-xf opts) (error-fn opts)))

(defn- wait-and-callback
  [ch {:keys [on-success
              callback
              on-failure]
       :or {on-success identity
            callback identity
            on-failure identity}}]
  (a/go
    (let [res (a/<! ch)]
      (callback)
      ; TODO: If the caller supplies :on-error, how do we know not to call on-success?
      (if (error? res)
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
