(ns dgknght.app-lib.bootstrap-4
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [dgknght.app-lib.client-macros :refer-macros [call
                                                          with-retry]]
            [dgknght.app-lib.html :refer [add-class
                                          add-classes
                                          conj-to-vec]]
            [dgknght.app-lib.forms :as forms]
            [dgknght.app-lib.forms-validation :as v]
            [dgknght.app-lib.bootstrap-icons :as icons]))

(defn- jq-match
  [selector]
  (call js/window :j-query selector))

(defn jq-toast
  [selector]
  (call (jq-match selector) :toast "show"))

(defn jq-tool-tip
  [selector]
  (call (jq-match selector) :tooltip))

(defn jq-popover
  [selector opts]
  (call (jq-match selector) :popover (clj->js opts)))

(defmethod forms/decorate [::forms/checkbox ::forms/element ::bootstrap-4]
  [elem _model _field _errors _options]
  (add-class elem "form-check-input"))

(defmethod forms/decorate [::forms/checkbox ::forms/field ::bootstrap-4]
  [[_ attr :as elem] model field errors {:keys [hide?] :as options}]
  [:div.form-group {:class (when (if (satisfies? IDeref hide?)
                                   @hide?
                                   hide?)
                             "d-none")}
   [:div.form-check {:class (when (:inline? options) "form-check-inline")}
    [:label.form-check-label {:for (:id attr)}
     (forms/decorate elem
                     model
                     field
                     errors
                     (assoc-in options [::forms/decoration ::forms/presentation] ::forms/element))
     (or (:caption options)
         (forms/->caption field))]]])

(defmethod forms/decorate [::forms/checkbox ::forms/inline-field ::bootstrap-4]
  [[_ attr :as elem] model field errors {:keys [input-container-html] :as options}]
  [:div.form-check.form-check-inline (merge {} input-container-html)
   [:label.form-check-label {:for (:id attr)}
    (forms/decorate elem
                    model
                    field
                    errors
                    (assoc-in options [::forms/decoration ::forms/presentation] ::forms/element))
    (or (:caption options)
        (forms/->caption field))]])

(defmethod forms/decorate [::forms/text ::forms/element ::bootstrap-4]
  [elem model _field errors {:keys [prepend append]}]
  (let [decorated (cond-> (add-class elem "form-control")
                    (v/valid? model) (add-class "is-valid")
                    (seq errors) (add-class "is-invalid"))]
    (if (or prepend append)
      [:div.input-group {:class (when (seq errors) "is-invalid")} ; adding is-invalid here triggers bootstraps invalid-feedback visbility
       (when prepend [:div.input-group-prepend prepend])
       decorated
       (when append [:div.input-group-append append])]
      decorated)))

(defn popover
  [{:keys [message id title]}]
  (with-retry (jq-popover (str "#" id) {:trigger "hover"}))
  [:span.ml-1.hint-toggle {:id id
                           :title title
                           :data-toggle "popover"
                           :data-content message}
   (icons/icon :question {:size :small})])

(defn help-popover
  [field {:keys [help]}]
  (when help
    (popover {:id (str "help-" (->> field (map name) (string/join "-")))
              :message help
              :title "Helpful Hint"})))

(defmethod forms/decorate [::forms/text ::forms/field ::bootstrap-4]
  [[_ attr :as elem] model field errors {:keys [hide?] :as options}]
  (let [inner-decorated (forms/decorate elem
                                        model
                                        field
                                        errors
                                        (assoc-in options
                                                  [::forms/decoration ::forms/presentation]
                                                  ::forms/element))
        errors (v/validation-msg @model field)]
    [:div.form-group {:class (when (if (satisfies? IDeref hide?) @hide? hide?) "d-none")}
     [:label {:for (:id attr)} (or (:caption options)
                                   (forms/->caption field))]
     (help-popover field options)
     inner-decorated
     (when errors
       [:div.invalid-feedback errors])]))

(defn- decorate-list-item
  [elem]
  (add-classes elem
               (cond-> ["list-group-item"
                        "list-group-item-action"]
                 (-> elem meta :active?) (conj "active"))))

(defn decorate-typeahead-list
  [[tag attr :as elem]]
  (let [elems (drop 2 elem)]
    (apply vector
           tag
           (-> attr
               (update-in [:style] merge {:position :absolute :z-index 99})
               (update-in [:class] conj-to-vec "list-group"))
           (map decorate-list-item elems))))

(defmethod forms/decorate [::forms/typeahead ::forms/element ::bootstrap-4]
  [elem model field errors {:as options
                     :keys [list-elem]}]
  [:span
   (forms/decorate elem model field errors (assoc-in options [::forms/decoration ::forms/target] ::forms/text))
   (decorate-typeahead-list list-elem)])

(defmethod forms/decorate [::forms/typeahead ::forms/field ::bootstrap-4]
  [elem model field errors {:as options
                            :keys [hide? caption list-elem]}]
  [:div.form-group {:class (when (if (satisfies? IDeref hide?) @hide? hide?) "d-none")}
   [:label {:for (get-in elem [1 :id])}
    (or caption
        (forms/->caption field))]
   (help-popover field options)
   (forms/decorate elem
                   model
                   field
                   errors
                   (update-in options [::forms/decoration] merge {::forms/target ::forms/text
                                                                  ::forms/presentation ::forms/element}))
   (decorate-typeahead-list list-elem)
   [:div.invalid-feedback (v/validation-msg @model field)]])

(defmulti ^:private nav-item :role)

(defmethod ^:private nav-item :separator
  [{:keys [id]}]
  ^{:key (str "separator-" id)}
  [:li.dropdown-divider {:role "separator"}])

(defmethod ^:private nav-item :dropdown
  [{:keys [children id caption active? tool-tip]}]
  (when-not (seq children)
    (throw "A dropdown nav item must have children"))

  ^{:key (str "menu-item-" id)}
  [:li.nav-item.dropdown
   [:a.nav-link.dropdown-toggle {:href "#"
                                 :title tool-tip
                                 :class [(when active? "active")]
                                 :data-toggle "dropdown"
                                 :role "button"
                                 :aria-expanded false
                                 :aria-haspopup true}
    caption
    [:span.caret]]
   [:ul.dropdown-menu
    (for [child children]
      (nav-item child))]])

(defmethod ^:private nav-item :default
  [{:keys [id caption url tool-tip active? on-click]
    :or {url "#"}}]
  ^{:key (str "menu-item-" id)}
  [:li.nav-item {:class (when active? "active")}
   [:a.nav-link {:href url
                 :title tool-tip
                 :on-click on-click}
    caption]])

(defn navbar
  "Renders a bootstrap nav bar"
  [{:keys [title title-url items secondary-items] :or {title-url "/"}}]
  [:nav.navbar.navbar-expand-lg.navbar-light.bg-light
   [:div.container
    [:a.navbar-brand {:href title-url} title]
    [:button.navbar-toggler {:type :button
                             :data-toggle :collapse
                             :data-target "#primary-navbar"
                             :aria-controls "primary-navbar"
                             :aria-expanded false
                             :aria-label "Toggle navigation"}
     [:span.navbar-toggler-icon]]

    [:div#primary-navbar.collapse.navbar-collapse
     [:ul.navbar-nav.mr-auto {}
      (for [item items]
        (nav-item item))]
     (when (seq secondary-items)
       [:ul.nav.navbar-nav
        (for [item secondary-items]
          (nav-item item))])]]])

(defn alert
  [{:keys [message severity]
    :or {severity :info}
    :as alert}
   remove-fn]
  ^{:key (str "alert-" (:id alert))}
  [:div.alert.alert-dismissible.fade.show {:class (str "alert-" (name severity))
                                           :role "alert"}
   [:button.btn-sm.close {:type :button
                          :aria-label "Close"
                          :on-click (fn [_] (remove-fn alert))}
    (icons/icon :x-circle)]
   message])

(defn- nav-tab
  [{:keys [active?
           disabled?
           hidden?
           on-click
           caption
           elem-key]}]
  ^{:key elem-key}
  [:li.nav-item
   [:a.nav-link {:href "#"
                 :on-click on-click
                 :class (cond
                          active? "active"
                          disabled? "disabled"
                          hidden? "d-none")}
    caption]])

(defn nav-tabs
  ([items]
   (nav-tabs {} items))
  ([options items]
   [:ul.nav.nav-tabs options
    (doall (map nav-tab items))]))

(defn nav-pills
  ([items]
   (nav-pills {} items))
  ([options items]
   [:ul.nav.nav-pills options
    (doall (map nav-tab items))]))

(defn- page-item
  [index state page-index-key]
  ^{:key (str "page-item-" index)}
  [:li.page-item {:class (when (= index (get-in @state page-index-key))
                           "active")}
   [:a.page-link {:href "#"
                  :on-click #(swap! state assoc-in page-index-key index)}
    (inc index)]])

(defn- page-item-simple-state
  [index page-index]
  ^{:key (str "page-item-" index)}
  [:li.page-item {:class (when (= index @page-index)
                           "active")}
   [:a.page-link {:href "#"
                  :on-click #(reset! page-index index)}
    (inc index)]])

(defn pagination
  "Creates navigation for paged data. Expects an derefable map with the following:
    :total      - the total number of items in the data set
    :page-index - the current page index (0-based)
    :page-size  - the number of items per page"
  ([state] (pagination state {}))
  ([state {:keys [total-key
                  page-size-key
                  page-index-key]
           :or {total-key [:total]
                page-size-key [:page-size]
                page-index-key [:page-index]}}]
   (let [total (r/cursor state total-key)
         page-size (r/cursor state page-size-key)]
     (fn []
       [:nav {:aria-label "Pagination"}
        [:ul.pagination
         (->> (range (Math/ceil (/ @total @page-size)))
              (map #(page-item % state page-index-key))
              doall)]])))
  ([total page-size page-index]
   [:nav {:aria-label "Pagination"}
      [:ul.pagination
       (->> (range (Math/ceil (/ total page-size)))
            (map #(page-item-simple-state % page-index))
            doall)]]))

(defn spinner
  ([] (spinner {}))
  ([{:keys [style]
     :or {style :border}}]
   [:div {:role :status
          :class (str "spinner-" (name style))}
    [:span.sr-only "Loading..."]]))

(defmethod forms/spinner ::bootstrap-4
  [options]
  (spinner options))
