(ns dgknght.app-lib.forms
  (:require [reagent.core :as r]
            [reagent.ratom :refer [make-reaction]]
            [goog.string :as gstr]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [dgknght.app-lib.forms-validation :as v]
            [dgknght.app-lib.core :as lib]
            [dgknght.app-lib.web :refer [format-time
                                         unformat-time]]
            [dgknght.app-lib.math :as math]
            [dgknght.app-lib.inflection :refer [humanize]]
            [dgknght.app-lib.dom :as dom]
            [dgknght.app-lib.bootstrap-icons :as icons]
            [dgknght.app-lib.calendar :as cal]
            [dgknght.app-lib.calendar-view :as calview]
            [dgknght.app-lib.forms.common :refer [->id
                                                  ->name]]
            [dgknght.app-lib.forms.typeahead :as typeahead]))

(def defaults (atom {}))

(derive ::password ::text)
(derive ::email ::text)
(derive ::number ::text)
(derive ::select ::text)
(derive ::textarea ::text)

(defn ->caption
  [field]
  (let [humanized (humanize (last field))]
    (if-let [trimmed (re-find #"^.+(?= id$)" humanized)]
      trimmed
      humanized)))

(defn- infer-target
  [[tag attr]]
  (keyword "dgknght.app-lib.forms"
                            (name (if (= :input tag)
                                    (:type attr)
                                    tag))))

(defn- extract-target
  [options elem]
  (get-in options
          [::decoration ::target]
          (infer-target elem)))

(defn- extract-presentation
  [options _elem]
  (get-in options
          [::decoration ::presentation]
          ::element))

(defn- extract-framework
  [options & _]
  (let [ks [::decoration ::framework]]
    (or (get-in options ks)
        (get-in @defaults ks))))

(def ^:private extract-dispatch
  (juxt extract-target
        extract-presentation
        extract-framework))

(defmulti decorate
  (fn [elem _model _field _errors options]
    (when-not (= ::none (::decoration options))
      (extract-dispatch options elem))))

(defmulti spinner extract-framework)

(defmethod decorate :default
  [elem _model field _errors {::keys [decoration] :as options}]
  (when-not (= ::none decoration)
    (.warn js/console (prn-str {:unhandled-decoration decoration
                                :elem elem
                                :target (extract-target options elem)
                                :field field})))
  elem)

(defn- input-class
  [model field]
  (get-in model [::input-classes field]))

(defn checkbox-input
  ([model field] (checkbox-input model field {}))
  ([model field {:keys [on-change
                        value
                        html]
                 :or {on-change identity}
                 :as options}]
   (let [value-atm (r/cursor model field)
         checked (if value
                   (make-reaction #(contains? @value-atm value))
                   (make-reaction #(boolean @value-atm)))
         accept (if value
                  #(swap! value-atm (fn [v]
                                      (let [v-set (cond
                                                    (set? v) v
                                                    (nil? v) #{}
                                                    :else #{v})]
                                        (if (dom/checked? %)
                                          (conj v-set value)
                                          (disj v-set value)))))
                  #(reset! value-atm (dom/checked? %)))
         id (if value
              (str (->id field) "-" value)
              (->id field))]
     (fn []
       (decorate
         [:input (merge {:id id}
                        html
                        {:type :checkbox
                         :checked @checked
                         :value value
                         :on-change (fn [e]
                                      (accept e)
                                      (on-change @model))})]
         model
         field
         [] ; TODO: do we ever validate a checkbox?
         options)))))

(defn checkbox-field
  ([model field]
   (checkbox-field model field {}))
  ([model field options]
   (checkbox-input model
                   field
                   (assoc-in options [::decoration ::presentation] ::field))))

(defn- ensure-value-label-pair
  [v]
  (if (sequential? v)
    v
    [v (humanize v)]))

(defn- checkbox-inputs*
  [model field items {:keys [id-prefix] :as options}]
  (->> items
       (map ensure-value-label-pair)
       (map-indexed (fn [index [value label]]
                      (let [id (str id-prefix (->id field) "-checkbox-" index)]
                        (-> [:input {:type :checkbox
                                     :id id
                                     :checked (contains? (get-in @model field) value)
                                     :on-change (fn [e]
                                                  (if (dom/checked? e)
                                                    (swap! model update-in field (fnil conj #{}) value)
                                                    (swap! model update-in field disj value)))
                                     :value value}]
                            (decorate model field []  (-> options
                                                          (assoc-in [::decoration ::presentation] ::inline-field)
                                                          (assoc :caption label)))
                            (with-meta {:key id})))))
       doall))

(defn checkbox-inputs
  [model field items {:keys [container-html] :as options}]
  [:div
   (merge {} container-html)
   (checkbox-inputs* model
                     field
                     (if (lib/derefable? items)
                       @items
                       items)
                     options)])

(defn checkboxes-field
  "Renders a list of checkboxes that behavior like a multi-select select element."
  ([model field items] (checkboxes-field model field items {}))
  ([model field items options]
   (fn []
     [:div.form-group
      [:label.control-label (or (:caption options)
                                (->caption field))]
      [:br]
      (checkbox-inputs* model field items options)])))

(defn text-input
  [model field {:keys [html on-change validations]
                input-type :type
                :or {input-type :text
                     on-change identity}
                :as options}]
  (let [value (r/cursor model field)
        errors (make-reaction #(v/validation-msg model field))]
    (v/add-rules model field validations)
    (fn []
      (decorate
        [:input (merge {:id (->id field)}
                       html
                       {:type input-type
                        :name (->name field)
                        :value @value
                        :on-blur (fn [e]
                                   (v/validate model field)
                                   (v/set-custom-validity e @model field))
                        :on-change (fn [e]
                                     (let [new-value (-> e dom/target dom/value)]
                                       (swap! model assoc-in field new-value)
                                       (on-change new-value)))})]
        model
        field
        @errors
        options))))

(defn text-field
  [model field options]
  (fn []
    [text-input model
                field
                (assoc-in options [::decoration ::presentation] ::field)]))

(defn- update-field
  ([model field e] (update-field model field e {}))
  ([model field e {:keys [transform-fn]
                   :or {transform-fn identity}}]
   (swap! model assoc-in field (-> e .-target .-value transform-fn))))

(defn textarea-elem
  ([model field] (textarea-elem model field {}))
  ([model field {:keys [validations] :as options}]
   (let [changed? (r/atom false)
         value (r/cursor model field)
         errors (make-reaction #(v/validation-msg model field))]
     (v/add-rules model field validations)
     (fn []
       (decorate
         [:textarea
          (merge
            (:html options)
            {:id (->id field)
             :value @value
             :class (input-class @model field)
             :on-change (fn [e]
                          (reset! changed? true)
                          (update-field model field e))
             :on-blur #(when (or @changed? (nil? @value))
                         (v/validate model field))})]
         model
         field
         @errors
         options)))))

(defn textarea-field
  ([model field] (textarea-field model field {}))
  ([model field options]
   (textarea-elem model field (assoc-in options [::decoration ::presentation] ::field))))

(defn- one?
  [coll]
  (= 1 (count coll)))

(defmulti ^:private nilify
  (fn [model _field]
    (type model)))

(defmethod nilify PersistentVector
  [model field]
  (if (one? field)
    (assoc-in model field nil)
    (update-in model (take 1 field) nilify (rest field))))

(defmethod nilify :default
  [model field]
  (if (one? field)
    (dissoc model (first field))
    (update-in model (take 1 field) nilify (rest field))))

(defn email-field
  ([model field]
   (email-field model field {}))
  ([model field options]
   (text-field model field (merge options {:type :email
                                           :html {:auto-complete "email"}}))))

(defn password-field
  ([model field {:keys [new?] :as options}]
   (text-field
     model
     field
     (merge options {:type :password
                     :html {:auto-complete (if new?
                                             "new-password"
                                             "current-password")}}))))

(defn- specialized-text-input
  [model field {input-type :type
                :keys [parse-fn
                       unparse-fn
                       equals-fn
                       disabled-fn
                       on-accept
                       validations
                       html]
                :or {input-type :text
                     equals-fn =
                     disabled-fn (constantly false)
                     unparse-fn str
                     on-accept identity}
                :as options}]
  (let [text-value (r/atom (unparse-fn (get-in @model field)))
        errors (make-reaction #(v/validation-msg model field))]
    (v/add-rules model field validations)
    (add-watch model
               (cons ::specialized-text-input field)
               (fn [_field _sender before after]
                 (let [b (get-in before field)
                       a (get-in after field)]
                   (when-not (equals-fn a b)
                     (reset! text-value (unparse-fn a))))))
    (fn []
      (let [attr (merge {:id (->id field)}
                        html
                        {:type input-type
                         :auto-complete :off
                         :disabled (disabled-fn)
                         :name (->name field)
                         :value @text-value
                         :on-blur (fn []
                                    (when-let [new-value (get-in @model field)]
                                      (let [formatted (unparse-fn new-value)]
                                        (when-not (= formatted @text-value)
                                          (reset! text-value (unparse-fn new-value))))))
                         :on-change (fn [e]
                                      (let [new-value (.-value (.-target e))
                                            parsed (try
                                                     (parse-fn new-value)
                                                     (catch js/Error e
                                                       (.dir js/console e)
                                                       (.log js/console
                                                             (gstr/format "Error parsing \"%s\": (%s) %s"
                                                                          new-value
                                                                          (.-name e)
                                                                          (.-message e)))
                                                       nil))]
                                        (if (seq new-value)
                                          (when parsed
                                            (swap! model assoc-in field parsed)
                                            (v/validate model field)
                                            (on-accept))
                                          (do
                                            (swap! model nilify field)
                                            (v/validate model field)))
                                        (reset! text-value new-value)))})]
        (decorate [:input attr] model field @errors options)))))

(defn- parse-full-date
  [date-string]
  (when
    (re-find #"^\d{1,2}/\d{1,2}/\d{4}$" date-string)
    (tf/parse (tf/formatter "M/d/yyyy") date-string)))

(defn- days-between
  [& dates]
  (t/in-days (apply t/interval (sort t/before? (take 2 dates)))))

(defn- parse-month-day
  [date-string]
  (when-let [[_ month day] (re-find #"^(\d{1,2})/(\d{1,2})$" date-string)]
    (let [today (t/today-at-midnight)
          year (t/year today)]
      (->> (range -1 2)
           (map (comp #(t/local-date % month day)
                      #(+ year %)))
           (sort-by #(days-between today %))
           first))))

(def ^:private parse-date*
  (some-fn parse-full-date
           parse-month-day))

(defn- parse-date
  [date-string]
  (when date-string
    (parse-date* date-string)))

(defn- unparse-date
  [date]
  (if date
    (tf/unparse (tf/formatter "M/d/yyyy") date)
    ""))

(def ^:private date-input-defaults
  {:unparse-fn unparse-date
   :parse-fn parse-date})

(defn date-input
  [model field {:as options
                :keys [icon]
                :or {icon (icons/icon :calendar {:size :small})}}]
  (let [ctl-state (r/atom {:calendar (cal/init {:first-day-of-week :sunday
                                                :selected (get-in @model field)})})
        visible? (r/cursor ctl-state [:visible?])]
    (add-watch model field (fn [k _ before after]
                             (let [b (get-in before k)
                                   a (get-in after k)]
                               (when (and a (nil? b))
                                 (swap! ctl-state assoc
                                        :calendar (cal/init {:first-day-of-week :sunday
                                                             :selected a}))))))
    (fn []
      [:span
       [specialized-text-input
        model
        field
        (merge
          date-input-defaults
          {:on-accept (fn [d]
                        (swap! ctl-state #(-> %
                                              (update-in [:calendar] cal/select d)
                                              (dissoc :visible?))))}
          options
          {:append [:button.btn.btn-secondary
                    {:on-click #(swap! ctl-state update-in [:visible?] not)
                     :type :button}
                    icon]
           :on-key-down (fn [e]
                          (when (dom/ctrl-key? e)
                            (.preventDefault e)
                            (when-let [[oper value] (case (dom/key-code e)
                                                      :left  [t/minus (t/days 1)]
                                                      :right [t/plus  (t/days 1)]
                                                      :up    [t/minus (t/months 1)]
                                                      :down  [t/plus  (t/months 1)]
                                                      nil)]
                              (swap! model update-in field oper value))))
           :unparse-fn unparse-date
           :parse-fn parse-date
           :equals-fn #(when (and %1 %2) (t/equal? %1 %2))})]
       [:div.shadow.rounded {:class (when-not @visible? "d-none")
                             :style {:position :absolute
                                     :border "1px solid var(--dark)"
                                     :padding "2px"
                                     :z-index 99
                                     :background-color "#fff"}}
        [calview/calendar ctl-state {:small? true
                                     :on-day-click (fn [date]
                                                     (swap! model assoc-in field date)
                                                     (swap! ctl-state #(-> %
                                                                           (update-in [:calendar] cal/select date)
                                                                           (dissoc :visible?))))}]]])))

(defn date-field
  [model field options]
  (date-input model field (assoc-in options [::decoration ::presentation] ::field)))

(defn- parse-int
  [text-value]
  (when (and text-value
             (re-find #"^\d+$" text-value))
    (js/parseInt text-value)))

(defn integer-input
  [model field options]
  (specialized-text-input model field (merge options {:type :number
                                                      :parse-fn parse-int})))

(defn integer-field
  [model field options]
  (integer-input model field (assoc-in options [::decoration ::presentation] ::field)))

(defn float-input
  [model field options]
  (specialized-text-input model field (merge options {:type :number
                                                      :parse-fn lib/parse-float})))

(defn float-field
  [model field options]
  (float-input model field (assoc-in options [::decoration ::presentation] ::field)))

(defn decimal-input
  [model field options]
  (specialized-text-input model field (merge options {:type :text
                                                      :parse-fn math/eval})))

(defn decimal-field
  ([model field] (decimal-field model field {}))
  ([model field options]
   (decimal-input model field (assoc-in options [::decoration ::presentation] ::field))))

(defmulti ^:private select-option
  (fn [v]
    (if (vector? v)
      :compound
      (if (keyword? v)
        :keyword
        :simple))))

(defmethod ^:private select-option :keyword
  [item field]
  ^{:key (str "option-" field "-" (name item))}
  [:option {:value (name item)} (name item)])

(defmethod ^:private select-option :simple
  [item field]
  ^{:key (str "option-" field "-" (str item))}
  [:option {:value (str item)} (str item)])

(defmethod ^:private select-option :compound
  [[value caption] field]
  ^{:key (str "option-" field "-" value)}
  [:option {:value value} caption])

(defn select-elem
  "Renders a select element.

  model - An atom that will contain data entered into the form
  field - a vector describing the location in the model where the value for this field is to be saved. (As in get-in)
  items - The items to be rendered in the list. Each item in the list is a tuple with the value in the 1st position and the label in the 2nd."
  [model field items {:keys [transform-fn
                             validations
                             on-change
                             html]
                      :or {transform-fn identity
                           on-change identity
                           html {}}
                      :as options}]
  (v/add-rules model field validations)
  (let [errors (make-reaction #(v/validation-msg model field))]
    (fn []
      (decorate
        [:select (merge html
                        {:id field
                         :name field
                         :value (or (get-in @model field) "")
                         :on-change (fn [e]
                                      (let [value (.-value (.-target e))]
                                        (swap! model
                                               assoc-in
                                               field
                                               (if (empty? value)
                                                 nil
                                                 (transform-fn value)))
                                        (v/validate model field)
                                        (v/set-custom-validity e @model field)
                                        (on-change model field)))})
         (->> (if (lib/derefable? items)
                @items
                items)
              (map #(select-option % field))
              doall)]
        model
        field
        @errors
        options))))

(defn select-field
  "Renders a select element within a bootstrap field-group with a label.

  model - An atom that will contain data entered into the form
  field - a vector describing the location in the model where the value for this field is to be saved. (As in get-in)
  items - The items to be rendered in the list. Each item in the list is a tuple with the value in the 1st position and the label in the 2nd."
  ([model field items] (select-field model field items {}))
  ([model field items options]
   (select-elem model field items (assoc-in options [::decoration ::presentation] ::field))))

(defmulti apply-selected-item
  (fn [_ {:keys [mode]}] mode))

(defmethod apply-selected-item :direct
  [item {:keys [text-value
                value-fn
                model
                field
                on-change]
         :or {on-change identity}}]
  (if item
    (let [new-value (value-fn item)]
      (reset! text-value new-value)
      (swap! model assoc-in field new-value)
      (on-change item))
    (let [new-value @text-value]
      (swap! model assoc-in field new-value)
      (on-change new-value))))

(defmethod apply-selected-item :default
  [item {:keys [model
                field
                text-value
                caption-fn
                on-change
                value-fn]
         :or {caption-fn identity}}]
  (when item
    (swap! model
           assoc-in
           field
           (value-fn item))
    (reset! text-value (caption-fn item)))
  (on-change item))

(defn typeahead-input
  "Renders an input field with typeahead search capability

  model - an atom wrapping a map which contains an attribute to be updated
  field - a vector of fields identifying the attribute to be updated (as is get-in/update-in)
  options -
    search-fn       - a fn that takes a single string argument and returns matching data records
    find-fn         - a fn that takes the stored values and finds the corrsponding data record
    caption-fn      - accepts a data record and returns the value to display in the field
    list-caption-fn - like caption-fn, but used to render a data record in the list. Uses caption-fn if not supplied
    value-fn        - accepts a data record and returns the value to be stored in the attribute
    on-change       - callback invoked when the value of the attribute changes
    html            - a map of attributes to be passed directly to the input element
    max-items       - the maximum number of matching data records to show in the list
    list-attr       - attributes to be applied to the list HTML element"
  [model field options]
  (let [state (typeahead/state model field options)
        errors (make-reaction #(v/validation-msg model field))]
    (typeahead/set-value state)
    (typeahead/watch-model state)
    (fn []
      (let [input (typeahead/elem state)
            result-list (typeahead/list-elem state)]
        (decorate input
                  model
                  field
                  @errors
                  (-> state
                      (assoc :list-elem result-list)
                      (update-in [::decoration] merge {::presentation ::element
                                                       ::target ::typeahead})))))))

(defn typeahead-field
  [model field options]
  (let [state (typeahead/state model field options)
        errors (make-reaction #(v/validation-msg model field))]
    (typeahead/set-value state)
    (typeahead/watch-model state)
    (fn []
      (let [input (typeahead/elem state)
            result-list (typeahead/list-elem state)]
        (decorate input
                  model
                  field
                  @errors
                  (-> state
                      (assoc :list-elem result-list)
                      (update-in [::decoration] merge {::presentation ::field
                                                       ::target ::typeahead})))))))

(def ^:private time-input-defaults
  {:unparse-fn format-time
   :parse-fn unformat-time})

(defn time-input
  [model field options]
  [specialized-text-input model field (merge time-input-defaults options)])

(defn invalid?
  "Returns true if the model has any form validation errors."
  [model]
  (boolean (seq (::invalid-feedback @model))))

; TODO: This either needs to be decorated, or moved somewhere else
(defn busy-button
  [{:keys [html caption icon busy?]}]
  (fn []
    (if busy?
      [:button.btn (merge html
                          {:disabled (boolean @busy?)})
       (cond
         (and icon caption)
         (icons/icon-with-text (if @busy? :spinner icon)
                               caption)

         icon
         (if @busy?
           (spinner {})
           (icons/icon icon))

         :else
         (if @busy?
           (icons/icon-with-text :spinner caption)
           caption))]
      [:div.alert.alert-danger "Must specify :busy?"])))
