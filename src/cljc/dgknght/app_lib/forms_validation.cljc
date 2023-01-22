(ns dgknght.app-lib.forms-validation
  (:refer-clojure :exclude [format])
  (:require [clojure.string :as string]
            #?(:cljs [cljs.core.async :as a]
               :clj  [clojure.core.async :as a])
            #?(:cljs [goog.string :as gstr])
            #?(:cljs [cljs.core :refer [Keyword PersistentVector]])
            #?(:cljs [cljs.core.async.impl.channels :refer [ManyToManyChannel]])
            #?(:clj [clojure.pprint :refer [pprint]]
               :cljs [cljs.pprint :refer [pprint]])
            [dgknght.app-lib.inflection :refer [humanize]]
            [dgknght.app-lib.core :as lib])
  #?(:clj (:import [clojure.lang
                    Keyword
                    PersistentVector
                    PersistentArrayMap]
                   [clojure.core.async.impl.channels ManyToManyChannel])))

(defn ->caption
  [field]
  (let [humanized (humanize (last field))]
    (if-let [trimmed (re-find #"^.+(?= id$)" humanized)]
      trimmed
      humanized)))

(defn- format
  [msg & args]
  #?(:cljs (apply gstr/format msg args)
     :clj (apply clojure.core/format msg args)))

(defn add-rules
  [model field rules]
  (swap! model vary-meta assoc-in [::validation ::rules field] rules))

(defn get-rules
  ([model]
   (if (lib/derefable? model)
     (get-rules @model)
     (get-in  (meta model)
             [::validation ::rules])))
  ([model field]
   (if (lib/derefable? model)
     (get-rules @model field)
     (get-in (meta model)
             [::validation ::rules field]))))

(defn validated?
  ([model]
   (if (lib/derefable? model)
     (validated? @model)
     (get-in model [::validation ::validated?])))
  ([model value]
   (swap! model assoc-in [::validation ::validated?] value)))

(defn- format-validation-msg
  [errors]
  (when-let [errs (seq errors)]
    (string/join " " errs)))

(defn validation-msg
  ([model] ; model is a getter for all fields
   (if (lib/derefable? model)
     (validation-msg @model)
     (->> (get-in model [::validation ::messages])
          (mapcat vals)
          format-validation-msg)))
  ([model field] ; model and field is a getter for all validations for the field
   (if (lib/derefable? model)
     (validation-msg @model field)
     (format-validation-msg
       (vals (get-in model [::validation ::messages field])))))
  ([model field rule-key] ; model, field, and rule key is a getter for the validator for the field
   (if (lib/derefable? model)
     (validation-msg @model field rule-key)
     (format-validation-msg (get-in model [::validation ::messages field rule-key]))))
  ([model field rule-key msg] ; model, field, rule key & msg is a setter
   (if msg
     (swap! model
            assoc-in
            [::validation ::messages field rule-key]
            (format msg (->caption field)))
     (swap! model
            update-in
            [::validation ::messages field]
            dissoc
            rule-key))))

(defn- val-xf
  [field val-key pred? msg]
  (map (fn [model]
         (if (pred? @model)
           (validation-msg model field val-key nil)
           (validation-msg model field val-key msg))
         model)))

(def ^:private validation-rules
  {::required (fn [field & _]
                (val-xf field
                        ::required
                        #(lib/present? (get-in % field))
                        "%s is required."))
   ::format (fn [field pattern]
              (val-xf field
                      ::format
                      #(if-let [v (get-in % field)]
                         (re-find pattern v)
                         true)
                      "%s is not well-formed."))
   ::length (fn [field {:keys [min max]}]
              (let [msg (cond
                          (and min max) (format "%%s must be between %s and %s characters." min max)
                          min (format "%%s cannot be less than %s characters." min)
                          max (format "%%s cannot be more than %s characters." max))]
              (val-xf field
                      ::length
                      #(if-let [v (get-in % field)]
                         (<= (or min 1)
                             (count v)
                             (or max 999))
                         true)
                      msg)))
   ::matches (fn [field other-field]
               (val-xf field
                       ::matches
                       (fn [m]
                         (pprint {:field (get-in m field)
                                  :other-field (get-in m other-field)
                                  := (= (get-in m field)
                                        (get-in m other-field))})
                         (= (get-in m field)
                            (get-in m other-field)))
                       (format "%%s must match %s."
                               (->caption other-field))))})

(defn valid?
  [model]
  (if (lib/derefable? model)
    (valid? @model)
    (and (validated? model)
         (->> (get-in model [::validation ::messages])
              (mapcat second)
              empty?))))

(defmulti rule-xf (fn [r _]
                    (type r)))

(defmethod rule-xf :default
  [k field]
  (println (prn-str {::no-rule k
                     ::field field})))

; a registered validator with no params
(defmethod rule-xf Keyword
  [k field]
  (if-let [f (get-in validation-rules [k])]
    (f field)
    (println (format "Unable to find validation rule %s" k))))

; a registered validator with params
(defmethod rule-xf PersistentVector
  [[k & args] field]
  (let [f (get-in validation-rules [k])]
    (apply f field args)))

; an unregistered validator (passes a pred fn)
(defmethod rule-xf PersistentArrayMap
  [{:keys [pred message] val-key :key} field]
  (fn [xf]
    (completing
      (fn [ch model]
        (let [result (pred @model field)
              f (fn [valid?]
                  (if valid?
                    (validation-msg model field val-key nil)
                    (validation-msg model field val-key message)))]
          (if (= ManyToManyChannel
                 (type result))
            (a/go
              (f (a/<! result))
              (xf ch model))
            (do
              (f result)
              (xf ch model))))))))

; Validation should follow this guidelines:
;  1. Give positive feedback (remove negative feedback) on blur
;  2. Always validate on blur, but don't show negative feedback until submit
(defn validator
  [field rules]
  {:pre [(vector? field) (set? rules)]}

  (when (seq rules)
    (apply comp (->> rules
                     (map #(rule-xf % field))
                     (filter identity)))))

(defn validation-handler
  [rules model field]
  (if (seq rules)
    (let [ch (a/chan 1 (validator field rules))]
      (a/go-loop [m (a/<! ch)]
                 (when m
                   (recur (a/<! ch))))
      (fn [_e]
        (a/go (a/>! ch model))))
    identity))

(defn set-custom-validity
  ([event msg]
   (.setCustomValidity (.-currentTarget event) msg))
  ([event model field]
   (set-custom-validity event (validation-msg model field))))

(defn- validate*
  [model validator mark-validated?]
  (let [xf (if mark-validated?
             (comp validator
                   (map #(validated? % true)))
             validator)
        ch (a/promise-chan xf)]
    (a/put! ch model)
    ch))

(defn validate
  "Applies validation rules configured in the models and
  returns a channel from which the model can be retrieved post validation.

  The one argument arity also sets a flag in the data indicating
  that the entire model has been validated.

  If no validation rules are specified for the model, nil is returned."
  ([model]
   (if-let [all-rules (get-rules model)]
     (let [xf (apply comp
                     (->> all-rules
                          (filter second)
                          (map (fn [[field rules]]
                                 (validator field rules)))))]
       (validate* model xf true))
     (let [ch (a/promise-chan)]
       (a/put! ch (validated? model true))
       ch)))
  ([model field]
   (validate model field (get-rules model field)))
  ([model field rules]
   (when (seq rules)
     (validate* model (validator field rules) false))))
