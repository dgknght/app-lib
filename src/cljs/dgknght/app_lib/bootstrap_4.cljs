(ns dgknght.app-lib.bootstrap-4
  (:require [reagent.core :as r]
            [dgknght.app-lib.core :as lib]
            [dgknght.app-lib.forms :as forms]
            [dgknght.app-lib.bootstrap-icons :as icons]))

(def ^:private add-class
  (lib/fscalar (fnil conj [])))

(def ^:private add-classes
  (lib/fscalar (fnil concat [])))

(defn- invalid-feedback
  [model field]
  (get-in model [::forms/invalid-feedback field]))

(defmethod forms/decorate [::forms/checkbox ::forms/element ::bootstrap-4]
  [elem _model _field _options]
  (update-in elem [1 :class] add-class "form-check-input"))

(defmethod forms/decorate [::forms/checkbox ::forms/field ::bootstrap-4]
  [[_ attr :as elem] model field {:keys [hide?] :as options}]
  [:div.form-group {:class (when (if (satisfies? IDeref hide?)
                                   @hide?
                                   hide?)
                             "d-none")}
   [:div.form-check {:class (when (:inline? options) "form-check-inline")}
    [:label.form-check-label {:for (:id attr)}
     (forms/decorate elem
                     model
                     field
                     (assoc-in options [::forms/decoration ::forms/presentation] ::forms/element))
     (or (:caption options)
         (forms/->caption field))]]])

(defmethod forms/decorate [::forms/checkbox ::forms/inline-field ::bootstrap-4]
  [[_ attr :as elem] model field {:keys [input-container-html] :as options}]
  [:div.form-check.form-check-inline (merge {} input-container-html)
   [:label.form-check-label {:for (:id attr)}
    (forms/decorate elem
                    model
                    field
                    (assoc-in options [::forms/decoration ::forms/presentation] ::forms/element))
    (or (:caption options)
        (forms/->caption field))]])

(defmethod forms/decorate [::forms/text ::forms/element ::bootstrap-4]
  [elem _model _field {:keys [prepend append]}]
  (let [decorated (update-in elem
                             [1 :class]
                             add-class
                             "form-control")]
    (if (or prepend append)
      [:div.input-group
       (when prepend [:div.input-group-prepend prepend])
       decorated
       (when append [:div.input-group-append append])]
      decorated)))

(defmethod forms/decorate [::forms/text ::forms/field ::bootstrap-4]
  [[_ attr :as elem] model field {:keys [hide?] :as options}]
  (let [inner-decorated (forms/decorate elem
                                        model
                                        field
                                        (assoc-in options
                                                  [::forms/decoration ::forms/presentation]
                                                  ::forms/element))]
    [:div.form-group {:class (when (if (satisfies? IDeref hide?) @hide? hide?) "d-none")}
     [:label {:for (:id attr)} (or (:caption options)
                                   (forms/->caption field))]
     inner-decorated
     [:div.invalid-feedback (invalid-feedback @model field)]]))

(defn- decorate-list-item
  [elem]
  (update-in elem
             [1 :class]
             add-classes
             (cond-> ["list-group-item"
                      "list-group-item-action"]
               (-> elem meta :active?) (conj "active"))))

(defn- decorate-typeahead-list
  [[tag attr & elems]]
  (apply vector
         tag
         (-> attr
             (update-in [:style] merge {:position :absolute :z-index 99})
             (update-in [:class] add-class "list-group"))
         (map decorate-list-item elems)))

(defmethod forms/decorate [::forms/typeahead ::forms/element ::bootstrap-4]
  [elem model field {:as options
                     :keys [list-elem]}]
  [:span
   (forms/decorate elem model field (assoc-in options [::forms/decoration ::forms/target] ::forms/text))
   (decorate-typeahead-list list-elem)])

(defmethod forms/decorate [::forms/typeahead ::forms/field ::bootstrap-4]
  [elem model field {:as options
                     :keys [hide? caption list-elem]}]
  [:div.form-group {:class (when (if (satisfies? IDeref hide?) @hide? hide?) "d-none")}
   [:label {:for (get-in elem [1 :id])}
    (or caption
        (forms/->caption field))]
   (forms/decorate elem model field (update-in options [::forms/decoration] {::forms/target ::forms/text
                                                                             ::forms/presentation ::element}))
   (decorate-typeahead-list list-elem)
   [:div.invalid-feedback (invalid-feedback @model field)]])

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
  [index state]
  ^{:key (str "page-item-" index)}
  [:li.page-item {:class (when (= index (get-in @state [:page-index]))
                           "active")}
   [:a.page-link {:href "#"
                  :on-click #(swap! state assoc :page-index index)}
    (inc index)]])

(defn pagination
  "Creates navigation for paged data. Expects an derefable map with the following:
     :total      - the total number of items in the data set
     :page-index - the current page index (0-based)
     :page-size  - the number of items per page"
  [state]
  (let [total (r/cursor state [:total])
        page-size (r/cursor state [:page-size])]
    (fn []
      [:nav {:aria-label "Pagination"}
       [:ul.pagination
        (->> (range (Math/ceil (/ @total @page-size)))
             (map #(page-item % state))
             doall)]])))

(defn spinner
  ([] (spinner {}))
  ([{:keys [style]
     :or {style :border}}]
   [:div {:role :status
          :class (str "spinner-" (name style))}
    [:span.sr-only "Loading..."]]))
