(ns dgknght.app-lib.forms-validation-test
  (:require [cljs.test :refer-macros [deftest is async]]
            [cljs.core.async :as a]
            [dgknght.app-lib.validation-helpers :refer-macros [validate]]
            [dgknght.app-lib.forms-validation :as v]))

; validation workflow is like this:
; 1. When a field is rendered in a form, validation rules are specified, as
;    keys for registered validation rules, or maps for custom. The forms ns
;    will save this information in the model meta data.
; 2. When a field is changed, validation is performed on that field
;    (automatically in onBlur). Any validation failures encountered at this
;    step should not be rendered until after step #3 has occurred. Successes
;    can be indicated.
; 3. When a form is submitted, the entire model is validated (manually in
;    onSubmit). At this point, all successes and failures should be rendered

(deftest add-validation-to-a-model
  (let [model (atom {})
        field [:first-name]]
    (v/add-rules model field #{::v/required [::v/length {:min 20}]})
    (is (= (v/get-rules model field)
           #{::v/required [::v/length {:min 20}]})
        "The rules can be retrieved after being set.")))

(deftest validate-a-required-field-fail
  (async
    done
    (let [model (atom {})
          field [:first-name]]
      (validate model field #{::v/required}
                   (is (= "First name is required." (v/validation-msg model field))
                       "Validation error messages are included within the model.")
                   (done)))))

(deftest validate-a-required-field-pass
  (async
    done
    (let [model (atom {:first-name "John"})
          field [:first-name]]
      (validate model field #{::v/required}
                (is (nil? (v/validation-msg model field))
                    "No validation message is present in the model")
                (done)))))

(def ^:private email-pattern
  #"^\w+@\w+\.[a-z]{2,4}$")

(deftest validate-by-format-fail
  (async
    done
    (let [model (atom {:email "notvalid"})
          field [:email]]
      (validate model field #{[::v/format email-pattern]}
                (is (= "Email is not well-formed." (v/validation-msg model field))
                    "Validation error messages are included in the model.")
                (done)))))

(deftest validate-by-format-pass
  (async
    done
    (let [model (atom {:email "john@doe.com"})
          field [:email]]
      (validate model field #{[::v/format email-pattern]}
                (is (nil? (v/validation-msg model field))
                    "No validation message is present in the model.")
                (done)))))

(deftest validate-by-max-length-fail
  (async
    done
    (let [model (atom {:last-name "abcdef"})
          field [:last-name]]
      (validate model field #{[::v/length {:max 5}]}
                (is (= "Last name cannot be more than 5 characters." (v/validation-msg model field))
                    "Validation error messages are included in the model.")
                (done)))))

(deftest validate-by-max-length-pass
  (async
    done
    (let [model (atom {:last-name "abcde"})
          field [:last-name]]
      (validate model field #{[::v/length {:max 5}]}
                (is (nil? (v/validation-msg model field))
                    "No validation message is present in the model.")
                (done)))))

(deftest validate-by-min-length-fail
  (async
    done
    (let [model (atom {:middle-name "abcd"})
          field [:middle-name]]
      (validate model field #{[::v/length {:min 5}]}
                (is (= "Middle name cannot be less than 5 characters." (v/validation-msg model field))
                    "Validation error messages are included in the model.")
                (done)))))

(deftest validate-by-min-length-pass
  (async
    done
    (let [model (atom {:middle-name "abcde"})
          field [:middle-name]]
      (validate model field #{[::v/length {:min 5}]}
                (is (nil? (v/validation-msg model field))
                    "No validation message is present in the model.")
                (done)))))

(deftest validate-by-matching-field-fail
  (async
    done
    (let [model (atom {:password "mysecret"
                       :password-confirmation "mytypo"})
          field [:password-confirmation]]
      (validate model field #{[::v/matches [:password]]}
                (is (= "Password confirmation must match Password." (v/validation-msg model field))
                    "Validation error messages are included in the model.")
                (done)))))

(deftest validate-by-matching-field-pass
  (async
    done
    (let [model (atom {:password "mysecret"
                       :password-confirmation "mysecret"})
          field [:password-confirmation]]
      (validate model field #{[::v/matches [:password]]}
                (is (nil? (v/validation-msg model field))
                    "No validation message is present in the model.")
                (done)))))

(defn- async-unique-username?
  [model field]
  (let [ch (a/promise-chan)
        existing #{"johndoe" "janedoe"}
        unique? (complement existing)]
    (a/go
      ; database call would go here
      (a/put! ch (unique? (get-in model field))))
    ch))

(def ^:private async-unique-username-spec
  {:key ::unique
   :pred async-unique-username?
   :message "%s is already in use."})

(deftest validate-by-async-fn-fail
  (async
    done
    (let [model (atom {:username "johndoe"})
          field [:username]]
      (validate model field #{async-unique-username-spec}
                (is (= "Username is already in use." (v/validation-msg model field))
                    "The correct validate message is set on the model")
                (done)))))

(deftest validate-by-async-fn-pass
  (async
    done
    (let [model (atom {:username "newuser"})
          field [:username]]
      (validate model field #{async-unique-username-spec}
                (is (nil? (v/validation-msg model field))
                    "No validate message is present in the model")
                (done)))))

(defn- sync-unique-username?
  [model field]
  (not (#{"johndoe" "janedoe"} (get-in model field))))

(def ^:private sync-unique-username-spec
  {:key ::unique
   :pred sync-unique-username?
   :message "%s is already in use."})

(deftest validate-by-sync-fn-fail
  (async
    done
    (let [model (atom {:username "johndoe"})
          field [:username]]
      (validate model field #{sync-unique-username-spec}
                (is (= "Username is already in use." (v/validation-msg model field))
                    "The correct validate message is set on the model")
                (done)))))

(deftest validate-by-sync-fn-pass
  (async
    done
    (let [model (atom {:username "newuser"})
          field [:username]]
      (validate model field #{sync-unique-username-spec}
                (is (nil? (v/validation-msg model field))
                    "No validate message is present in the model")
                (done)))))

(deftest validate-whole-model
  (async
    done
    (let [model (atom {:first-name "J"})]
      (v/add-rules model [:first-name] #{::v/required [::v/length {:min 2}]})
      (v/add-rules model [:last-name] #{::v/required [::v/length {:min 2}]})
      (if-let [result (v/validate model)]
        (a/go
          (a/<! result)
          (is (v/validated? model) "The model indicates that it has been validated")
          (is (not (v/valid? model)) "The model indicates that it is not valid")
          (is (= "First name cannot be less than 2 characters." (v/validation-msg model [:first-name]))
              "The first name validation errors can be retrieved")
          (is (= "Last name is required." (v/validation-msg model [:last-name]))
              "The last name validation errors can be retrieved")
          (done))
        (do
          (is false "The function has incorrectly returned a nil value.")
          (done))))))

(deftest validate-whole-model-with-no-rules
  (async
    done
    (let [model (atom {:first-name "J"})]
      (if-let [result (v/validate model)]
        (a/go
          (a/<! result)
          (is (v/validated? model) "The model indicates that it has been validated")
          (is (v/valid? model) "The model indicates that it is valid")
          (done))
        (do
          (is false "The function has incorrectly returned a nil value.")
          (done))))))
