(ns dgknght.app-lib.api-async
  (:refer-clojure :exclude [get])
  (:require [cljs-http.client :as http]
            [cljs.core.async :as a]
            [dgknght.app-lib.api :as api]))

(defn- extract-error
  [m]
  (if (:success m)
    m
    (let [error (try
                  (api/extract-error m)
                  (catch js/Error _e
                    "Unable to parse the body as JSON"))]
      (assoc m ::error error))))

(defn- extract-res-body
  [{:keys [body] :as res}]
  (or body res)) ; ensure we don't put nil on the channel

(defn- throw-on-non-success
  [m]
  (when-let [error (::error m)]
    (throw (js/Error. error)))
  m)

(defn- build-xf
  [{:keys [transform handle-ex extract-body]}]
  (apply comp
         (cond-> [(map extract-error)]
           handle-ex                      (conj (map throw-on-non-success))
           (#{true :before} extract-body) (conj (map extract-res-body))
           (sequential? transform)        (concat transform)
           transform                      (conj transform)
           (= :after extract-body)        (conj (map extract-res-body)))))

(defn request
  [options]
  {:pre [(map? options)]}

  (let [opts (merge @api/defaults options)]
    (assoc opts :channel (a/chan 1
                                 (build-xf opts)
                                 (:handle-ex opts)))))

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

(defn path
  [& segments]
  (apply api/path segments))

(defn multipart-params
  [req params]
  (assoc req :multipart-params params))

(defn apply-fn
  "Given a function that accepts a single argument, returns a transducing
  function that applies the given function either to each value as a whole,
  or each element of each values if the value is sequential.

  This would be appropriate for use with option {:extract-body :before}"
  [f]
  (fn [xf]
    (completing
      (fn [ch v]
        (xf ch
            (if (sequential? v)
              (map f v)
              (f v)))))))

(defn apply-body-fn
  "Given a function that accepts a single argument, returns a transducing
  function that applies the given function either to the body of a
  response map or to each element in the body of the response map if the
  body is sequential.

  This would be appropriate for option {:extract-body :after} or without
  the :extract-body option."
  [f]
  (fn [xf]
    (completing
      (fn [ch res]
        (xf ch
            (update-in res
                       [:body]
                       (fn [b]
                         (if (sequential? b)
                           (map f b)
                           (f b)))))))))
