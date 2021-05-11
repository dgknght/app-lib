(ns dgknght.app-lib.validation
  (:refer-clojure :exclude [format])
  (:require [clojure.core :as c]
            [clojure.spec.alpha :as s]
            [dgknght.app-lib.web :refer [email-pattern] ]
            [dgknght.app-lib.inflection :refer [humanize
                                                conjoin]])
  (:import [org.joda.time LocalDate DateTime LocalTime]))

(def email?
  (every-pred string?
              (partial re-matches email-pattern)))

(def local-date?
  (partial instance? LocalDate))

(def nilable-local-date?
  (some-fn nil? local-date?))

(def local-time?
  (partial instance? LocalTime))

(def date-time?
  (partial instance? DateTime))

(def nilable-date-time?
  (some-fn nil? date-time?))

(def non-empty-string?
  (every-pred string? seq))

(def positive-integer?
  (every-pred integer? pos?))

(def nilable-positive-integer?
  (some-fn nil? positive-integer?))

(def positive-number?
  (every-pred number? pos?))

(def nilable-positive-number?
  (some-fn nil? positive-number?))

(def positive-big-dec?
  (every-pred decimal? pos?))

(def big-dec-not-less-than-zero?
  (every-pred decimal?
              (some-fn pos? zero?)))

(defn min-length?
  [minimum value]
  (>= (count value) minimum))

(defn not-longer-than?
  [max-length value]
  (<= (count value) max-length))

(defn length-between?
  [min-length max-length value]
  (let [actual (count value)]
    (and (<= actual max-length)
         (>= actual min-length))))

(defn- req-pred?
  "Returns a boolean value if this predicate is the one the framework
  uses to satisfy a :req or :req-un specification"
  [pred]
  (and (sequential? pred)
       (= 'clojure.core/fn (nth pred 0))
       (= 'clojure.core/contains? (first (nth pred 2)))))

(defn- resolve-<=
  [pred]
  (when (= 'clojure.core/<= (nth pred 0))
    (str "%s must contain at least "
         (nth pred 1)
         " item(s)")))

(defn- resolve-contains?
  [pred]
  (when (and (= 'clojure.core/fn (nth pred 0))
             (= 'clojure.core/contains? (nth (nth pred 2) 0)))
    "%s is required"))

(declare spec-data)

(defn- simple-resolve-path
  [pred]
  (when (symbol? pred)
    (get-in @spec-data [::paths pred])))

(def spec-data (atom {::path-resolvers [simple-resolve-path]
                      ::message-resolvers [resolve-<=
                                           resolve-contains?]
                      ::paths {}
                      ::messages {'clojure.core/identity "%s is required"
                                  'clojure.core/string? "%s must be a string"
                                  'clojure.core/integer? "%s must be an integer"
                                  'clojure.core/decimal? "%s must be a number"
                                  'decimal? "%s must be a number"
                                  'dgknght.app-lib.core/present? "%s is required"
                                  'dgknght.app-lib.validation/non-empty-string? "%s is required"
                                  'dgknght.app-lib.validation/positive-integer? "%s must be greater than zero"
                                  'dgknght.app-lib.validation/positive-big-dec? "%s must be greater than zero"
                                  'dgknght.app-lib.validation/local-date? "%s must be a date"
                                  'dgknght.app-lib.validation/nilable-local-date? "%s must be a date"
                                  'dgknght.app-lib.validation/big-dec-not-less-than-zero? "%s cannot be less than zero"
                                  'dgknght.app-lib.validation/email? "%s must be a valid email address"
                                  'dgknght.app-lib.validation/min-length? "%s must be at least %s characters"
                                  'dgknght.app-lib.validation/length-between? "%s must be between %s and %s characters"
                                  'dgknght.app-lib.validation/not-longer-than? "%s cannot be more than %s characters"}}))

(defmacro reg-spec
  [pred {:keys [message path]}]
  `(let [sym# (symbol (resolve '~pred))]
     (swap! spec-data (fn [d#]
                        (cond-> d#
                          ~message (assoc-in [::messages sym#] ~message)
                          ~path (assoc-in [::paths sym#] ~path))))))

(defmacro reg-msg
  [pred msg]
  `(swap! spec-data assoc (symbol (resolve '~pred)) ~msg))

(defn reg-path-resolver
  [resolver]
  (swap! spec-data update-in [::path-resolvers] conj resolver))

(defn- default-pred-msg
  [pred]
  (if (req-pred? pred)
    "%s is required"
    (get-in @spec-data [pred] "%s is invalid")))

(defn- unwrap-partial
  [pred]
  (if (and (sequential? pred)
           (= 'clojure.core/partial
              (nth pred 0)))
    [(nth pred 1)
     (drop 2 pred)]
    [pred []]))

(defmulti ^:private pred-msg type)

(defmethod pred-msg clojure.lang.PersistentHashSet
  [pred]
  (str "%s must be "
       (conjoin "or" pred)))

(defn- resolve-msg
  [pred]
  (let [f (apply some-fn (::message-resolvers @spec-data))]
    (f pred)))

(defn- seq-pred-msg
  [pred]
  (or
    (get-in @spec-data [::messages (nth pred 0)])
    (resolve-msg pred)
    (default-pred-msg (nth pred 0))))

(defmethod pred-msg clojure.lang.PersistentList
  [pred]
  (seq-pred-msg pred))

(defmethod pred-msg clojure.lang.LazySeq
  [pred]
  (seq-pred-msg pred))

(defmethod pred-msg clojure.lang.Cons
  [pred]
  (seq-pred-msg pred))

(defmethod pred-msg clojure.lang.Symbol
  [pred]
  (or (get-in @spec-data [::messages pred])
      (default-pred-msg pred)))

(defmethod pred-msg :default
  [pred]
  (throw (ex-info (str "Unknown predicate type: " (type pred))
                  {:pred pred})))

(defn- resolve-path
  [path pred]
  (cond
    (req-pred? pred) (conj path (nth (nth pred 2) 2))
    (seq path) path
    :else (let [f (apply some-fn (::path-resolvers @spec-data))]
                (f pred))))

(defn- fieldize
  [attr-key]
  (if (integer? attr-key)
    "Value"
    (humanize attr-key)))

(defn- append-error
  [errors {:keys [pred in]}]
  (let [[unwrapped args] (unwrap-partial pred)
        msg (pred-msg unwrapped)
        path (resolve-path in pred)]
    (update-in errors path (fnil conj [])
               (apply c/format
                      msg
                      (fieldize (last path))
                      args))))

(defn- ->errors
  [{::s/keys [problems]}]
  (reduce append-error
          {}
          problems))

(defn validate
  "Validates the specified model using the specified spec"
  [model spec]
  (if-let [result (s/explain-data spec model)]
    (assoc model ::valid? false ::explanation result ::errors (->errors result))
    (assoc model ::valid? true ::errors {})))

(defn has-error?
  "Returns true if the specified model contains validation errors"
  ([model]
   (-> model ::errors seq))
  ([model path]
   (let [path (if (keyword? path)
                [path]
                path)]
     (get-in model (cons ::errors path)))))

(defn valid?
  "Returns false if the model has any validation errors"
  [model]
  (-> model ::errors seq not))

(defn error-messages
  "Returns the errors from the specified model. If given only a model,
  returns a map of all errors. If given a model and a key, returns the
  errors for the specified key from wihin the model."
  ([model]
   (::errors model))
  ([model path]
   (let [path (if (keyword? path)
                [path]
                path)]
     (get-in (error-messages model) path))))

(defn flat-error-messages
  "Returns a flat list of strings describing the error messages for the
  model instead of the map returned by error-messages"
  [model]
  (-> model
      ::errors
      vals
      flatten))

(defmacro with-validation
  "Accepts a model and validation rules. If the model
  passes the rules, the specified body is executed
  and the result returned. If not, the invalid model,
  with validation errors, is returned.

  Note that this rebinds the validated user object
  to the same binding used to call the macro."
  [model spec & body]
  `(let [validated# (validate ~model ~spec)
         f# (fn* [~model] ~@body)]
     (if (valid? validated#)
       (f# validated#)
       validated#)))
