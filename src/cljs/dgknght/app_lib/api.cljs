(ns dgknght.app-lib.api
  (:refer-clojure :exclude [get])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.string :as string]
            [cljs.core.async :refer [<!]]
            [cljs-http.client :as http]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [lambdaisland.uri :refer [map->query-string]]
            [goog.string :as gstr]
            [dgknght.app-lib.core :refer [present?]]
            [dgknght.app-lib.web :as web]))

(defonce defaults (atom {}))

(defn- success?
  [{:keys [status]}]
  (<= 200 status 299))

(defn- log-failure
  [response action model]
  (.error js/console
          (gstr/format
            "Unable to %s the model %s: %s"
            action
            (prn-str model)
            (prn-str response)))
  response)

(defn path
  [& segments]
  (apply web/path (concat [:api] segments)))

(defn append-query-string
  [path criteria]
  (if (empty? criteria)
    path
    (let [query-string (map->query-string criteria)]
      (str path "?" query-string))))

(defn header
  [req k v]
  (update-in (or req {}) [:headers] assoc k v))

(defn content-type
  [req content-type]
  (header req "Content-Type" content-type))

(defn accept
  [req content-type]
  (header req "Accept" content-type))

(defn json-params
  [req params]
  (assoc req :json-params params))

(defn multipart-params
  [req params]
  (assoc req :multipart-params params))

(defn request
  ([] (request {}))
  ([options]
   (-> {}
       (content-type "application/json")
       (accept "application/json")
       (merge @defaults options))))

(defn parse-json
  [json]
  (transform-keys ->kebab-case-keyword
                  (js->clj (.parse js/JSON json))))

(def ^:private error-map
  {404 "not found"
   403 "forbidden"
   401 "unauthorized"
   400 "bad request"
   500 "server error"})

(defn extract-error
  [{:keys [body status]}]
  (if-let [parsed (when (present? body)
                    (if (string? body)
                      (parse-json body)
                      body))]
    (if-let [val-errors (:dgknght.app-lib.validation/errors parsed)]
      (string/join ", " (-> val-errors vals flatten))
      (or (some #(% parsed) [:error
                             :message])
          body))
    (get-in error-map [status] (str "Unknown error: " status))))

(defn- respond
  [response action path success-fn error-fn]
  (if (success? response)
    (-> response :body success-fn)
    (-> response
        (log-failure action path)
        extract-error
        error-fn)))

(defn get
  ([path success-fn error-fn]
   (get path {} success-fn error-fn))
  ([path criteria success-fn error-fn]
   (get path criteria {} success-fn error-fn))
  ([path criteria options success-fn error-fn]
   (go (let [req (request options)
             response (<! (http/get (append-query-string path criteria)
                                    req))]
         (respond response :get path success-fn error-fn)))))

(defn post
  ([path success-fn error-fn] (post path {} success-fn error-fn))
  ([path model success-fn error-fn] (post path model {} success-fn error-fn))
  ([path model options success-fn error-fn]
   (go (let [response (<! (http/post path (-> options
                                              request
                                              (json-params model))))]
         (respond response :post path success-fn error-fn)))))

(defn post-files
  "Post files to the server. The files can be extracted from a drop event like this:

    (defn- handle-drop
      [e page-state]
      (.preventDefault e)
      (let [file-list (aget e \"dataTransfer\" \"files\")
            file-count (aget file-list \"length\")
            files (mapv #(.item file-list %) (range file-count))]
        (swap! page-state update-in [:invitations :import-data :files] (fnil concat []) files)))"
  ([path content success-fn error-fn]
   (post-files path content {} success-fn error-fn))
  ([uri content options success-fn error-fn]
   (go (let [{:keys [success body]
              :as response} (<! (http/post uri (request (merge options {:multipart-params content}))))]
         (if success
           (success-fn body)
           (error-fn (extract-error response)))))))

(defn patch
  ([path model success-fn error-fn]
   (patch path model {} success-fn error-fn))
  ([path model options success-fn error-fn]
   (go (let [response (<! (http/patch path (-> options
                                               request
                                               (json-params model))))]
         (respond response :patch path success-fn error-fn)))))

(defn delete
  ([path success-fn error-fn]
   (delete path {} success-fn error-fn))
  ([path options success-fn error-fn]
   (go (let [response (<! (http/delete path (request options)))]
         (if (success? response)
           (success-fn)
           (-> response
               (log-failure "delete" path)
               extract-error
               error-fn))))))
