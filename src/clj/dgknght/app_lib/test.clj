(ns dgknght.app-lib.test
  (:require [clojure.test :refer [assert-expr do-report]]
            [clojure.data :refer [diff]]
            [clojure.string :as string]
            [clojure.data.zip :as zip]
            [clojure.data.zip.xml :refer [xml1->]]
            [clojure.zip :refer [xml-zip]]
            [clojure.pprint :refer [pprint]]
            [cheshire.core :as json]
            [crouton.html :as html]
            [lambdaisland.uri :refer [uri
                                      query-string->map]]
            [dgknght.app-lib.core :refer [prune-to
                                          safe-nth
                                          update-in-if]]
            [dgknght.app-lib.models :as models]
            [dgknght.app-lib.validation])
  (:import java.io.StringWriter))

(defn parse-html-body
  [{:keys [body html-body] :as response}]
  (if html-body
    response
    (assoc response :html-body (html/parse-string body))))

(defn parse-json-body
  [{:keys [body json-body] :as response}]
  {:pre [(or (= 204 (:status response))
             (string? (:body response)))]}

  (if json-body
    response
    (assoc response :json-body (json/parse-string body true))))

(defn report-msg
  [& msgs]
  (->> msgs
       (filter identity)
       (string/join ": ")))

; if the form has 3 elements, the 2nd is the unvalidated model
; and the 3rd is the spec.
; if the form has 2 elements, the 2nd is the already-validated model
(defmethod assert-expr 'valid?
  [msg form]
  (let [model (safe-nth form 1)
        spec (safe-nth form 2)]
    `(let [validated#  (if ~spec
                         (dgknght.app-lib.validation/validate ~model ~spec)
                         ~model)
           messages# (dgknght.app-lib.validation/error-messages validated#)]
       (do-report (merge {:expected {} :actual messages#}
                         (if (seq messages#)
                           {:type :fail :message (report-msg ~msg "Found unexpected errors")}
                           {:type :pass :message ~msg}))))))

(defmethod assert-expr 'invalid?
  [msg form]
  (let [model (safe-nth form 1)
        path (safe-nth form 2)
        expected-msg (safe-nth form 3)
        spec (safe-nth form 4)]
    `(let [validated# (if ~spec
                        (dgknght.app-lib.validation/validate ~model ~spec)
                        ~model)
           messages# (set (dgknght.app-lib.validation/error-messages validated# ~path))
           pass?# (if ~expected-msg
                    (messages# ~expected-msg)
                    (seq messages#))]
       (do-report (merge {:expected ~expected-msg :actual messages#}
                         (if pass?#
                           {:type :pass :message ~msg}
                           {:type :fail :message (report-msg ~msg "Didn't find the expected error message")}))))))

(defmacro assert-http-status
  "This macro should not be used directly. Instead use assert expresions such as http-success?"
  [expected-status msg form]
  (let [response (safe-nth form 1)]
    `(let [status# (get-in ~response [:status])]
       (do-report {:type (if (= ~expected-status status#)
                           :pass
                           :fail)
                   :message (report-msg ~msg
                                        (format "Expected HTTP status code %s, but found %s"
                                                ~expected-status
                                                status#))
                   :expected ~expected-status
                   :actual status#}))))

(defmethod assert-expr 'http-redirect-to?
  [msg form]
  (let [req (safe-nth form 1)
        target (safe-nth form 2)]
    `(let [res# ~req]
       (if (= 302 (:status res#))
         (if (= ~target
                (get-in res# [:headers "Location"]))
           (do-report {:type :pass
                       :message ~msg
                       :expected ~target
                       :actual (get-in res# [:headers "Location"])})
           (do-report {:type :fail
                       :message (format "Expected response to redirect to %s, but it didn't" ~target)
                       :expected ~target
                       :actual (get-in res# [:headers "Location"])}))
         (do-report {:type :fail
                     :message "Expected response to be a redirect, but it wasn't"
                     :expected 302
                     :actual (:status res#)})))))

(defmethod assert-expr 'http-success?
  [msg form]
  (let [response (safe-nth form 1)]
    `(do-report {:type (if (and (>= (:status ~response) 200)
                                (< (:status ~response) 300))
                         :pass
                         :fail)
                 :message (format "Expected an HTTP status code in the 200s, but found %s: %s"
                                  (:status ~response)
                                  ~msg)
                 :expected "20x"
                 :actual (:status ~response)})))

(defmethod assert-expr 'http-created?
  [msg form]
  `(assert-http-status 201 ~msg ~form))

(defmethod assert-expr 'http-no-content?
  [msg form]
  `(assert-http-status 204 ~msg ~form))

(defmethod assert-expr 'http-bad-request?
  [msg form]
  `(assert-http-status 400 ~msg ~form))

(defmethod assert-expr 'http-unauthorized?
  [msg form]
  `(assert-http-status 401 ~msg ~form))

(defmethod assert-expr 'http-forbidden?
  [msg form]
  `(assert-http-status 403 ~msg ~form))

(defmethod assert-expr 'http-not-found?
  [msg form]
  `(assert-http-status 404 ~msg ~form))

(defmethod assert-expr 'http-teapot?
  [msg form]
  `(assert-http-status 418 ~msg ~form))

(defmethod assert-expr 'http-unprocessable?
  [msg form]
  `(assert-http-status 422 ~msg ~form))

(defmethod assert-expr 'comparable?
  [msg form]
  (let [expected (safe-nth form 1)
        actual (safe-nth form 2)
        ks (->> form
                (drop 3)
                (into []))]
    `(let [expected# (if (seq ~ks)
                       (select-keys ~expected ~ks)
                       ~expected)
           actual# (prune-to ~actual expected#)
           result# (if (= expected# actual#)
                     :pass
                     :fail)
           diffs# (when (= :fail result#)
                    (clojure.data/diff expected# actual#))]
       (do-report
         {:expected expected#
          :actual actual#
          :message ~msg
          :type result#
          :diffs [[actual# (take 2 diffs#)]]}))))

(defn comparable-uri
  [input]
  (-> {}
      (into (uri input))
      (update-in-if [:query] query-string->map)))

(defn uri=
  [& args]
  (->> args
       (map comparable-uri)
       (apply =)))

(defn uri-diff
  [& args]
  (->> args
       (take 2)
       (map comparable-uri)
       (apply diff)))

(defn uri-diff-str
  [uri-1 uri-2]
  (let [out (StringWriter.)
        [missing extra] (uri-diff uri-1 uri-2)]
    (.write out "missing:\n")
    (pprint missing out)
    (.write out "extra:\n")
    (pprint extra out)
    (.toString out)))

(defmethod assert-expr 'url-like?
  [msg form]
  (let [expected (uri (safe-nth form 1))
        actual (safe-nth form 2)]
    `(let [actual# (uri ~actual)]
       (do-report {:expected (str ~expected)
                   :actual (str actual#)
                   :message (format "%s:\n%s"
                                    ~msg
                                    (uri-diff-str ~expected actual#))
                   :type (if (uri= ~expected actual#)
                           :pass
                           :fail)}))))

(defmethod assert-expr 'not-url-like?
  [msg form]
  (let [expected (uri (safe-nth form 1))
        actual (safe-nth form 2)]
    `(let [actual# (uri ~actual)]
       (do-report {:expected (str ~expected)
                   :actual (str actual#)
                   :message ~msg
                   :type (if (uri= ~expected actual#)
                           :fail
                           :pass)}))))

(defmethod assert-expr 'seq-of-maps-like?
  [msg form]
  (let [expected (safe-nth form 1)
        actual (safe-nth form 2)]
    `(let [actual# (map-indexed #(prune-to %2 (get-in ~expected [%1]))
                                ~actual)]
       (do-report {:expected ~expected
                   :actual actual#
                   :message ~msg
                   :type (if (= ~expected actual#)
                           :pass
                           :fail)}))))

(defmethod assert-expr 'seq-with-map-like?
  [msg form]
  (let [expected (safe-nth form 1)
        coll (safe-nth form 2)]
    `(let [coll# (map #(prune-to % ~expected) ~coll)
           found# (some #(= ~expected %)
                        coll#)]
       (do-report (merge {:expected ~expected
                          :actual (or found# coll#)}
                         (if found#
                           {:type :pass
                            :message ~msg}
                           {:type :fail
                            :message (report-msg ~msg
                                                 (format "Expected to find %s in collection %s"
                                                         ~expected
                                                         coll#))}))))))

(defmethod assert-expr 'seq-with-no-map-like?
  [msg form]
  (let [expected (safe-nth form 1)
        coll (safe-nth form 2)]
    `(let [found# (->> ~coll
                       (filter #(= ~expected (prune-to % ~expected)))
                       first)]
       (do-report (merge {:expected ~expected
                          :actual found#}
                         (if found#
                           {:type :fail
                            :message (report-msg ~msg
                                                 "Expected not to find a matching map, but did find one")}
                           {:type :pass
                            :message ~msg}))))))

(defmethod assert-expr 'seq-containing-model?
  [msg form]
  (let [coll (safe-nth form 1)
        expected (safe-nth form 2)]
    `(let [id# (models/->id ~expected)
           actual# (->> ~coll
                        (map models/->id)
                        set)]
       (do-report
         {:type (if (actual# id#) :pass :fail)
          :message (report-msg ~msg
                               (format "Expected to find model with :id %s, but didn't find it"
                                       id#))
          :expected id#
          :actual actual#}))))

(defmethod assert-expr 'seq-not-containing-model?
  [msg form]
  (let [coll (safe-nth form 1)
        expected (safe-nth form 2)]
    `(let [id# (models/->id ~expected)
           actual# (->> ~coll
                        (map models/->id)
                        set)]
       (do-report
         {:type (if (actual# id#) :fail :pass)
          :message (report-msg ~msg
                               (format "Expected not to find model with :id %s, but found it"
                                       id#))
          :expected id#
          :actual actual#}))))

(defmethod assert-expr 'mime-msg-containing?
  [msg form]
  (let [mime-msg (safe-nth form 1)
        content-type (safe-nth form 2)
        pattern (safe-nth form 3)]
    `(let [content# (->> (:body ~mime-msg)
                         (filter #(= ~content-type (:type %)))
                         (map :content)
                         first)]
       (do-report {:type (if (and content#
                                  (re-find ~pattern content#))
                           :pass
                           :fail)
                   :message (if content#
                              (report-msg ~msg (format "Expected to find \"%s\" in the %s content, but did not find it."
                                                       ~pattern
                                                       ~content-type))
                              (report-msg ~msg (format "Expected to find content type %s in the message, but did not find it."
                                                       ~content-type)))
                   :expected ~pattern
                   :actual content#}))))

(defmethod assert-expr 'http-response-with-cookie?
  [msg form]
  (let [response (safe-nth form 1)
        cookie-name (safe-nth form 2)
        expected (safe-nth form 3)]
    `(let [cookie# (->> (get-in ~response [:headers "Set-Cookie"])
                        (filter #(string/starts-with? % ~cookie-name))
                        first)
           actual# (when cookie# (re-find #"(?<==)[^;]+" cookie#))]
       (do-report {:type (if (= ~expected actual#)
                           :pass
                           :fail)
                   :message (if cookie#
                              (report-msg ~msg (format "Expected cookie \"%s\" to have value \"%s\", but found \"%s\""
                                                       ~cookie-name
                                                       ~expected
                                                       actual#))
                              (report-msg ~msg (format "Expected to find cookie \"%s\" in the response, but did not find it."
                                                       ~cookie-name)))
                   :expected ~expected
                   :actual actual#}))))

(defmethod assert-expr 'http-response-without-cookie?
  [msg form]
  (let [response (safe-nth form 1)
        cookie-name (safe-nth form 2)]
    `(let [cookie# (->> (get-in ~response [:headers "Set-Cookie"])
                        (filter #(string/starts-with? % ~cookie-name))
                        first)]
       (do-report {:type (if cookie#
                           :fail
                           :pass)
                   :message (report-msg ~msg (format "Expected not to find a cookie \"%s\", but found one."
                                                       ~cookie-name))
                   :expected "no cookie"
                   :actual cookie#}))))

(defmethod assert-expr 'html-response-with-content?
  [msg form]
  (let [response (safe-nth form 1)
        predicates (->> form
                        (drop 2))]
    `(let [f# (fn [x#]
                (apply xml1-> x# ~predicates))
           match# (some f#
                        (zip/descendants
                          (-> ~response
                              parse-html-body
                              :html-body
                              xml-zip)))]
       (do-report {:type (if match# :pass :fail)
                   :message (report-msg ~msg (format "Did not find the expected html content."))
                   :expected ~predicates
                   :actual "not found"}))))

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
