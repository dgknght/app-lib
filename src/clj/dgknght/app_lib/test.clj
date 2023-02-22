(ns dgknght.app-lib.test
  (:require [cheshire.core :as json]
            [crouton.html :as html]
            [dgknght.app-lib.core :refer [safe-nth]]
            [dgknght.app-lib.validation])
  (:import com.fasterxml.jackson.core.JsonParseException))

(defn parse-html-body
  [{:keys [body html-body] :as response}]
  {:pre [(map? response)]}

  (if html-body
    response
    (assoc response :html-body (html/parse-string body))))

(defn parse-json-body
  [{:keys [body json-body] :as response}]
  {:pre [(map? response)
         (or (= 204 (:status response))
             (string? (:body response)))]}

  (if json-body
    response
    (try
      (assoc response :json-body (json/parse-string body true))
      (catch JsonParseException e
        (assoc response :json-body {:parse-error (.getMessage e)})))))

(defmacro with-mail-capture
  "Intercepts calls to postal.core/send-message and places them in an
  atom that can be expected after running the code under test.

  The bindings are in the first argument and contain:
    arg-name  - the name of the binding for the atom containing the intercepted messages
    fn-name   - (optional) the name of the function to redef. Defaults to postal.core/send-messages)
    arg-index - (optional) the index of the argument passed to the redeffed function that contains the message to be intercepted"
  [bindings & body]
  (let [arg-index (or (safe-nth bindings 2) 1)]
    `(let [messages# (atom [])
           f# (fn* [~(first bindings)] ~@body)]
       (with-redefs [~(if (< 1 (count bindings))
                        (symbol (resolve (second bindings)))
                        'postal.core/send-message)
                     (fn [& args#]
                       (swap! messages#
                              conj
                              (nth args# ~arg-index)))]
         (f# messages#)))))

(defmulti ^:private multipart-section
  #(when (contains? % :file-name)
     :file))

(defmethod ^:private multipart-section :default
  [{:keys [name value]}]
  (format "Content-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n"
          name
          value))

(defmethod ^:private multipart-section :file
  [{:keys [name file-name content-type body]}]
  (format "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\nContent-Type: %s\r\n\r\n%s"
          name
          file-name
          content-type
          body))

(defn multipart-body
  "Creates and assoces a multipart body onto a a mock request.

    (multipart-body req {:name \"normal-field\"
                         :value \"literal content\"}
                        {:name \"file-field-1\"
                         :file-name \"data.csv\"
                         :content-type \"text/csv\"
                         :body csv-data-2}
                        {:name \"file-field-2\"
                         :file-name \"more-data.csv\"
                         :content-type \"text/csv\"
                         :body csv-data-2})"
  [request & bodies]
  (let [boundary "TestRequestFormBoundary"
        body (str (apply str (interleave (repeat (str "--" boundary "\r\n"))
                                         (map multipart-section bodies)))
                  "--" boundary "--\r\n")]
    (-> request
        (assoc :body (java.io.StringBufferInputStream. body))
        (assoc-in [:headers "content-type"] (str "multipart/form-data;boundary=\"" boundary "\"")))))

(defn user-agent
  "Adds a user agent header to the request"
  [req user-agent]
  (update-in req [:headers] assoc "user-agent" user-agent))

(defn attr-set
  "Given a list of maps, return a set containing the
  values at the specified attribute for each map"
  [attr ms]
  (when (sequential? ms)
    (->> ms
         (map #(get-in % [attr]))
         set)))

(defmacro with-log-capture
  [bindings & body]
  `(let [logged# (atom [])
         f# (fn* [~(first bindings)]
                 ~@body)]
     (with-redefs [clojure.tools.logging/log*
                   (fn [& args#]
                     (swap! logged# conj args#))]
       (f# logged#))))
