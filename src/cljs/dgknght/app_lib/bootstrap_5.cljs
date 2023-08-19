(ns dgknght.app-lib.bootstrap-5
  "Functions to build bootstrap 5.x web elements"
  (:require [clojure.string :as string]
            [dgknght.app-lib.inflection :refer [title-case]]
            [dgknght.app-lib.notifications :as notify]
            [dgknght.app-lib.html :refer [add-class
                                          add-classes]]
            [dgknght.app-lib.core :as lib]
            [dgknght.app-lib.forms :as forms]
            [dgknght.app-lib.forms-validation :as v]
            [dgknght.app-lib.bootstrap-4 :as bs-4]
            [dgknght.app-lib.bootstrap-icons :as icons]))

(derive ::bootstrap-5 ::bs-4/bootstrap-4)

(defmulti expand-nav-item
  #(cond
     (string? %) :string
     (map? %) :map))
 
(defmethod expand-nav-item :map
  [i]
  (if (-> i :children seq)
    (update-in i [:children] #(map expand-nav-item %))
    i))
 
(defmethod expand-nav-item :string
  [path]
  {:id (keyword (->> (string/split path #"/")
                     (filter lib/presence)
                     (string/join "-")))
   :label (->> (string/split path #"/")
               (map title-case)
               (filter lib/presence)
               (string/join " "))
   :path path})

(defmulti nav-item
  #(or (:role %)
       (when (seq (:children %))
         :dropdown)))

(defmethod ^:private nav-item :separator
  [{:keys [id]}]
  ^{:key (str "separator-" id)}
  [:li [:hr.dropdown-divider {:role "separator"}]])

(defmethod ^:private nav-item :dropdown
  [{:keys [children id label active? tool-tip]}]
  (let [id (str "menu-item-" id)]
    ^{:key id}
    [:li.nav-item.dropdown
     [:a.nav-link.dropdown-toggle {:href "#"
                                   :title tool-tip
                                   :class [(when active? "active")]
                                   :data-bs-toggle :dropdown
                                   :role :button
                                   :aria-expanded false
                                   :aria-haspopup true}
      label]
     [:ul.dropdown-menu
      (->> children
           (map (comp nav-item
                      #(assoc % :in-dropdown? true)))
           doall)]]))

(defmethod ^:private nav-item :default
  [{:keys [id label path tool-tip active? nav-fn badge-class badge toggle in-dropdown?]
    :or {path "#"}}]

  ^{:key (str "menu-item-" id)}
  [:li {:class (when-not in-dropdown? "nav-item") }
   [:a.d-flex.align-items-center
    (cond-> {:href path
             :class (if in-dropdown? "dropdown-item" "nav-link")}
      tool-tip (assoc :title tool-tip)
      toggle (merge {:data-bs-toggle :collapse
                     :data-bs-target toggle})
      nav-fn (assoc :on-click nav-fn)
      active? (assoc :class "active"
                     :aria-current "page"))
    (or label (title-case id))
    (when badge
      [:span.badge.ms-1 {:class badge-class} badge])]])

(defn navbar
  [items {:keys [brand brand-path profile-photo-url secondary-items]}]
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
             (map nav-item
                  expand-nav-item)
             doall)]
       (when (seq secondary-items)
         [:ul.navbar-nav.mb-2.mb-lg-0
          (->> secondary-items
               (map nav-item
                    expand-nav-item)
               doall)])])
    (when profile-photo-url
      [:img.rounded-circle.d-none.d-lg-block
       {:src profile-photo-url
        :style {:max-width "32px"}
        :alt "Profile Photo"}])]])

(def ^{:deprecated "0.3.5"} icon icons/icon)

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
  (let [ctor (. js/bootstrap -Toast)
        node-list (.querySelectorAll js/document "div.toast")
        nodes (map #(.item node-list %)
                   (range (.-length node-list)))]
    (doseq [node nodes]
      (. (ctor. node #js {}) show))))

(defmulti toast
  #(if (:title %)
     :with-title
     :default))

(defmethod toast :default
  [{:keys [body id]}]
  ^{:key (str "toast-" id)}
  [:div.toast
   {:role :alert
    :class ["text-light"
            "bg-primary"
            "border-0"
            "fixed-bottom"
            "mb-3"
            "start-50"
            "translate-middle-x"]
    :aria-live :assertive
    :aria-atomic true}
   [:div.d-flex
    [:div.toast-body body]
    [:button.btn-close.btn-close-white.me-2.m-auto
     {:aria-label "Close"
      :on-click #(notify/untoast id)}]]])

(defmethod toast :with-title
  [{:keys [title body id]}]
  ^{:key (str "toast-" id)}
  [:div.toast {:role :alert
               :aria-live :assertive
               :aria-atomic true}
   [:div.toast-header
    [:strong.me-auto title]
    [:button.btn.text-secondary
     {:aria-label "Close"
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

(defn ^{:deprecated "0.3.5"} icon-with-text
  ([icon-key text] (icon-with-text icon-key text {:size :small}))
  ([icon-key text {:keys [size] :as options}]
   [:span.d-flex.align-items-center
    (if (= :spinner icon-key)
      (spinner {:size size})
      (icon icon-key options))
    [:span.ms-1
     text]]))

(defn ^{:deprecated "0.3.5"} busy-button
  [{:keys [html caption icon busy? disabled?]
    :or {disabled? (atom false)}}]
  (fn []
    (if busy?
      [:button.btn (merge html
                          {:disabled (or (boolean @busy?)
                                         @disabled?)})
       (cond
         (and icon caption)
         (icon-with-text (if @busy? :spinner icon)
                         caption
                         {:size :small})

         icon
         (if @busy?
           (spinner {:size :small})
           (icons/icon icon {:size :small}))

         :else
         (if @busy?
           (icon-with-text :spinner caption {:size :small})
           caption))]
      [:div.alert.alert-danger "Must specify :busy?"])))

(defmethod forms/decorate [::forms/checkbox ::forms/field ::bootstrap-5]
  [[_ attr :as elem] model field errors {:keys [hide?] :as options}]
  [:div.mb-3 {:class (when (if (satisfies? IDeref hide?)
                             @hide?
                             hide?)
                       "d-none")}
   [:div.mb-3.form-check {:class (when (:inline? options) "form-check-inline")}
    (forms/decorate elem
                    model
                    field
                    errors
                    (assoc-in options [::forms/decoration ::forms/presentation] ::forms/element))
    [:label.form-check-label {:for (:id attr)}
     (or (:caption options)
         (forms/->caption field))]]])

(defmethod forms/decorate [::forms/select ::forms/element ::bootstrap-5]
  [elem _model _field errors _options]
  (add-classes elem (cond-> ["form-select"]
                      (seq errors) (conj "is-invalid"))))

(defmethod forms/decorate [::forms/text ::forms/element ::bootstrap-5]
  [elem model _field errors {:keys [prepend append]}]
  (let [decorated (cond-> (add-class elem "form-control")
                    (v/valid? model) (add-class "is-valid")
                    (seq errors) (add-class "is-invalid"))]
    (if (or prepend append)
      [:div.input-group.mb-3 {:class (when (seq errors) "is-invalid")} ; adding is-invalid here triggers bootstraps invalid-feedback visbility
       prepend
       decorated
       append]
      decorated)))

(defmethod forms/decorate [::forms/text ::forms/field ::bootstrap-5]
  [[_ attr :as elem] model field errors {:keys [hide?] :as options}]
  (let [inner-decorated (forms/decorate elem
                                        model
                                        field
                                        errors
                                        (assoc-in options
                                                  [::forms/decoration ::forms/presentation]
                                                  ::forms/element))]
    [:div.mb-3 {:class (when (if (lib/derefable? hide?) @hide? hide?)
                         "d-none")}
     [:label.form-label {:for (:id attr)} (or (:caption options)
                                              (forms/->caption field))]
     (bs-4/help-popover field options)
     inner-decorated
     (when (and (v/validated? model)
                (seq errors))
       [:div.invalid-feedback errors])]))

(defmethod forms/decorate [::forms/typeahead ::forms/field ::bootstrap-5]
  [elem model field errors {:as options
                            :keys [hide? caption list-elem]}]
  [:div.mb-3 {:class (when (if (satisfies? IDeref hide?) @hide? hide?) "d-none")}
   [:label.form-label {:for (get-in elem [1 :id])}
    (or caption
        (forms/->caption field))]
   (bs-4/help-popover field options)
   (forms/decorate elem
                   model
                   field
                   errors
                   (update-in options [::forms/decoration] merge {::forms/target ::forms/text
                                                                  ::forms/presentation ::forms/element}))
   (bs-4/decorate-typeahead-list list-elem)
   [:div.invalid-feedback (v/validation-msg @model field)]])

(defn nav-tabs
  ([items]
   (nav-tabs {} items))
  ([options items]
   [:ul.nav.nav-tabs options
    (->> items
         (map (comp nav-item
                    expand-nav-item))
         doall)]))

(defn nav-pills
  ([items]
   (nav-pills {} items))
  ([options items]
   [:ul.nav.nav-pills options
    (->> items
         (map (comp nav-item
                    expand-nav-item))
         doall) ]))

(defn pagination
  [& args]
  (apply bs-4/pagination args))
