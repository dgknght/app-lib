(ns dgknght.app-lib.core
  (:refer-clojure :exclude [uuid decimal?])
  (:require [clojure.string :as string]
            [clojure.walk]
            #?(:clj [clojure.core :as cc])
            #?(:clj [clojure.pprint :refer [pprint]])
            #?(:cljs [dgknght.app-lib.decimal :as d]))
  #?(:clj (:import java.util.UUID)))

(defn trace
  [msg]
  #?(:clj (pprint msg)
     :cljs (.log js/console (prn-str msg))))

(def boolean-values #{"true" "1" "y" "yes" "t"})

(defn ensure-string
  [v]
  (if (keyword? v)
    (name v)
    (str v)))

(defn parse-bool
  [value]
  (when value
    (contains? boolean-values (string/lower-case value))))

(defn- parse-int*
  [v]
  #?(:clj (Integer/parseInt v)
         :cljs (js/parseInt v)))

(def parse-int
  (some-fn #(when (integer? %) %)
           #(when (and (string? %)
                       (seq %))
              (parse-int* (string/replace % #"," ""))))) ; TODO: this is specific to US number formatting rules

(defn- parse-float*
  [v]
  #?(:clj (Double/parseDouble v)
     :cljs (js/parseFloat v)))

(def parse-float
  (some-fn #(when (float? %) %)
           #(when (and (string? %)
                       (seq %))
              (parse-float* %))))

(defn- decimal?
  [v]
  #?(:clj (cc/decimal? v)
     :cljs (d/decimal? v)))

(def parse-decimal
  (some-fn #(when (decimal? %) %)
           #(when (and (string? %)
                       (re-find #"^-?\d+(?:\.\d+)?$" %))
              #?(:clj (bigdec %)
                 :cljs (d/->decimal %)))))

(defn assoc-if
  "Performs an assoc if the specified value is not nil."
  [m k v]
  (if v
    (assoc m k v)
    m))

(defn update-in-if
  "Performs an update-in if the key already exists in the map."
  [m k-path f & args]
  (if-let [v (get-in m k-path)]
    (assoc-in m k-path (apply f v args))
    m))

(defn deep-contains?
  "Given a data structure, returns a boolean value indicating whether
  or not the structure contains that specified key at any level of nesting."
  [data k]
  (cond
    (vector? data) (some #(deep-contains? % k) data)
    (map? data)    (contains? data k)
    :else          false))

(defn deep-get
  "Given a data structure, returns the value of the first found
  instance of the specified key."
  [data k]
  (cond
    (vector? data) (some #(deep-get % k) data)
    (map? data)    (get-in data [k])
    :else          nil))

(defn deep-update-in-if
  "Given a data structure, returns the same structure with the
  value at the first instance of the specified key updated with
  the given function."
  [data k f]
  (cond
    (vector? data) (mapv #(deep-update-in-if % k f) data)
    (map? data)    (update-in-if data [k] f)
    :else          data))

(defn deep-dissoc
  "Given a data structure, returns the same structure with the
  first found instance of the specified key removed"
  [data k]
  (cond
    (vector? data) (mapv #(deep-dissoc % k) data)
    (map? data)  (dissoc data k)
    :else data))

#?(:clj (defn uuid
          ([] (UUID/randomUUID))
          ([value]
           (when value
             (if (instance? UUID value)
               value
               (UUID/fromString (str value))))))
      :cljs (defn uuid
              ([] (random-uuid))
              ([value]
               (when value
                 (uuid value)))))

(defn present?
  [v]
  (boolean
    (if (or (string? v)
            (coll? v))
      (seq v)
      v)))

(defn index-by
  [key-fn coll]
  (->> coll
       (map (juxt key-fn identity))
       (into {})))

(declare prune-map)

(defmulti prune-to
  (fn [_target source]
    (cond
      (map? source) :map
      (and (sequential? source)
           (map? (first source))) :sequence)))

(defmethod prune-to :map
  [target source]
  (prune-map target source (keys source)))

(defmethod prune-to :sequence
  [target source]
  (let [ks (set (mapcat keys source))]
    (map-indexed #(prune-map %2 (nth source %1) ks)
         target)))

(defmethod prune-to :default
  [target _source]
  target)

(defn- prune-map
  [target source ks]
  (->> (select-keys target ks)
       (map (fn [[k v]]
              [k (prune-to v (get-in source [k]))]))
       (into {})))

(defn safe-nth
  [col index]
  (when (> (count col) index)
    (nth col index)))

(defn mkstr
  "Creates a new string be repeating the specifed
  based the specified number of times"
  [base length]
  (->> (repeat base)
       (take length)
       (string/join "")))

(defn conj-to-last
  "Given a list of lists, conjs the value x onto the last (as identified by the fn pop) inner list of the outer list"
  [lists x]
  (conj (pop lists)
        (conj (peek lists) x)))

(defn- ->coll
  [x]
  (if (coll? x) x [x]))

(defn fscalar
  "Given a function that expects a collection as the first argument, returns a
  function that, if passed something that isn't a collection, creates a new
  collection, adds the first arg to it, then applies the function"
  [f]
  (fn
    ([x] (f (->coll x)))
    ([x a] (f (->coll x) a))
    ([x a b] (f (->coll x) a b))
    ([x a b & cs] (apply f (->coll x) a b cs))))
