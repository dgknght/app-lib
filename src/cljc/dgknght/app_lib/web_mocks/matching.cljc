(ns dgknght.app-lib.web-mocks.matching
  (:require [dgknght.app-lib.core :as lib])
  #?(:clj (:import java.util.regex.Pattern)))

#?(:clj
   (defn- regexp?
     [v]
     (instance? Pattern v)))

(defn- dispatch-matcher
  [matcher]
  (cond
    (regexp? matcher)  :pattern
    (string? matcher)  :string
    (map? matcher)     :map
    (keyword? matcher) :keyword
    (ifn? matcher)     :fn))

(defmulti match-value?
  (fn [_ matcher]
    (dispatch-matcher matcher)))

(defmethod match-value? :default
  [value matcher]
  (= value matcher))

(defmethod match-value? :pattern
  [value matcher]
  (re-find matcher value))

(defmethod match-value? :fn
  [value matcher]
  (matcher value))

(defmulti match? (fn [_req matcher]
                   (dispatch-matcher matcher)))

(defmethod match? :string
  [{:keys [url]} matcher]
  (= url matcher))

(defmethod match? :pattern
  [req pattern]
  (re-find pattern (:url req)))

(defmethod match? :map
  [req matcher]
  (->> matcher
       (map (fn [[k v]]
              (match-value? (get-in req [k])
                            v)))
       (every? identity)))

(defmethod match? :fn
  [req f]
  (f req))

(defmulti meets-spec?
  (fn [_match-count spec]
    (cond
      (keyword? spec) ::keyword
      (map? spec) ::map)))

(defmethod meets-spec? ::map
  [match-count {:keys [times]}]
  (= times match-count))

(defmethod meets-spec? ::keyword
  [match-count spec]
  (case spec
    :once (= 1 match-count)
    :twice (= 2 match-count)
    (throw (ex-info (lib/format "Unrecognized call spec %s" spec) {:spec spec}))) )

(defmulti readable dispatch-matcher)

(defmethod readable :pattern
  [matcher]
  (str "request with url matching \"" matcher "\""))

(defmethod readable :string
  [matcher]
  (str "request with url \"" matcher "\""))

(defmethod readable :map
  [matcher]
  (str "request with attributes " (prn-str matcher)))

(defmethod readable :fn
  [_matcher]
  "request matching predicate fn")
