(ns dgknght.app-lib.inflection
  (:require [clojure.string :as string]
            [dgknght.app-lib.core :refer [ensure-string]]
            #?(:cljs [goog.string :as gstr])))

(defn humanize
  "Accepts a value in kabob case and returns the value in human friendly form"
  [value]
  (-> value
      ensure-string
      (string/replace #"[_-]" " ")
      (string/replace #" id$" "")
      string/capitalize))

(def ^:private title-case-ignore-patterns
  [#"^\s+$"
   #"^[A-Z]+$"])

(defn- title-case-ignore?
  [word]
  (some #(re-matches % word) title-case-ignore-patterns))

(defn- title-case-word
  [word]
  (if (title-case-ignore? word)
    word
    (string/capitalize word)))

(defn title-case
  "Renders the string in title case. E.g.
  (title-case \"my important thing\") => \"My Important Thing\""
  [s]
  (->> (-> s
           ensure-string
           (string/replace "-" " ")
           (string/split #"\b"))
       (map title-case-word)
       (string/join "")))

(defn ordinal
  "Accepts a number and returns a string expressing the value
  as an ordinal. E.g., 1 => '1st'"
  [number]
  (let [rules [{:pattern #"(?:[2-9]|\b|^)1\z"
                :suffix  "st"}
               {:pattern #"(?:[2-9]|\b|^)2\z"
                :suffix "nd"}
               {:pattern #"(?:[2-9]|\b|^)3\z"
                :suffix "rd"}
               {:pattern #"."
                :suffix "th"}]
        s (str number)]
    (str s (some #(when (re-find (:pattern %) s)
                    (:suffix %))
                 rules))))

(defn- apply-matching-rule
  [word {pattern :pattern f :fn}]
  (when-let [match (re-find pattern word)]
    (f match)))

(defn singular
  "Accepts a plural noun and attempts to convert it into singular"
  [word]
  (let [rules [{:pattern #"(?i)\A(child)ren\z"
                :fn second}
               {:pattern #"(.+)s\z"
                :fn second}]]
    (some (partial apply-matching-rule word) rules)))

(defn plural
  "Acceps a singular noun and attempts to convert it into plural"
  [word]
  {:pre [word]}
  (let [[kw word] (if (keyword? word)
                       [true (name word)]
                       [false word])
        rules [{:pattern #".*(?=y\z)"
                :fn (fn [match] (str match "ies"))}
               {:pattern #".*"
                :fn (fn [match] (str match "s"))}]
        result (some (partial apply-matching-rule word) rules)]
    (if kw
      (keyword result)
      result)))

(defn- fmt
  [msg & args]
  #?(:clj (apply format msg args)
     :cljs (apply gstr/format msg args)))

(defn conjoin
  "Accepts a sequences and returns a comma delimited string
  with the specified conjunction between the final two elements"
  [conjunction col]
  (case (count col)
    1 (ensure-string (first col))
    2 (fmt "%s %s %s"
           (ensure-string (first col))
           conjunction
           (ensure-string (second col)))
    (let [strings (map ensure-string col)]
      (fmt "%s, %s %s"
           (string/join ", " (butlast strings))
           conjunction
           (last strings)))))
