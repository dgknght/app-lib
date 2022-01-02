(ns dgknght.app-lib.api-async
  (:refer-clojure :exclude [get])
  (:require [cljs-http.client :as http]
            [cljs.core.async :as a]
            [dgknght.app-lib.api :as api]))

(def ^:private extract-error
  (map #(if (:success %)
          %
          (assoc % ::error (api/extract-error %)))))

(def ^:private throw-on-non-success
  (map #(if (:success %)
          %
          (throw (js/Error. (api/extract-error %))))))

(defn- build-xf
  [{:keys [transform handle-ex]}]
  (let [err-xf (if handle-ex
                 throw-on-non-success
                 extract-error)
        custom (cond
                 (nil? transform) []
                 (sequential? transform) transform
                 :else [transform])]
    (apply comp err-xf (concat custom))))

(defn request
  [options]
  {:pre [(map? options)]}

  (-> @api/defaults
      (merge options)
      (assoc :channel (a/chan 1
                              (build-xf options)
                              (:handle-ex options)))))

(defn get
  ([path] (get path {}))
  ([path criteria] (get path criteria {}))
  ([path criteria options]
   (http/get (api/append-query-string path criteria)
             (request options))))

(defn post
  ([path resource] (post path resource {}))
  ([path resource options]
   (http/post path (-> options
                       request
                       (assoc :json-params resource)))))

(defn patch
  ([path resource] (patch path resource {}))
  ([path resource options]
   (http/patch path (-> options
                        request
                        (assoc :json-params resource)))))

(defn delete
  ([path] (delete path {}))
  ([path options]
   (http/delete path (request options))))
