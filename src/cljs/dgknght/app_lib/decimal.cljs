(ns dgknght.app-lib.decimal
  (:refer-clojure :exclude [+ - * / zero?])
  (:require ["decimal.js" :as Decimal]))

(defn ->decimal
  [value]
  (when value
    (Decimal. value)))

(defn decimal?
  [v]
  (instance? Decimal v))

(extend-type Decimal
  IEquiv
  (-equiv [this other]
    (when-let [parsed (try
                      (when other
                        (->decimal other))
                      (catch js/Error _ nil))]
      (.equals this parsed))))

(defn abs
  [value]
  (when value
    (.abs (->decimal value))))

(defn zero []
  (->decimal 0))

(defn zero?
  [value]
  (when value
    (.isZero (->decimal value))))

(defn +
  [& vs]
  (when (not-every? nil? vs)
    (->> vs
         (filter identity)
         (map ->decimal)
         (reduce #(.plus %1 %2) (zero)))))

(defn -
  [v1 v2]
  (when (and v1 v2)
    (.minus (->decimal v1) v2)))

(defn *
  [v1 v2]
  (when (and v1 v2)
    (.times (->decimal v1) v2)))

(defn /
  [v1 v2]
  (when (and v1 v2)
    (.div (->decimal v1) v2)))

(defn sum
  [coll]
  (->> coll
       (filter identity)
       (reduce + 0M)))

(defn trunc
  [d]
  (.trunc d))

(defn round
  [d]
  (when d
    (.round d)))
