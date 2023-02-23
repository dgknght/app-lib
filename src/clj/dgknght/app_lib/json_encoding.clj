(ns dgknght.app-lib.json-encoding
  (:require [cheshire.generate :refer [add-encoder]]
            [dgknght.app-lib.web :refer [serialize-date
                                         serialize-time
                                         serialize-date-time]])
  (:import [org.joda.time
            LocalDate
            LocalTime
            DateTime]))

(add-encoder LocalDate (fn [date g]
                         (.writeString g (serialize-date date))))

(add-encoder LocalTime (fn [local-time g]
                         (.writeString g (serialize-time local-time))))

(add-encoder DateTime (fn [date g]
                        (.writeString g (serialize-date-time date))))
