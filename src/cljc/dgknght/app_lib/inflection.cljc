(ns dgknght.app-lib.inflection
  (:require [clojure.string :as string]
            #?(:clj  [clojure.pprint :refer [pprint]]
               :cljs [cljs.pprint :refer [pprint]])
            [dgknght.app-lib.core :refer [ensure-string]]
            #?(:cljs [goog.string :as gstr])))

#?(:clj (do
          (derive clojure.lang.Keyword ::keyword)
          (derive java.lang.String ::string))
   :cljs (do
          (derive cljs.core.Keyword ::keyword)
          (derive js/String ::string)))


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

(def ^:private ordinal-rules
  [[#"(?:[2-9]|\b|^)1$" "st"]
   [#"(?:[2-9]|\b|^)2$" "nd"]
   [#"(?:[2-9]|\b|^)3$" "rd"]
   [#"."                "th"]])

(defn ordinal
  "Accepts a number and returns a string expressing the value
  as an ordinal. E.g., 1 => '1st'"
  [number]
  (let [s (str number)
        suffix (->> ordinal-rules
                    (map #(update-in % [0] (fn [x] (re-find x s))))
                    (filter first)
                    (map second)
                    first)]
    (str s suffix)))

(defn- apply-word-rule
  [[match f]]
  (f match))

(defn- apply-word-rules
  [word rules]
  (->> rules
       (map #(update-in % [0] (fn [p] (re-find p word))))
       (filter first)
       (map apply-word-rule)
       first))

(defmulti singular
  "Accepts a plural noun and attempts to convert it into singular"
  type)

(def ^:private ->singular-rules
  [[#"(?i)^(child)ren$"    second]
   [#"(?i)^moose$"         identity]
   [#"(?i)^geese$"         (constantly "goose")]
   [#"(?i)^mice$"          (constantly "mouse")]
   [#"(?i)^((?:wo)?m)en$" #(str (second %) "an")]
   [#"^(.+)ies$"           #(str (second %) "y")]
   [#"^(.+)s$"             second]])

(defmethod singular :default [x] x)

(defmethod singular ::string
  [word]
  (apply-word-rules word ->singular-rules))

(defmethod singular ::keyword
  [word]
  (-> word
      name
      singular
      keyword))

(def ^:private ->plural-rules
  [[#"(?i)^child$"        #(str % "ren")]
   [#"(?i)^moose$"         identity]
   [#"(?i)^goose$"         (constantly "geese")]
   [#"(?i)^mouse$"         (constantly "mice")]
   [#"(?i)^((?:wo)?m)an$" #(str (second %) "en")]
   [#"^(.+)y$"            #(str (second %) "ies")]
   [#"^.+$"               #(str % "s")]])

(defmulti plural
  "Acceps a singular noun and attempts to convert it into plural"
  type)

(defmethod plural :default [x] x)

(defmethod plural ::string
  [word]
  (apply-word-rules word ->plural-rules))

(defmethod plural ::keyword
  [word]
  (-> word
      name
      plural
      keyword))

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
