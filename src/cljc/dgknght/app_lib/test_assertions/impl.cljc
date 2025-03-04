(ns dgknght.app-lib.test-assertions.impl
  (:require [clojure.data :refer [diff]]
            [clojure.string :as string]
            #?(:clj [clojure.spec.alpha]
               :cljs [cljs.spec.alpha])
            #?(:clj [clojure.data.zip :as zip])
            #?(:clj [clojure.data.zip.xml :refer [xml1->]])
            #?(:clj [clojure.zip :refer [xml-zip]])
            #?(:clj [clojure.pprint :refer [pprint]]
               :cljs [cljs.pprint :refer [pprint]])
            #?(:clj [clj-time.format :as tf]
               :cljs [cljs-time.format :as tf])
            #?(:clj [clj-time.coerce :refer [to-date-time]]
               :cljs [cljs-time.coerce :refer [to-date-time]])
            [lambdaisland.uri :refer [uri
                                      query-string->map]]
            #?(:cljs [goog.string])
            #?(:cljs [goog.string.format])
            #?(:clj [dgknght.app-lib.test :refer [parse-html-body]])
            [dgknght.app-lib.models :as models]
            [dgknght.app-lib.core :refer [update-in-if
                                          prune-to
                                          safe-nth]])
  #?(:clj (:import java.io.StringWriter)))

#?(:cljs (def fmt goog.string/format)
   :clj (def fmt clojure.core/format))

(defn report-msg
  [& msgs]
  (->> msgs
       (filter identity)
       (string/join ": ")))

(defn comparable?
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
       {:expected expected#
        :actual actual#
        :message ~msg
        :type result#
        :diffs [[actual# (take 2 diffs#)]]})))

(defn seq-of-maps-like?
  [msg form]
  (let [expected (safe-nth form 1)
        actual (safe-nth form 2)]
    `(let [actual# (->> ~expected
                        (interleave ~actual)
                        (partition 2)
                        (mapv #(apply prune-to %)))
           same-count?# (= (count ~actual)
                           (count ~expected))
           result# (if (and same-count?#
                            (= ~expected actual#))
                     :pass
                     :fail)
           diffs# (when (= :fail result#)
                    (diff ~expected actual#))]
       {:expected ~expected
        :actual actual#
        :message (if same-count?#
                   ~msg
                   (fmt "%s: expected %s items, but found %s"
                        ~msg
                        (count ~expected)
                        (count ~actual)))
        :type result#
        :diffs [[actual# (take 2 diffs#)]]})))

(defn seq-with-map-like?
  [msg form]
  (let [expected (safe-nth form 1)
        coll (safe-nth form 2)]
    `(let [coll# (map #(prune-to % ~expected) ~coll)
           found# (some #(= ~expected %)
                        coll#)]
       (merge {:expected ~expected
               :actual (or found# coll#)}
              (if found#
                {:type :pass
                 :message ~msg}
                {:type :fail
                 :message (report-msg ~msg
                                      (fmt "Expected to find %s in collection %s"
                                           ~expected
                                           coll#))})))))

(defn seq-with-no-map-like?
  [msg form]
  (let [expected (safe-nth form 1)
        coll (safe-nth form 2)]
    `(let [found# (->> ~coll
                       (filter #(= ~expected (prune-to % ~expected)))
                       first)]
       (merge {:expected ~expected
               :actual found#}
              (if found#
                {:type :fail
                 :message (report-msg ~msg
                                      "Expected not to find a matching map, but did find one")}
                {:type :pass
                 :message ~msg})))))

(defn seq-containing-value?
  [msg form]
  (let [coll (safe-nth form 2)
        expected (safe-nth form 1)]
    `(let [actual# (set ~coll)]
       {:type (if (actual# ~expected) :pass :fail)
        :message (report-msg ~msg
                             (fmt "Expected to find value %s within, but didn't find it"
                                  ~expected
                                  actual#))
        :expected ~expected
        :actual actual#})))

(defn seq-excluding-value?
  [msg form]
  (let [coll (safe-nth form 2)
        expected (safe-nth form 1)]
    `(let [actual# (set ~coll)]
       {:type (if (actual# ~expected) :fail :pass)
        :message (report-msg ~msg
                             (fmt "Expected not to find value %s within, but found it"
                                  ~expected
                                  actual#))
        :expected ~expected
        :actual actual#})))

(defn seq-containing-model?
  [msg form]
  (let [coll (safe-nth form 2)
        expected (safe-nth form 1)]
    `(let [id# (models/->id ~expected)
           actual# (->> ~coll
                        (map models/->id)
                        set)]
       {:type (if (actual# id#) :pass :fail)
        :message (report-msg ~msg
                             (fmt "Expected to find model with :id %s, but didn't find it"
                                  id#))
        :expected id#
        :actual actual#})))

(defn seq-excluding-model?
  [msg form]
  (let [coll (safe-nth form 2)
        expected (safe-nth form 1)]
    `(let [id# (models/->id ~expected)
           actual# (->> ~coll
                        (map models/->id)
                        set)]
       {:type (if (actual# id#) :fail :pass)
        :message (report-msg ~msg
                             (fmt "Expected not to find model with :id %s, but found it"
                                  id#))
        :expected id#
        :actual actual#})))

; if the form has 3 elements, the 2nd is the unvalidated model
; and the 3rd is the spec.
; if the form has 2 elements, the 2nd is the already-validated model
(defn valid?
  [msg form]
  (let [model (safe-nth form 1)
        spec (safe-nth form 2)]
    `(let [validated#  (if ~spec
                         (dgknght.app-lib.validation/validate ~model ~spec)
                         ~model)
           messages# (dgknght.app-lib.validation/error-messages validated#)]
       (merge {:expected {} :actual messages#}
              (if (seq messages#)
                {:type :fail :message (report-msg ~msg "Found unexpected errors")}
                {:type :pass :message ~msg})))))

(defn invalid?
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
       (merge {:expected ~expected-msg :actual messages#}
              (if pass?#
                {:type :pass :message ~msg}
                {:type :fail :message (report-msg ~msg "Didn't find the expected error message")})))))

(defn http-redirect-to?
  [msg form]
  (let [target (safe-nth form 1)
        res (safe-nth form 2)]
    `(let [res# ~res]
       (assert (string? ~target) (str "The first agument must be a url. Found: " (prn-str ~target)))
       (assert (map? res#) (str "The second argument must be a response map. Found: " (prn-str res#)))
       (if (= 302 (:status res#))
         (if (= ~target
                (get-in res# [:headers "Location"]))
           {:type :pass
            :message ~msg
            :expected ~target
            :actual (get-in res# [:headers "Location"])}
           {:type :fail
            :message (fmt "Expected response to redirect to %s, but it didn't" ~target)
            :expected ~target
            :actual (get-in res# [:headers "Location"])})
         {:type :fail
          :message "Expected response to be a redirect, but it wasn't"
          :expected 302
          :actual (:status res#)}))))

(def guess-the-body
  (some-fn :edn-body
           :json-body
           :body))

(defn assert-http-status
  [expected-status msg form]
  (let [response (safe-nth form 1)]
    `(let [status# (get-in ~response [:status])
           msg# (some #(get-in ~response %)
                      [[:edn-body :error]
                       [:edn-body :dgknght.app-lib.validation/errors]
                       [:edn-body :message]
                       [:json-body :error]
                       [:json-body :message]
                       [:body :error]
                       [:body :message]
                       [:body]])]
       {:type (if (= ~expected-status status#)
                :pass
                :fail)
        :message (report-msg ~msg
                             (fmt "Expected HTTP status code %s, but found %s"
                                  ~expected-status
                                  status#))
        :expected ~expected-status
        :actual (fmt "%s: %s" status# msg#)})))

(defn http-success?
  [msg form]
  (let [response (safe-nth form 1)]
    `{:type (if (< 199 (:status ~response) 300)
              :pass
              :fail)
      :message (fmt "Expected an HTTP status code in the 200s, but found %s: %s"
                    (:status ~response)
                    ~msg)
      :expected "20x"
      :actual (fmt "%s: %s" (:status ~response) (guess-the-body ~response))}))

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
  (let [[missing extra] (uri-diff uri-1 uri-2)]
    #?(:clj
       (when-let [out (StringWriter.)] ; when-let stops the linter from complaining about redundant let
         (.write out "missing:\n")
         (pprint missing out)
         (.write out "extra:\n")
         (pprint extra out)
         (.toString out))
       :cljs
       (str "missing:\n"
            (prn-str missing)
            "extra:\n"
            (prn-str extra)))))

(defn url-like?
  [msg form]
  (let [expected (uri (safe-nth form 1))
        actual (safe-nth form 2)]
    `(let [actual# (uri ~actual)]
       {:expected (str ~expected)
        :actual (str actual#)
        :message (fmt "%s:\n%s"
                      ~msg
                      (uri-diff-str ~expected actual#))
        :type (if (uri= ~expected actual#)
                :pass
                :fail)})))

(defn not-url-like?
  [msg form]
  (let [expected (uri (safe-nth form 1))
        actual (safe-nth form 2)]
    `(let [actual# (uri ~actual)]
       {:expected (str ~expected)
        :actual (str actual#)
        :message ~msg
        :type (if (uri= ~expected actual#)
                :fail
                :pass)})))

(defn mime-msg-containing?
  [msg form]
  (let [mime-msg (safe-nth form 1)
        content-type (safe-nth form 2)
        pattern (safe-nth form 3)]
    `(let [content# (->> (:body ~mime-msg)
                         (filter #(= ~content-type (:type %)))
                         (map :content)
                         first)]
       {:type (if (and content#
                       (re-find ~pattern content#))
                :pass
                :fail)
        :message (if content#
                   (report-msg ~msg (fmt "Expected to find \"%s\" in the %s content, but did not find it."
                                         ~pattern
                                         ~content-type))
                   (report-msg ~msg (fmt "Expected to find content type %s in the message, but did not find it."
                                         ~content-type)))
        :expected ~pattern
        :actual content#})))

(defn mime-msg-not-containing?
  [msg form]
  (let [mime-msg (safe-nth form 1)
        content-type (safe-nth form 2)
        pattern (safe-nth form 3)]
    `(let [content# (->> (:body ~mime-msg)
                         (filter #(= ~content-type (:type %)))
                         (map :content)
                         first)]
       {:type (if (and content#
                       (re-find ~pattern content#))
                :fail
                :pass)
        :message (if content#
                   (report-msg ~msg (fmt "Expected not to find \"%s\" in the %s content, but found it."
                                         ~pattern
                                         ~content-type))
                   (report-msg ~msg (fmt "Expected to find content type %s in the message, but did not find it."
                                         ~content-type)))
        :expected ~pattern
        :actual content#})))

(defn http-response-with-cookie?
  [msg form]
  (let [cookie-name (safe-nth form 1)
        expected (safe-nth form 2)
        response (safe-nth form 3)]
    `(let [cookie# (->> (get-in ~response [:headers "Set-Cookie"])
                        (filter #(string/starts-with? % ~cookie-name))
                        first)
           actual# (when cookie# (re-find #"(?<==)[^;]+" cookie#))]
       {:type (if (= ~expected actual#)
                :pass
                :fail)
        :message (if cookie#
                   (report-msg ~msg (fmt "Expected cookie \"%s\" to have value \"%s\", but found \"%s\""
                                         ~cookie-name
                                         ~expected
                                         actual#))
                   (report-msg ~msg (fmt "Expected to find cookie \"%s\" in the response, but did not find it."
                                         ~cookie-name)))
        :expected ~expected
        :actual actual#})))

(defn http-response-without-cookie?
  [msg form]
  (let [response (safe-nth form 1)
        cookie-name (safe-nth form 2)]
    `(let [cookie# (->> (get-in ~response [:headers "Set-Cookie"])
                        (filter #(string/starts-with? % ~cookie-name))
                        first)]
       {:type (if cookie#
                :fail
                :pass)
        :message (report-msg ~msg (fmt "Expected not to find a cookie \"%s\", but found one."
                                       ~cookie-name))
        :expected "no cookie"
        :actual cookie#})))

#?(:clj
   (defn html-response-with-content?
     [msg form]
     (let [response (safe-nth form 1)
           predicates (drop 2 form)]
       `(let [f# (fn [x#]
                   (apply xml1-> x# ~predicates))
              match# (some f#
                           (zip/descendants
                             (-> ~response
                                 parse-html-body
                                 :html-body
                                 xml-zip)))]
          {:type (if match# :pass :fail)
           :message (report-msg ~msg "Did not find the expected html content.")
           :expected ~predicates
           :actual "not found"}))))

(defn format-comparable-date
  [d]
  (tf/unparse (tf/formatters :date) (to-date-time d)))

#?(:clj
   (defn same-date?
     [msg form]
     (let [expected (safe-nth form 1)
           actual (safe-nth form 2)]
       `(let [expected# (format-comparable-date ~expected)
              actual# (format-comparable-date ~actual)]
          {:type (if (= expected# actual#) :pass :fail)
           :message (report-msg ~msg (fmt "Expected \"%s\", but found \"%s\"" expected# actual#))
           :expected expected#
           :actual actual#}))))

(defn conformant?
  [msg form valid? explain]
  (let [spec (safe-nth form 1)
        value (safe-nth form 2)]
    `(let [expected# (str "Conformance with " ~spec)]
       {:expected expected#
        :actual (~explain ~spec ~value)
        :message ~msg
        :type (if (~valid? ~spec ~value)
                :pass
                :fail)})))

(defn ex-msg
  [e]
  #?(:clj (.getMessage e)
     :cljs (.-message e)))

(defn thrown-with-ex-data?
  [msg form]
  (let [expected-msg (safe-nth form 1)
        expected-data (safe-nth form 2)
        body (drop 3 form)]
    `(try
       ~@body
       {:type :fail
        :expected {:ex-data ~expected-data
                   :ex-message ~expected-msg}
        :actual "no exception was thrown"
        :message ~msg}
       (catch Exception e#
         (let [actual-data# (ex-data e#)
               actual-msg# (ex-message e#)]
           {:expected {:ex-data ~expected-data
                       :ex-message ~expected-msg}
            :actual {:ex-data actual-data#
                     :ex-message actual-msg#}
            :message ~msg
            :type (if (and (= ~expected-data actual-data#)
                           (= ~expected-msg actual-msg#))
                    :pass
                    :fail)})))))
