(ns dgknght.app-lib.forms
  (:require [reagent.core :as r]
            [reagent.ratom :refer [make-reaction]]
            [clojure.string :as string]
            [goog.string :as gstr]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [dgknght.app-lib.core :as lib]
            [dgknght.app-lib.web :refer [format-time
                                         unformat-time]]
            [dgknght.app-lib.math :as math]
            [dgknght.app-lib.inflection :refer [humanize]]
            [dgknght.app-lib.html :as html]
            [dgknght.app-lib.bootstrap-icons :as icons]
            [dgknght.app-lib.calendar :as cal]
            [dgknght.app-lib.calendar-view :as calview]))

(def defaults (atom {}))

(derive ::password ::text)
(derive ::email ::text)
(derive ::number ::text)
(derive ::select ::text)
(derive ::textarea ::text)

(defn ->id
  [field]
  (->> field
       (map (comp #(string/replace % #"[^a-z0-9_-]+" "")
                  #(if (keyword? %)
                     (name %)
                     (str %))))
       (string/join "-")))

(defn ->caption
  [field]
  (let [humanized (humanize (last field))]
    (if-let [trimmed (re-find #"^.+(?= id$)" humanized)]
      trimmed
      humanized)))

(defn ->name
  [field]
  (->> field
       (map #(if (keyword? %)
               (name %)
               %))
       (string/join "-")))

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
  (fn [elem _model _field options]
    (when-not (= ::none (::decoration options))
      (extract-dispatch options elem))))

(defmulti spinner extract-framework)

(defmethod decorate :default
  [elem _model field {::keys [decoration] :as options}]
  (when-not (= ::none decoration)
    (.warn js/console (prn-str {:unhandled-decoration decoration
                                :elem elem
                                :target (extract-target elem options)
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
                                        (if (html/checked? %)
                                          (conj v-set value)
                                          (disj v-set value)))))
                  #(reset! value-atm (html/checked? %)))
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
                                                  (if (html/checked? e)
                                                    (swap! model update-in field (fnil conj #{}) value)
                                                    (swap! model update-in field disj value)))
                                     :value value}]
                            (decorate model field (-> options
                                                      (assoc-in [::decoration ::presentation] ::inline-field)
                                                      (assoc :caption label)))
                            (with-meta {:key id})))))
       doall))

(defn checkbox-inputs
  [model field items {:keys [container-html] :as options}]
  [:div
   (merge {} container-html)
   (checkbox-inputs* model field items options)])

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
  [model field {:keys [html on-change]
                input-type :type
                :or {input-type :text
                     on-change identity}
                :as options}]
  (let [value (r/cursor model field)]
    (fn []
      (decorate
        [:input (merge {:id (->id field)}
                       html
                       {:type input-type
                        :name (->name field)
                        :value @value
                        :on-change (fn [e]
                                     (let [new-value (-> e html/target html/value)]
                                       (swap! model assoc-in field new-value)
                                       (on-change new-value)))})]
        model
        field
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

(def ^:private validation-rules
  {:required (fn [model field]
               (when (string/blank? (str (get-in model field)))
                 (js/Promise.resolve "is required.")))
   :format (fn [model field pattern]
             (let [v (get-in model field)]
               (when (and v (not (re-find pattern v)))
                 (js/Promise.resolve "is invalid."))))
   :length (fn [model field {:keys [min max]}]
             (let [v (get-in model field)]
               (when v
                 (cond
                   (and min
                        (< (count v) min))
                   (js/Promise.resolve (str "must be at least " min " characters."))

                   (and max
                        (> (count v) max))
                   (js/Promise.resolve (str "cannot be more than " max " characters."))))))})

(defn- check-validation-rule
  [rule model field]
  (let [[f args] (cond
            (keyword? rule)
            [(rule validation-rules) []]

            (vector? rule)
            [((first rule) validation-rules) (rest rule)]

            :else
            [rule []])]

    (when-not f
      (throw (str "Unrecognized rule: " (prn-str rule))))

    (apply f (concat [model field] args))))

(defn- check-validation-rules
  [model field rules]
  (when (seq rules)
    (js/Promise.all
      (map #(check-validation-rule % model field)
           rules))))

(defn- validate-field
  [model field rules]
  (when (seq rules)
    (let [p (check-validation-rules @model field rules)]
      (.then p (fn [results]
                 (let [violations (->> results
                                       (filter identity)
                                       (map #(str (humanize (last field)) " " %)))]
                   (if (seq violations)
                     (swap! model #(-> %
                                       (assoc-in [::input-classes field] "is-invalid")
                                       (assoc-in [::invalid-feedback field] (string/join ", " violations))))
                     (swap! model #(-> %
                                       (assoc-in [::input-classes field] "is-valid")
                                       (update-in [::invalid-feedback] dissoc field))))))))))

(defn textarea-elem
  ([model field] (textarea-elem model field {}))
  ([model field options]
   (let [changed? (r/atom false)
         value (r/cursor model field)]
     (fn []
       (decorate
         [:textarea
          {:id (->id field)
           :value @value
           :class (input-class @model field)
           :on-change (fn [e]
                        (reset! changed? true)
                        (update-field model field e))
           :on-blur #(when (or @changed? (nil? @value))
                       (validate-field model
                                       field
                                       (:validate options)))}]
         model
         field
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
                                           :auto-complete "email"}))))

(defn password-field
  ([model field options]
   (text-field model field (merge options {:type :password
                                           :auto-complete "current-password"}))))

(defn- specialized-text-input
  [model field {input-type :type
                :keys [parse-fn
                       unparse-fn
                       equals-fn
                       disabled-fn
                       on-accept
                       html]
                :or {input-type :text
                     equals-fn =
                     disabled-fn (constantly false)
                     unparse-fn str
                     on-accept identity}
                :as options}]
  (let [text-value (r/atom (unparse-fn (get-in @model field)))]
    (add-watch model field (fn [_field _sender before after]
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
                         :on-change (fn [e]
                                      (let [new-value (.-value (.-target e))
                                            parsed (try
                                                     (parse-fn new-value)
                                                     (catch js/Error e
                                                       (.log js/console
                                                             (gstr/format "Error parsing \"%s\": (%s) %s"
                                                                          new-value
                                                                          (.-name e)
                                                                          (.-message e)))
                                                       nil))]
                                        (if (seq new-value)
                                          (when parsed
                                            (swap! model assoc-in field parsed)
                                            (on-accept))
                                          (swap! model nilify field))
                                        (reset! text-value new-value)))})]
        (decorate [:input attr] model field options)))))

(defn- parse-date
  [date-string]
  (when (and date-string
             (re-find #"^\d{1,2}/\d{1,2}/\d{4}$" date-string))
    (tf/parse (tf/formatter "M/d/yyyy") date-string)))

(defn- unparse-date
  [date]
  (if date
    (tf/unparse (tf/formatter "M/d/yyyy") date)
    ""))

(def ^:private date-input-defaults
  {:unparse-fn unparse-date
   :parse-fn parse-date})

(defn date-input
  [model field options]
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
                    (icons/icon :calendar {:size :small})]
           :on-key-down (fn [e]
                          (when (html/ctrl-key? e)
                            (.preventDefault e)
                            (when-let [[oper value] (case (html/key-code e)
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

(defn- derefable?
  [x]
  (satisfies? cljs.core/IDeref x))

(defn select-elem
  "Renders a select element.

  model - An atom that will contain data entered into the form
  field - a vector describing the location in the model where the value for this field is to be saved. (As in get-in)
  items - The items to be rendered in the list. Each item in the list is a tuple with the value in the 1st position and the label in the 2nd."
  [model field items {:keys [transform-fn
                             on-change]
                      :or {transform-fn identity
                           on-change identity}
                      :as options}]
  (fn []
    (decorate
      [:select {:id field
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
                               (on-change model field)))}
       (->> (if (derefable? items)
              @items
              items)
            (map #(select-option % field))
            doall)]
      model
      field
      options)))

(defn select-field
  "Renders a select element within a bootstrap field-group with a label.

  model - An atom that will contain data entered into the form
  field - a vector describing the location in the model where the value for this field is to be saved. (As in get-in)
  items - The items to be rendered in the list. Each item in the list is a tuple with the value in the 1st position and the label in the 2nd."
  ([model field items] (select-field model field items {}))
  ([model field items options]
   (select-elem model field items (assoc-in options [::decoration ::presentation] ::field))))

(defn- assoc-select-item
  [{:keys [value-fn
           caption-fn
           on-change
           create-fn
           items
           text-value
           model
           field]
    :or {on-change identity
         value-fn identity
         create-fn (constantly nil)
         caption-fn identity}
    :as options}]
  (assoc options
         :select-item (fn [index]
                        (let [item (if index
                                     (lib/safe-nth @items index)
                                     (create-fn @text-value))
                              [value caption] (if item
                                                ((juxt value-fn caption-fn) item)
                                                [nil ""])]
                          (reset! items nil)
                          (swap! model
                                 assoc-in
                                 field
                                 value)
                          (reset! text-value caption)
                          (on-change item)))))

(defn- assoc-key-down
  [{:keys [model
           field
           find-fn
           caption-fn
           items
           index
           text-value
           select-item]
    :or {find-fn identity
         caption-fn identity}
    :as options}]
  (assoc options
         :handle-key-down
         (fn [e]
           (when @items
             (case (html/key-code e)
               :up           (swap! index #(-> (or % (count @items))
                                               dec
                                               (mod (count @items))))
               :down         (swap! index #(-> (or % -1)
                                               inc
                                               (mod (count @items))))
               (:enter :tab) (select-item @index)
               :escape       (do
                               (find-fn (get-in @model field)
                                        #(reset! text-value (caption-fn %)))
                               (reset! items nil))
               nil)))))

(defn- assoc-handle-change
  [{:keys [text-value
           items
           max-items
           search-fn]
    :or {max-items 10
         search-fn identity}
    :as options}]
  (assoc options
         :handle-change
         (fn [e]
           ; TODO: debounce this lookup
           (let [raw-value (-> e html/target html/value)]
             (reset! text-value raw-value)
             (if (empty? raw-value)
               (reset! items nil)
               (search-fn raw-value #(->> %
                                          (take max-items)
                                          (reset! items))))))))

(defn- typeahead-list
  [{:keys [field
           list-attr
           items
           index
           select-item
           list-caption-fn]}]
  (apply vector
         :div
         list-attr
         (map-indexed
           (fn [i item]
             ^{:key (str (string/join field "-") "option-" i)
               :active? (= @index i)}
             [:button {:type :button
                       :on-click #(select-item i)}
              (list-caption-fn item)])
           @items)))

(defn- typeahead-state
  [model field {:keys [caption-fn
                       list-caption-fn
                       find-fn]
                :as options}]
  (-> options
      (merge {:model model
              :field field
              :text-value (r/atom "")
              :items (r/atom nil)
              :index (r/atom nil)
              :caption-fn (or caption-fn
                              identity)
              :list-caption-fn (or list-caption-fn
                                   caption-fn
                                   identity)
              :find-fn (or find-fn identity)})
      (update-in [:html :id] (fnil identity (->id field)))
      assoc-select-item
      assoc-key-down
      assoc-handle-change))

(defn- watch-typeahead-model
  [{:keys [model field find-fn text-value caption-fn]}]
  (add-watch model
             field
             (fn [_field _sender before after]
               (let [v-before (get-in before field)
                     v-after (get-in after field)]
                 (if v-after
                   (when (nil? v-before)
                     (find-fn v-after #(reset! text-value (caption-fn %))))
                   (reset! text-value ""))))))

(defn- set-typeahead-value
  [{:keys [model field find-fn text-value caption-fn]}]
    (when-let [current-value (get-in @model field)]
      (find-fn current-value #(reset! text-value (caption-fn %)))))

(defn- typeahead-elem
  [{:keys [field
           items
           html
           text-value
           handle-change
           handle-key-down]
    {:keys [on-key-up]} :html}]
  [:input
   (merge {:id (->id field)}
          html
          {:type :text
           :auto-complete :off
           :name (->name field)
           :value @text-value
           :on-key-down handle-key-down
           :on-key-up (when on-key-up
                        #(when-not @items
                           (on-key-up %)))
           :on-change handle-change})])

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
  (let [state (typeahead-state model field options)]
    (set-typeahead-value state)
    (watch-typeahead-model state)
    (fn []
      (let [input (typeahead-elem state)
            result-list (typeahead-list state)]
        (decorate input
                  model
                  field
                  (-> state
                      (assoc :list-elem result-list)
                      (update-in [::decoration] merge {::presentation ::element
                                                       ::target ::typeahead})))))))

(defn typeahead-field
  [model field options]
  (let [state (typeahead-state model field options)]
    (set-typeahead-value state)
    (watch-typeahead-model state)
    (fn []
      (let [input (typeahead-elem state)
            result-list (typeahead-list state)]
        (decorate input
                  model
                  field
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

(defn clean
  "Given a model that has been populated and validated by
  form controlrs, removes extra attributes added for those
  purposes."
  [m]
  (dissoc m ::input-classes ::invalid-feedback))

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
