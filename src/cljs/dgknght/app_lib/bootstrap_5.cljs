(ns dgknght.app-lib.bootstrap-5
  "Functions to build bootstrap 5.x web elements"
  (:require [dgknght.app-lib.inflection :refer [title-case]]
            [dgknght.app-lib.notifications :as notify]
            [dgknght.app-lib.html :refer [add-class]]
            [dgknght.app-lib.forms :as forms]
            [dgknght.app-lib.bootstrap-4 :as bs-4]
            [dgknght.app-lib.bootstrap-icons :as icons]))

(derive ::bootstrap-5 ::bs-4/bootstrap-4)

(defn nav-item
  ([item] (nav-item item {}))
  ([{:keys [id path active? label nav-fn badge badge-class]
     :or {path "#"}}
    {:keys [toggle]}]
   ^{:key (str "nav-item-" (name id))}
   [:li.nav-item
    [:a.nav-link.d-flex.align-items-center (cond-> {:href path}
                                             toggle (merge {:data-bs-toggle :collapse
                                                            :data-bs-target toggle})
                                             nav-fn (assoc :on-click nav-fn)
                                             active? (assoc :class "active"
                                                            :aria-current "page"))
     (or label (title-case id))
     (when badge
       [:span.badge.ms-1 {:class badge-class} badge])]]))

(defn navbar
  [items {:keys [brand brand-path profile-photo-url]}]
  [:nav.navbar.navbar-expand-lg.navbar-light.bg-light
   [:div.container
    [:a.navbar-brand {:href brand-path} brand]
    [:button.navbar-toggler {:type :button
                             :data-bs-toggle :collapse
                             :data-bs-target "#primaryNav"
                             :aria-controls "primaryNav"
                             :aria-expanded false
                             :aria-label "Toggle Navigation"}
     (icons/icon :list)]
    (when (seq items)
      [:div#primaryNav.collapse.navbar-collapse
       [:ul.navbar-nav.me-auto.mb-2.mb-lg-0
        (->> items
             (map nav-item)
             doall)]])
    (when profile-photo-url
      [:img {:class ["rounded-circle"
                     "d-none"
                     "d-lg-block"]
             :src profile-photo-url
             :style {:max-width "32px"}
             :alt "Profile Photo"}])]])

(def icon icons/icon)

(defn spinner
  ([] (spinner {}))
  ([{:keys [style caption size]
     :or {caption "Loading..."}}]
   (let [class-name (str "spinner-" (if (= :grow style)
                                      "grow"
                                      "border"))]
     [:div {:class (cond-> [class-name]
                     (= :small size)
                     (conj (str class-name "-sm")))}
      [:span.visually-hidden caption]])))

(defmethod forms/spinner ::bootstrap-5
  [options]
  (spinner options))

(defn init-toasts []
  (let [ctor (. js/bootstrap -Toast)]
    (doseq [elem (.querySelectorAll js/document "div.toast")]
      (. (ctor. elem #js {}) show))))

(defn toast
  [{:keys [title body id]}]
  ^{:key (str "toast-" id)}
  [:div.toast {:role :alert
               :aria-live :assertive
               :aria-atomic true}
   [:div.toast-header
    [:strong.me-auto title]
    [:button {:aria-label "Close"
              :on-click #(notify/untoast id)}
     (icons/icon :x)]]
   [:div.toast-body
    body]])

(defn alert
  [{:keys [id severity message] :as a}]
  ^{:key (str "alert-" id)}
  [:div.alert.d-flex.align-items-center
   {:class (str "alert-" (name severity))
    :role :alert}
   [:span.me-auto.flex-grow-1 message]
   [:button.btn {:type :button
                 :class (str "text-" (name severity))
                 :on-click #(notify/unnotify a)
                 :aria-label "Close"}
    (icons/icon :x)]])

(defn icon-with-text
  ([icon-key text options]
   [:span.d-flex.align-items-center
    (if (= :spinner icon-key)
      (spinner {:size :small})
      (icon icon-key options))
    [:span.ms-1
     text]]))

(defn busy-button
  [{:keys [html caption icon busy?]}]
  (fn []
    (if busy?
      [:button.btn (merge html
                          {:disabled (boolean @busy?)})
       (cond
         (and icon caption)
         (icon-with-text (if @busy? :spinner icon)
                         caption
                         {:size :small})

         icon
         (if @busy?
           (spinner {})
           (icons/icon icon {:size :small}))

         :else
         (if @busy?
           (icon-with-text :spinner caption {:size :small})
           caption))]
      [:div.alert.alert-danger "Must specify :busy?"])))

(defmethod forms/decorate [::forms/checkbox ::forms/field ::bootstrap-5]
  [[_ attr :as elem] model field {:keys [hide?] :as options}]
  [:div.mb-3 {:class (when (if (satisfies? IDeref hide?)
                             @hide?
                             hide?)
                       "d-none")}
   [:div.mb-3.form-check {:class (when (:inline? options) "form-check-inline")}
    (forms/decorate elem
                    model
                    field
                    (assoc-in options [::forms/decoration ::forms/presentation] ::forms/element))
    [:label.form-check-label {:for (:id attr)}
     (or (:caption options)
         (forms/->caption field))]]])

(defmethod forms/decorate [::forms/select ::forms/element ::bootstrap-5]
  [elem _model _field _options]
  (add-class elem "form-select"))

(defmethod forms/decorate [::forms/text ::forms/field ::bootstrap-5]
  [[_ attr :as elem] model field {:keys [hide?] :as options}]
  (let [inner-decorated (forms/decorate elem
                                        model
                                        field
                                        (assoc-in options
                                                  [::forms/decoration ::forms/presentation]
                                                  ::forms/element))]
    [:div.mb-3 {:class (when (if (satisfies? IDeref hide?) @hide? hide?) "d-none")}
     [:label.form-label {:for (:id attr)} (or (:caption options)
                                              (forms/->caption field))]
     (bs-4/help-popover field options)
     inner-decorated
     [:div.invalid-feedback (bs-4/invalid-feedback @model field)]]))

(defmethod forms/decorate [::forms/typeahead ::forms/field ::bootstrap-5]
  [elem model field {:as options
                     :keys [hide? caption list-elem]}]
  [:div.mb-3 {:class (when (if (satisfies? IDeref hide?) @hide? hide?) "d-none")}
   [:label.form-label {:for (get-in elem [1 :id])}
    (or caption
        (forms/->caption field))]
   (bs-4/help-popover field options)
   (forms/decorate elem
                   model
                   field
                   (update-in options [::forms/decoration] merge {::forms/target ::forms/text
                                                                  ::forms/presentation ::forms/element}))
   (bs-4/decorate-typeahead-list list-elem)
   [:div.invalid-feedback (bs-4/invalid-feedback @model field)]])
