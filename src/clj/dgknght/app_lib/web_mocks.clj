(ns dgknght.app-lib.web-mocks
  (:require [clojure.test :refer [assert-expr do-report]]
            [clojure.set :refer [subset?]]
            [clj-http.core :as http]
            [lambdaisland.uri :as uri]
            [dgknght.app-lib.core :refer [safe-nth]]))

(defn- dispatch-matcher
  [matcher]
  (cond
    (string? matcher) :url
    (instance? java.util.regex.Pattern matcher) :pattern
    (map? matcher) :map
    (ifn? matcher) :fn))

(defmulti match? (fn [_req matcher]
                   (dispatch-matcher matcher)))

(defmethod match? :url
  [req url]
  (= (:url req) url))

(defmethod match? :pattern
  [req pattern]
  (re-find pattern (:url req)))

(defmethod match? :fn
  [req f]
  (f req))

(defn- parse-query-string
  [{:keys [query-string] :as m}]
  (if query-string
    (assoc m :query-params (uri/query-string->map query-string))
    m))

(defmulti match-value?
  (fn [_actual matcher]
    (dispatch-matcher matcher)))

(defmethod match-value? :default
  [actual matcher]
  (= matcher actual))

(defmethod match-value? :pattern
  [actual pattern]
  (re-find pattern actual))

(defmethod match-value? :map
  [actual m]
  (subset? (set m) (set actual)))

(defmethod match? :map
  [req m]
  (every? (fn [[k v]]
            (match-value? v (req k)))
          m))

(defn request-handler
  [calls mocks-map]
  (fn [request]
    (let [req (parse-query-string request)]
      (or (some (fn [[k f]]
                  (when (match? req k)
                    (swap! calls conj req)
                    (f request)))
                mocks-map)
          (throw (Exception. (format "Unable to match request: %s"
                                     req)))))))

(defmacro with-web-mocks
  [bindings mocks-map & body]
  `(let [f# (fn* [~(first bindings)] ~@body)
         calls# (atom [])]
     (with-redefs [http/request (request-handler calls# ~mocks-map)]
       (f# calls#))))

(defmulti meets-spec?
  (fn [_match-count spec]
    (type spec)))

(defmethod meets-spec? clojure.lang.PersistentArrayMap
  [match-count {:keys [times]}]
  (= times match-count))

(defmethod meets-spec? clojure.lang.Keyword
  [match-count spec]
  (case spec
    :once (= 1 match-count)
    (throw (IllegalArgumentException. (format "Unrecognized call spec %s", spec)))) )

(defmethod assert-expr 'called?
  [msg form]
  (let [spec (safe-nth form 1)
        calls (safe-nth form 2)
        matcher (safe-nth form 3)]
    `(let [matches# (filter #(match? % ~matcher) (deref ~calls))
           pass?# (meets-spec? (count matches#) ~spec)]
       (do-report {:expected ~spec
                   :actual (count matches#)
                   :message ~msg
                   :type (if pass?# :pass :fail)}))))

(defmethod assert-expr 'not-called?
  [msg form]
  (let [calls (safe-nth form 1)
        matcher (safe-nth form 2)]
    `(let [matches# (filter #(match? % ~matcher) (deref ~calls))
           pass?# (empty? matches#)]
       (do-report {:expected "no matching calls"
                   :actual matches#
                   :message ~msg
                   :type (if pass?# :pass :fail)}))))
