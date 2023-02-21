(ns dgknght.app-lib.dates
  (:require #?(:clj [clj-time.core :as t]
               :cljs [cljs-time.core :as t])
            #?(:clj [clj-time.coerce :as tc]
               :cljs [cljs-time.coerce :as tc])
            #?(:clj [clj-time.periodic :refer [periodic-seq]]
               :cljs [cljs-time.periodic :refer [periodic-seq]])
            [clojure.string :as string]
            [dgknght.app-lib.core :as lib]))

(defn- parse-partial
  [value]
  (->> (re-matches #"(\d{4})(?:-(\d{2})(?:-(\d{2}))?)?"
                                          value)
                              rest
                              (map lib/parse-int)))

(defn parse-range
  "Accepts a date range in a variety of formats and returns
  a tuple containing the start and end dates, like [start end].

  2015                   => [#local-date 2015-01-01 #local-date 2015-12-31]
  2015-03                => [#local-date 2015-03-01 #local-date 2015-03-31]
  2015-03-02             => [#local-date 2015-03-02 #local-date 2015-03-02]
  #local-date 2015-03-03 => [#local-date 2015-03-02 #local-date 2015-03-02]"
  [value]
  (let [[year month day] (parse-partial value)]
    (cond

      day
      [(t/local-date year month day)
       (t/local-date year month day)]

      month
      [(tc/to-local-date (t/first-day-of-the-month year month))
       (tc/to-local-date (t/last-day-of-the-month year month))]

      :else
      [(t/local-date year 1 1)
       (t/local-date year 12 31)])))

(defn parse-interval
  "Takes a string containing a date or partial date and returns a corresponding interval

  2015       => (t/interval (t/date-time 2015 1 1) (t/date-time 2016 1 1))
  2015-03    => (t/interval (t/date-time 2015 3 1) (t/date-time 2015 4 1))
  2015-03-02 => (t/interval (t/date-time 2015 3 2) (t/date-time 2015 3 3))"
  [value]
  (let [[year month day] (parse-partial value)]
    (cond

      day
      (let [start (t/date-time year month day)]
        (t/interval start
                    (t/plus start
                            (t/days 1))))

      month
      (let [start (t/date-time year month 1)]
        (t/interval start
                    (t/plus start (t/months 1))))

      :else
      (let [start (t/date-time year 1 1)]
        (t/interval start
                    (t/plus start (t/years 1)))))))

(defn intervals
  [start interval]
  (->> (periodic-seq start interval)
       (partition 2 1)
       (map (fn [[s e]] (t/interval s e)))))

(defn ranges
  [start interval]
  (->> (periodic-seq start interval)
       (partition 2 1)
       (map (fn [[s e]] [s (t/minus e (t/days 1))]))))

(defn period
  [interval-type interval-count]
  {:pre [(#{:year :month :week} interval-type)]}

  (case interval-type
    :year (t/years interval-count)
    :month (t/months interval-count)
    :week (t/weeks interval-count)))

(defn relative
  [desc]
  (let [today (t/today)]
    (case desc
      "today"                   today
      "tomorrow"                (t/plus today (t/days 1))
      "yesterday"               (t/minus today (t/days 1))
      "start-of-this-year"      (t/local-date (t/year today) 1 1)
      "end-of-this-year"        (t/local-date (t/year today) 12 31)
      "start-of-this-month"     (t/first-day-of-the-month today)
      "end-of-this-month"       (t/last-day-of-the-month today)
      "start-of-previous-year"  (t/local-date (t/year (t/minus today (t/years 1))) 1 1)
      "end-of-previous-year"    (t/local-date (t/year (t/minus today (t/years 1))) 12 31)
      "start-of-previous-month" (t/first-day-of-the-month (t/minus today (t/months 1)))
      "end-of-previous-month"   (t/last-day-of-the-month (t/minus today (t/months 1))))))

(defn nominal-keys
  "Given a canonical key, return all of the nominal variants"
  [canonical]
  (let [str-key (name canonical)
        [_  base-key] (re-find #"^(.*)(?:-on|-at)$" str-key)]
    (map keyword [str-key
                  (str base-key "-before")
                  (str str-key "-or-before")
                  (str base-key "-after")
                  (str str-key "-or-after")])))

(defn- includes-time?
  [date]
  #?(:clj (instance? org.joda.time.DateTime date)
     :cljs (instance? goog.date.DateTime date)))

(defn- nominal-key
  [key-base [oper value]]
  (let [prep (if (includes-time? value)
               "at"
               "on")]
    (keyword
     (str
      key-base
      "-"
      (case oper
        :>  "after"
        :>= (str prep "-or-after")
        :<  "before"
        :<= (str prep "-or-before"))))))

(defn- apply-to-dynamic-keys
  [m {:keys [key-base suffixes update-fn]}]
  (let [str-k (name key-base)]
    (->> suffixes
         (mapv #(keyword (str str-k %)))
         (reduce (fn [result k]
                   (if-let [value (get-in m [k])]
                     (-> result
                         (dissoc k)
                         (update-fn str-k k value))
                     result))
                 m))))

(defn- between->nominal
  [m key-base]
  (let [[_ start end] (get-in m [key-base])]
    (if (and start end)
      (let [str-key (name key-base)
            prefix (if (string/ends-with? str-key "date")
                     "-on"
                     "")]
        (-> m
            (assoc (keyword (str str-key
                                 prefix
                                 "-or-after")) start
                   (keyword (str str-key
                                 prefix
                                 "-or-before")) end)
            (dissoc key-base)))
      m)))

(defn nominal-comparatives
  "Accepts a map and a key base and converts values with attributes
  that match the key base from symbolic comparatives into nominal
  comparatives.

  (nominal-comparatives {:end-on [:> some-date]} :end) => {:end-after some-date}"
  [m key-base]
  (-> m
      (between->nominal key-base)
      (apply-to-dynamic-keys
       {:key-base key-base
        :suffixes ["-on" "-at" nil]
        :update-fn (fn [result str-k _ value]
                     (assoc result (nominal-key str-k value)
                            (second value)))})))

(def ^:private suffix-keys
  {"before"       :<
   "on-or-before" :<=
   "at-or-before" :<=
   "after"        :>
   "on-or-after"  :>=
   "at-or-after"  :>=})

(defn nominative-variations
  [key-base]
  (let [str-key (name key-base)]
    (map #(keyword (str str-key %))
         [""
          "-before"
          "-on-or-before"
          "-at-or-before"
          "-after"
          "-on-or-after"])))

(defn- symbolic-key
  [key-base k value]
  (let [key-suffix (string/replace (name k) (str key-base "-") "")
        final-key (keyword
                   (str key-base
                        (when-not (string/ends-with? key-base "-date")
                          (if (includes-time? value) "-at" "-on"))))
        oper (get-in suffix-keys [key-suffix])]
    [final-key [oper value]]))

(defn nominal->between
  [m key-base]
  (let [str-key (name key-base)
        prefix (if (string/ends-with? str-key "date")
                 "-on"
                 "")
        start-key (keyword (str (name key-base) prefix "-or-after"))
        end-key (keyword (str (name key-base) prefix "-or-before"))
        start (get-in m [start-key])
        end (get-in m [end-key])]
    (if (and start end)
      (-> m
          (dissoc start-key end-key)
          (assoc key-base [:between start end]))
      m)))

(defn symbolic-comparatives
  "Accepts a map with comparative keys and updates the
  values with symbolic operators.

  (symbolic-comparatives {:end-after some-date} :end) => {:end-on [:> some-date]}"
  [m key-base]
  (-> m
      (nominal->between key-base)
      (apply-to-dynamic-keys
       {:key-base key-base
        :suffixes ["-before"
                   "-on-or-before"
                   "-at-or-before"
                   "-after"
                   "-on-or-after"
                   "-at-or-after"]
        :update-fn (fn [result str-k k value]
                     (let [[new-key value-with-oper] (symbolic-key str-k k value)]
                       (assoc result new-key value-with-oper)))})))

(defn earliest
  [& dates]
  (->> dates
       (filter identity)
       (sort t/before?)
       first))

(defn latest
  [& dates]
  (->> dates
       (filter identity)
       (sort t/after?)
       first))
