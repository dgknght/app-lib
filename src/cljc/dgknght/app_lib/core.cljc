(ns dgknght.app-lib.core
  (:refer-clojure :exclude [uuid decimal? format])
  (:require [clojure.string :as string]
            [clojure.walk]
            #?(:clj [clojure.core :as cc])
            #?(:clj [clojure.pprint :refer [pprint]]
               :cljs [cljs.pprint :refer [pprint]])
            #?(:cljs [goog.string :as gstr])
            #?(:cljs [dgknght.app-lib.decimal :as d]))
  #?(:clj (:import java.util.UUID clojure.lang.IDeref
                   clojure.lang.PersistentList
                   clojure.lang.PersistentVector
                   clojure.lang.PersistentArrayMap
                   clojure.lang.PersistentHashMap
                   clojure.lang.LazySeq)))

(derive #?(:clj PersistentVector
           :cljs cljs.core/PersistentVector)
        ::vector)
(derive #?(:clj PersistentList
           :cljs cljs.core/List)
        ::list)
(derive #?(:clj clojure.lang.PersistentList$EmptyList
           :cljs cljs.core/EmptyList)
        ::list)
(derive #?(:clj PersistentArrayMap
           :cljs cljs.core/PersistentArrayMap)
        ::map)
(derive #?(:clj PersistentHashMap
           :cljs cljs.core/PersistentHashMap)
        ::map)
(derive LazySeq ::sequential)
(derive ::vector ::sequential)
(derive ::list ::sequential)
(derive #?(:clj clojure.lang.MapEntry
           :cljs cljs.core/MapEntry)
        ::map-entry)
(derive #?(:clj java.lang.String
           :cljs js/String)
        ::string)

(defn format
  [msg & args]
  #?(:clj (apply clojure.core/format msg args)
     :cljs (apply gstr/format msg args)))

(def boolean-values #{"true" "1" "y" "yes" "t"})

(defn ensure-string
  [v]
  (if (keyword? v)
    (name v)
    (str v)))

(defn parse-bool
  [value]
  (when-not (nil? value)
    (if (boolean? value)
      value
      (contains? boolean-values
                 (string/lower-case value)))))

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
  {:pre [(map? m)]}

  (if v
    (assoc m k v)
    m))

(defn assoc-unless
  "Performs an assoc unless the map already contains the key"
  [m k v]
  {:pre [(map? m)]}

  (if (contains? m k)
    m
    (assoc m k v)))

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

(defn presence
  [v]
  (when (present? v) v))

(defn index-by
  [key-fn coll]
  {:pre [(sequential? coll)]}

  (->> coll
       (map (juxt key-fn identity))
       (into {})))

(declare prune-map)

(defmulti prune-to
  (fn [& args]
    (mapv type args)))

(defmethod prune-to [::map ::map]
  [target source]
  (prune-map target source (keys source)))

(defmethod prune-to [::sequential ::sequential]
  [target source]
  (if (map? (first source))
    (let [ks (set (mapcat keys source))
          src (concat source (repeat {}))]
      (vec (map-indexed #(prune-map %2 (nth src %1) ks)
                        target)))
    target))

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
  (when (and (not (nil? index))
             (> (count col) index))
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

(defn ->sequential
  [x]
  (cond
    (nil? x)         []
    (sequential? x)  x
    :else            [x]))

(defn fscalar
  "Given a function that expects something sequential as the first argument,
  returns a function that, if passed something that isn't sequential, creates a new
  vector, adds the first arg to it, then applies the function"
  [f]
  (fn
    ([x] (f (->sequential x)))
    ([x a] (f (->sequential x) a))
    ([x a b] (f (->sequential x) a b))
    ([x a b & cs] (apply f (->sequential x) a b cs))))

(defn fmax
  "Given a function that returns a number, ensure that the result
  is never more than the specified value"
  [f maximum]
  (fn [input]
    (let [output (f input)]
      (if (>= output maximum)
        maximum
        output))))

(defn fmin
  "Given a function that returns a number, ensure that the result
  is never less than the specified value"
  [f minimum]
  (fn [input]
    (let [output (f input)]
      (if (<= output minimum)
        minimum
        output))))

(defn derefable?
  "Returns a boolean value indicating whether or not
  a value can be dereferenced."
  [v]
  #?(:clj (instance? IDeref v)
     :cljs (satisfies? IDeref v)))

(defn one?
  [coll]
  {:pre [(sequential? coll)]}

  (= 1 (count coll)))

(defn dissoc-in
  [m [k & ks]]
  (if (empty? ks)
    (dissoc m k)
    (update-in m [k] dissoc-in ks)))
