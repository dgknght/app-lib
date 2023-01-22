(ns dgknght.app-lib.forms.common
  (:require [clojure.string :as string]))

(defn ->id
  [field]
  (->> field
       (map (comp #(string/replace % #"[^a-z0-9_-]+" "")
                  #(if (keyword? %)
                     (name %)
                     (str %))))
       (string/join "-")))

(defn ->name
  [field]
  (->> field
       (map #(if (keyword? %)
               (name %)
               %))
       (string/join "-")))
