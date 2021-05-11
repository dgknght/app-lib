(ns dgknght.app-lib.authorization
  (:require [stowaway.core :as storage]))

(derive ::create ::manage)
(derive ::show ::manage)
(derive ::update ::manage)
(derive ::destroy ::manage)

(defmulti allowed?
  "Returns a truthy or falsey value indicating whether or not the
  authenticated user is allowed to perform the specified
  action on the specified model"
  (fn [model action _user]
    [(storage/tag model) action]))

(defn- type-of
  [model-or-keyword]
  (if (keyword? model-or-keyword)
    model-or-keyword
    (storage/tag model-or-keyword)))

(defmethod allowed? :default
  [model action _user]
  (throw (ex-info (format "No authorization rules defined for action %s on type %s"
                          action
                          (type-of model))
                  {:action action
                   :type (type-of model)})))

(defn opaque?
  [error]
  (::opaque? (ex-data error)))

(defn- auth-error
  [model action opaque?]
  (let [[msg err-type] (if opaque?
                         ["not found" ::not-found]
                         ["forbidden" ::forbidden])]
    (ex-info msg {:type err-type
                  :action action
                  :model (storage/tag model)
                  ::opaque? opaque?})))
(def denied? (complement allowed?))

(defn authorize
  "Raises an error if the current user does not have
  permission to perform the specified function.

  This function returns the model so that it can be threaded together
  with other left-threadable operations"
  [model action user]
  {:pre [model action user]}
  (if (allowed? model action user)
    model
    (throw (auth-error model action (not (allowed? model ::show user))))))

(defmulti scope
  "Returns a criteria structure limiting the scope
  of a query to that to which the specified user
  has access."
  (fn
    [model-type _user]
    model-type))

(defmethod scope :default
  [model-type _user]
  (throw (ex-info (format "No scope defined for type %s"
                          model-type)
                  {:type model-type})))

(defn +scope
  ([criteria user]
   (+scope criteria (storage/tag criteria) user))
  ([criteria model-type user]
   (if-let [s (scope model-type user)]
     (if (empty? criteria)
       s
       [:and criteria s])
     criteria)))
