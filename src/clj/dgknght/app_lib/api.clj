(ns dgknght.app-lib.api
  (:refer-clojure :exclude [update])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [ring.util.response :as res]
            [dgknght.app-lib.authorization :as auth]
            [dgknght.app-lib.validation :as v]
            [dgknght.app-lib.json-encoding]))

(defn response
  ([]
   (response nil))
  ([body]
   (response body (if body 200 204)))
  ([body status]
   (-> body
       res/response
       (res/status status))))

(defn creation-response
  [model]
  (when (v/has-error? model)
    (log/infof "Unable to save the model %s: %s"
                (meta model)
                (string/join ", " (v/flat-error-messages model))))
  (response model (if (and (map? model)
                           (v/has-error? model))
                    400
                    201)))

(defn update-response
  [model]
  (response model (if (and (map? model)
                             (v/has-error? model))
                      400
                      200)))

(def no-content
  (response))

(def bad-request
  (response {:message "bad request"} 400))

(def unauthorized
  (response {:message "unauthorized"} 401))

(def forbidden
  (response {:message "forbidden"} 403))

(def not-found
  (response {:message "not found"} 404))

(def teapot
  (response {:message "I'm a teapot"} 418))

(def unprocessable
  (response {:message "unprocessable"} 422))

(def internal-server-error
  (response {:message "internal server error"} 500))

(defn wrap-authentication
  "Wraps the handler with an authentication lookup, passing
  the request on to the handle step in the handler if the authentication
  function returns non-nil, otherwise returing an unauthenticated response.
  
  Options:
    :authentication-fn - A function accepting one argument (the request) and returning the authentication result, or nil"
  [handler {:keys [authenticate-fn]}]
  (fn [req]
    (if-let [authenticated (authenticate-fn req)]
      (handler (assoc req :authenticated authenticated))
      unauthorized)))
 
(defn- error-response
  [error]
  (log/errorf error "Unexpected error handling request")
  internal-server-error)

(defn wrap-api-exception
  [handler]
  (fn [req]
    (try (handler req)
         (catch clojure.lang.ExceptionInfo e
           (let [opaque? (auth/opaque? e)]
             (cond
               (nil? opaque?)
               (error-response e)

               opaque?
               not-found

               :else
               forbidden)))
         (catch Exception e
           (error-response e)))))
