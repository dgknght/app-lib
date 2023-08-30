(ns dgknght.app-lib.api-3
  (:refer-clojure :exclude [get])
  (:require [cljs.core.async :as a]
            [cljs-http.client :as http]
            [dgknght.app-lib.api :as og]))

(def default-opts
  {:headers {"Content-Type" "application/json"
             "Accept" "application-json"}})

(def path og/path)

(defn- singular?
  [v]
  (and v
       (not (sequential? v))))

(defn- pluralize
  [x]
  (when x
    (if (singular? x)
      [x]
      x)))

(defn- build-xf
  [{:keys [pre-xf post-xf]}]
  (->> [(pluralize pre-xf)
        (map #(or (:body %) %))
        (pluralize post-xf)]
       (filter identity)
       (apply comp)))

(defn- build-chan
  [opts]
  (a/chan 1 (build-xf opts)))

(defn- wait-and-callback
  [ch {:keys [callback]
       :or {callback identity}}]
  (a/go
    (let [res (a/<! ch)]
      (callback res))))

(defn get
  [uri & {:as opts}]
  (wait-and-callback
    (http/get uri (-> default-opts
                      (merge opts)
                      (update-in [:channel] #(when-not %
                                               (build-chan opts)))))
    opts))
