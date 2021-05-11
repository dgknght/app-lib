(ns dgknght.app-lib.time
  (:refer-clojure :exclude [compare time])
  (:require [clojure.string :as string]
            [cljs-time.core :as t]
            [goog.string :as gstr]))

(defprotocol Timeish
  (add-minutes
    [this minutes]
    "Returns a new time the specified number of minutes after the instance")
  (->vector
    [this]
    "Returns the hour, minute, and second in a vector"))

(defrecord Time [hour minute second]
  Timeish
  (add-minutes [_ m]
    (let [new-m (+ m minute)]
      (Time. (+ hour (quot new-m 60))
             (mod new-m 60)
             second)))

  (->vector [_]
    [hour minute second]))

(defn time
  ([hour]               (time hour 0))
  ([hour minute]        (time hour minute 0))
  ([hour minute second] (Time. hour minute second)))

(defn format-time
  [t]
  (let [[adj-h ampm] (cond
                (= (:hour t) 0)
                [12 "AM"]

                (= (:hour t) 12)
                [(:hour t) "PM"]

                (> (:hour t) 12)
                [(- (:hour t) 12) "PM"]

                :else
                [(:hour t) "AM"])]
  (gstr/format "%d:%02d %s" adj-h (:minute t) ampm)))

(def ^:private time-pattern
  #"(?i)^(\d{1,2})(?::(\d{2})(?::(\d{2}))?)?\s*(A|P)M?$")

(defn parse
  [time-str]
  (when-let [[_ h m s ampm] (re-find time-pattern time-str)]
    (let [hour (js/parseInt h)
          minute (js/parseInt (or m "0"))
          second (js/parseInt (or s "0"))
          hour (if (= "p" (string/lower-case ampm))
                 (if (= 12 hour)
                   hour
                   (+ hour 12))
                 (if (= 12 hour)
                   0
                   hour))]
      (Time. hour
             minute
             second))))

(defn serialize
  [t]
  (gstr/format "%02d:%02d:%02d" (:hour t) (:minute t) (:second t)))

(defn unserialize
  [time-str]
  (when-let [[_ h m s] (re-find #"^(\d{1,2}):(\d{2})(?::(\d{2}))?$" time-str)]
    (Time. (js/parseInt h)
           (js/parseInt m)
           (js/parseInt (or s "0")))))

(defn compare
  [t1 t2]
  (if t2
    (->> [t1 t2]
         (map ->vector)
         (apply clojure.core/compare))
    1))

(defn compare-rev
  [t1 t2]
  (if t2
    (->> [t2 t1]
         (map ->vector)
         (apply clojure.core/compare))
    1))

(defn local-time
  ([]
   (let [right-now (t/time-now)]
    (local-time (t/hour right-now)
                (t/minute right-now)
                (t/second right-now))))
  ([hour]
   (local-time hour 0))
  ([hour minute]
   (local-time hour minute 0))
  ([hour minute second]
   (Time. hour minute second)))

(defn date-at
  "Returns a date-time consisting of the given local-date and local-time"
  ([local-date local-time]
   (t/local-date-time (t/year local-date)
                      (t/month local-date)
                      (t/day local-date)
                      (:hour local-time)
                      (:minute local-time)
                      (:second local-time))))
