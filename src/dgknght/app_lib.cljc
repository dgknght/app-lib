(ns dgknght.app-lib
  (:require #?(:clj [clojure.pprint :refer [pprint]]))
  #?(:clj (:import java.lang.Integer
                   java.util.UUID)))

(defn trace
  [msg]
  #?(:clj (pprint msg)
     :cljs (.log js/console (prn-str msg))))

(defn parse-int
  "Attempts to parse and return any non-nil value as an integer"
  [v]
  (when v
    #?(:clj (Integer/parseInt v)
       :cljs (js/parseInt v))))

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

#?(:clj
   (defn uuid
     ([] (UUID/randomUUID))
     ([value]
      (if (instance? UUID value)
        value
        (UUID/fromString (str value))))))
