(ns dgknght.app-lib.validation-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [dgknght.app-lib.web :refer [email-pattern]]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.validation :as v]))

(def present? (every-pred string? seq))
(v/reg-msg present? "%s is required")

(s/def ::name present?)
(s/def ::test-model (s/keys :req-un [::name]))

(deftest validate-a-required-field
  (testing "the value is present"
    (is (valid? {:name "Doug"} ::test-model)))
  (testing "the value is absent"
    (is (invalid? {} [:name] "Name is required" ::test-model)))
  (testing "the value is nil"
    (is (invalid? {:name nil} [:name] "Name is required" ::test-model)))
  (testing "the value is empty"
    (is (invalid? {:name ""} [:name] "Name is required" ::test-model))))

(s/def ::description (partial v/not-longer-than? 20))
(s/def ::max-length-model (s/keys :req-un [::description]))

(defn- mkstr
  [base repititions]
  (->> (repeat base)
       (take repititions)
       (string/join "")))

(deftest validate-a-max-length
  (is (valid? {:description (mkstr "x" 20)} ::max-length-model))
  (is (invalid? {:description (mkstr "x" 21)}
                [:description]
                "Description cannot be more than 20 characters"
                ::max-length-model)))

(s/def ::password (partial v/length-between? 10 100))
(s/def ::password-model (s/keys :req-un [::password]))

(deftest validate-a-length-between
  (is (invalid? {:password (mkstr "x" 9)}  [:password] "Password must be between 10 and 100 characters" ::password-model))
  (is (valid?   {:password (mkstr "x" 10)}  ::password-model))
  (is (valid?   {:password (mkstr "x" 100)} ::password-model))
  (is (invalid? {:password (mkstr "x" 101)} [:password] "Password must be between 10 and 100 characters" ::password-model)))

(def email? (partial re-matches email-pattern))
(v/reg-msg email? "%s must be a valid email address")

(s/def ::email email?)
(s/def ::email-model (s/keys :req-un [::email]))

(deftest validate-an-email-field
  (testing "a valid email"
    (is (valid? {:email "john@doe.com"} ::email-model)))
  (testing "an invalid email"
    (is (invalid? {:email "notanemail"}
                  [:email]
                  "Email must be a valid email address"
                  ::email-model))))

(defn- username-unique?
  [model]
  (not= "jdoe" (get-in model [:username])))

(v/reg-spec username-unique? {:message "%s is already in use"
                              :path [:username]})

(s/def ::username string?)
(s/def ::unique-model (s/and (s/keys :req-un [::username])
                             username-unique?))

(deftest validate-with-a-custom-rule
  (is (valid? {:username "dknght"} ::unique-model))
  (is (invalid? {:username "jdoe"}
                [:username]
                "Username is already in use"
                ::unique-model)))

(s/def ::size #{:small :large})
(s/def ::size-model (s/keys :req-un [::size]))

(deftest validate-against-a-set-of-allowed-values
  (is (valid? {:size :small} ::size-model))
  (is (valid? {:size :large} ::size-model))
  (is (invalid? {:size :medium} [:size] "Size must be large or small" ::size-model)))

(s/def ::type #{:simple :compound})
(s/def ::value string?)
(s/def ::extra string?)
(defmulti optional-spec :type)
(defmethod optional-spec :default [_]
  (s/keys :req-un [::type ::value]))
(defmethod optional-spec :compound [_]
  (s/keys :req-un [::type ::value ::extra]))

(s/def ::optional-model (s/multi-spec optional-spec :type))

(deftest validate-with-a-multi-spec
  (is (valid? {:type :simple :value "one"} ::optional-model))
  (is (valid? {:type :compound :value "one" :extra "two"} ::optional-model))
  (is (invalid? {:type :compound :value "one"}
                [:extra]
                "Extra is required"
                ::optional-model))
  #_(is (invalid? {:type :compound :value "one" :extra 2}
                [:extra]
                "Extra must be a string"
                ::optional-model)))

(s/def ::index integer?)
(s/def ::item (s/keys :req-un [::index]))
(s/def ::items (s/coll-of ::item :min-count 2))
(s/def ::collection-model (s/keys :req-un [::items]))

(deftest validate-a-collection-attribute
  (is (valid? {:items [{:index 1}
                       {:index 2}]}
              ::collection-model))
  (is (invalid? {:items [{:index 1}]}
                [:items]
                "Items must contain at least 2 item(s)"
                ::collection-model))
  (is (invalid? {:items [{:index 1}
                         {}]}
                [:items 1 :index]
                "Index is required"
                ::collection-model))
  (is (invalid? {:items [{:index 1}
                         {:index "two"}]}
                [:items 1 :index]
                "Index must be an integer"
                ::collection-model)))

(deftest test-for-presence-of-an-error
  (let [model-with-error {::v/errors {:first-name ["is required"]}
                          :last-name "Doe" }]
    (testing "The entire model"
      (is (v/has-error? model-with-error))
      (is (not (v/has-error? {})))
      (is (not (v/has-error? {::v/errors {}}))))
    (testing "A single attribute"
      (is (v/has-error? model-with-error :first-name))
      (is (v/has-error? model-with-error [:first-name]))
      (is (not (v/has-error? model-with-error :last-name)))
      (is (not (v/has-error? model-with-error [:last-name]))))))

(deftest test-for-validity
  (let [model-with-error {::v/errors {:first-name ["is required"]}
                          :last-name "Doe" }]
    (testing "The entire model"
      (is (= false (v/valid? model-with-error)))
      (is (nil? (v/valid? {})))
      (is (v/valid? {::v/errors {}})))))

(deftest test-a-minimum-length
  (is (v/min-length? 5 "abcde"))
  (is (not (v/min-length? 5 "abcd"))))

(deftest extra-error-messages
  (let [model {:first-name "John"
               ::v/errors {:last-name ["is required"]}}]
    (is (= ["is required"] (v/error-messages model :last-name)))
    (is (= ["is required"] (v/error-messages model [:last-name])))))

(deftest wrap-validation-in-a-macro
  (testing "a valid model"
    (let [model {:name "John"}
          body-called? (atom false)
          res (v/with-validation model ::test-model
                (reset! body-called? true)
                ::result)]
      (is (= ::result res) "The result of the body is returned")
      (is @body-called? "The block is called with a valid model")))
  (testing "an invalid model without throwing an exception"
    (let [model {}
          body-called? (atom false)
          res (v/with-validation model ::test-model
                (reset! body-called? true))]
      (is (v/has-error? res) "The result indicates invalid")
      (is (not @body-called?) "The block is not called with a valid model")))
  (testing "an invalid model and throw an exception"
    (let [model {}]
      (try
        (v/with-ex-validation model ::test-model
          (is false "The body should not be called"))
        (catch clojure.lang.ExceptionInfo e
          (is (= {:errors {:name ["Name is required"]}}
                 (ex-data e))
              "The exception contains the expected data"))))))
