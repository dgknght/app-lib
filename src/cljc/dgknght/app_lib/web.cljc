(ns dgknght.app-lib.web
  (:require [clojure.string :as string]
            #?(:clj [clojure.tools.logging :as log])
            [dgknght.app-lib.dates :as dates]
            #?(:cljs [dgknght.app-lib.time :as tm])
            #?(:clj [clj-time.format :as tf]
               :cljs [cljs-time.format :as tf]))
  (:import #?(:clj org.joda.time.LocalDate
              :cljs goog.date.Date)
           #?(:clj [java.text NumberFormat DecimalFormat]
              :cljs goog.i18n.NumberFormat)))

(def email-pattern
    #"(?i)^[a-z0-9_+.-]+@[a-z0-9_+.-]+\.[a-z]{2,4}$")

(defn ->string
  [value]
  (if (keyword? value)
    (name value)
    (str value)))

(defn path
  "Create a path from the specified segments"
  [& segments]
  (str "/" (->> segments
                (map ->string)
                (string/join "/"))))

(defn date?
  [value]
  #?(:clj (instance? LocalDate value)
     :cljs (instance? Date value)))

(defn serialize-date
  [date]
  {:pre [(date? date)]}

  (when date
    (tf/unparse-local-date (tf/formatters :date) date)))

(def ^:private non-nil-string?
  (every-pred string? seq))

(defn- date-string?
  [d]
  (when (non-nil-string? d)
    (re-find #"^\d{4}-\d{2}-\d{2}$" d)))

(def unserialize-date
  (some-fn #(when (date? %) %)
           #(when (date-string? %)
              (tf/parse-local-date (tf/formatters :date) %))
           #(when (non-nil-string? %)
              (dates/relative %))))

(defn serialize-date-time
  [date-time]
  (when date-time
    (tf/unparse (tf/formatters :date-time-no-ms) date-time)))

(defn unserialize-date-time [s]
  (let [f #(when (seq s) (tf/parse (tf/formatters :date-time-no-ms) s))]
    #?(:clj (try (f) (catch Exception e (log/debugf "Unable to parse date time \"%s\": %s" s (.getMessage e))))
       :cljs (try (f) (catch js/Error e (.warn js/console (str "Unable to parse date time \"" s "\": " (.-message e))))))))

(defn serialize-time
  [local-time]
  (when local-time
    #?(:clj (tf/unparse-local-time (tf/formatters :hour-minute-second) local-time)
       :cljs (tm/serialize local-time))))

(defn unserialize-time
  [time-str]
  (when time-str
    #?(:clj (tf/parse-local-time (tf/formatters :hour-minute-second) time-str)
       :cljs (tm/unserialize time-str))))

(def ^:private date-format (tf/formatter "M/d/yyyy"))

(defn- resolve-formatter
  [fmt]
  (cond
    (string? fmt) (tf/formatter fmt)
    (keyword? fmt) (tf/formatters fmt)
    :else fmt))

(defn format-date
  ([date] (format-date date date-format))
  ([date fmt]
   (when date
     (tf/unparse-local-date (resolve-formatter fmt) date))))

(defn unformat-date
  [date-string]
  (when (and date-string
             (re-find #"^\d{1,2}/\d{1,2}/\d{4}$" date-string))
    (try
      (tf/parse-local-date date-format date-string)
      (catch #?(:clj IllegalArgumentException
                :cljs js/Error) _
        nil))))

(def ^:private default-format-spec
  "M/d/yyyy h:mm a")

(defn format-date-time
  ([dt] (format-date-time dt default-format-spec))
  ([dt format-spec]
   (when dt
     (let [formatter (if (string? format-spec)
                       (tf/formatter format-spec)
                       (tf/formatters format-spec))]
       (tf/unparse formatter dt)))))

(defn reformat-date-time
  ([dt-str] (reformat-date-time dt-str default-format-spec))
  ([dt-str format-spec]
   (format-date-time (tf/parse (tf/formatters :date-time-no-ms dt-str)
                               dt-str)
                     format-spec)))

(defn format-time
  [local-time]
  #?(:clj (tf/unparse-local-time (tf/formatters :hour-minute) local-time)
     :cljs (tm/format-time local-time)))

(defn unformat-time
  [time-str]
  #?(:clj (tf/parse-local-time (tf/formatters :hour-minute) time-str)
     :cljs (tm/parse time-str)))

(defn- number-format
  [fmt]
  {:pre [(#{:decimal :percent :currency} fmt)]}

  #?(:clj (doto (case fmt
                  :decimal (DecimalFormat.)
                  :percent (NumberFormat/getPercentInstance)
                  :currency (NumberFormat/getCurrencyInstance))
            (.setGroupingUsed true))
     :cljs (NumberFormat. (case fmt
                            :decimal  (.-DECIMAL  (.-Format NumberFormat))
                            :percent  (.-PERCENT  (.-Format NumberFormat))
                            :currency (.-CURRENCY (.-Format NumberFormat))))))

(defn format-decimal
  ([value] (format-decimal value {}))
  ([value {:keys [fraction-digits]
           :or {fraction-digits 2}}]
   (.format (doto (number-format :decimal)
              (.setMaximumFractionDigits fraction-digits)
              (.setMinimumFractionDigits fraction-digits))
            value)))

(defn format-percent
  ([value] (format-percent value {}))
  ([value {:keys [fraction-digits]
           :or {fraction-digits 1}}]
   (.format (doto (number-format :percent)
              (.setMaximumFractionDigits fraction-digits)
              (.setMinimumFractionDigits fraction-digits))
            value)))

(defn format-currency
  [value]
  (.format (doto (number-format :currency)
              (.setMaximumFractionDigits 2)
              (.setMinimumFractionDigits 2))
            value))
