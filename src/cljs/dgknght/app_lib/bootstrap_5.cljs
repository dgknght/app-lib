(ns dgknght.app-lib.bootstrap-5
  "Functions to build bootstrap 5.x web elements"
  (:require [dgknght.app-lib.inflection :refer [title-case]]
            [dgknght.app-lib.notifications :as notify]
            [dgknght.app-lib.html :refer [add-class]]
            [dgknght.app-lib.forms :as forms]
            [dgknght.app-lib.bootstrap-4 :as bs-4]
            [dgknght.app-lib.bootstrap-icons :as icons]))

(derive ::bootstrap-5 ::bs-4/bootstrap-4)

(defn- nav-item
  [{:keys [id path active? label nav-fn]
    :or {path "#"}}]
  ^{:key (str "nav-item-" (name id))}
  [:li.nav-item
   [:a.nav-link (cond-> {:href path}
                  nav-fn (assoc :on-click nav-fn)
                  active? (assoc :class "active"
                                 :aria-current "page"))
    (or label (title-case id))]])

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
     [:span.navbar-toggler-icon]]
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

(def ^:private icon-data
  {:arrow-left-short [[:path {:d "M7.854 4.646a.5.5 0 010 .708L5.207 8l2.647 2.646a.5.5 0 01-.708.708l-3-3a.5.5 0 010-.708l3-3a.5.5 0 01.708 0z"}]
                      [:path {:d "M4.5 8a.5.5 0 01.5-.5h6.5a.5.5 0 010 1H5a.5.5 0 01-.5-.5z"}]]
   :arrow-repeat     [[:path {:d "M2.854 7.146a.5.5 0 00-.708 0l-2 2a.5.5 0 10.708.708L2.5 8.207l1.646 1.647a.5.5 0 00.708-.708l-2-2zm13-1a.5.5 0 00-.708 0L13.5 7.793l-1.646-1.647a.5.5 0 00-.708.708l2 2a.5.5 0 00.708 0l2-2a.5.5 0 000-.708z"}]
                      [:path {:d "M8 3a4.995 4.995 0 00-4.192 2.273.5.5 0 01-.837-.546A6 6 0 0114 8a.5.5 0 01-1.001 0 5 5 0 00-5-5zM2.5 7.5A.5.5 0 013 8a5 5 0 009.192 2.727.5.5 0 11.837.546A6 6 0 012 8a.5.5 0 01.501-.5z"}]]
   :arrows-collapse  [[:path {:d "M2 8a.5.5 0 01.5-.5h11a.5.5 0 010 1h-11A.5.5 0 012 8zm6-7a.5.5 0 01.5.5V6a.5.5 0 01-1 0V1.5A.5.5 0 018 1z"}]
                      [:path {:d "M10.354 3.646a.5.5 0 010 .708l-2 2a.5.5 0 01-.708 0l-2-2a.5.5 0 11.708-.708L8 5.293l1.646-1.647a.5.5 0 01.708 0zM8 15a.5.5 0 00.5-.5V10a.5.5 0 00-1 0v4.5a.5.5 0 00.5.5z"}]
                      [:path {:d "M10.354 12.354a.5.5 0 000-.708l-2-2a.5.5 0 00-.708 0l-2 2a.5.5 0 00.708.708L8 10.707l1.646 1.647a.5.5 0 00.708 0z"}]]
   :arrows-expand    [[:path {:d "M2 8a.5.5 0 01.5-.5h11a.5.5 0 010 1h-11A.5.5 0 012 8zm6-1.5a.5.5 0 00.5-.5V1.5a.5.5 0 00-1 0V6a.5.5 0 00.5.5z"}]
                      [:path {:d "M10.354 3.854a.5.5 0 000-.708l-2-2a.5.5 0 00-.708 0l-2 2a.5.5 0 10.708.708L8 2.207l1.646 1.647a.5.5 0 00.708 0zM8 9.5a.5.5 0 01.5.5v4.5a.5.5 0 01-1 0V10a.5.5 0 01.5-.5z"}]
                      [:path {:d "M10.354 12.146a.5.5 0 010 .708l-2 2a.5.5 0 01-.708 0l-2-2a.5.5 0 01.708-.708L8 13.793l1.646-1.647a.5.5 0 01.708 0z"}]]
   :box-arrow-in-left [[:path {:d "M10 3.5a.5.5 0 0 0-.5-.5h-8a.5.5 0 0 0-.5.5v9a.5.5 0 0 0 .5.5h8a.5.5 0 0 0 .5-.5v-2a.5.5 0 0 1 1 0v2A1.5 1.5 0 0 1 9.5 14h-8A1.5 1.5 0 0 1 0 12.5v-9A1.5 1.5 0 0 1 1.5 2h8A1.5 1.5 0 0 1 11 3.5v2a.5.5 0 0 1-1 0v-2z"}]
                       [:path {:d "M4.146 8.354a.5.5 0 0 1 0-.708l3-3a.5.5 0 1 1 .708.708L5.707 7.5H14.5a.5.5 0 0 1 0 1H5.707l2.147 2.146a.5.5 0 0 1-.708.708l-3-3z"}]]
   :box-arrow-right [[:path {:d "M10 12.5a.5.5 0 0 1-.5.5h-8a.5.5 0 0 1-.5-.5v-9a.5.5 0 0 1 .5-.5h8a.5.5 0 0 1 .5.5v2a.5.5 0 0 0 1 0v-2A1.5 1.5 0 0 0 9.5 2h-8A1.5 1.5 0 0 0 0 3.5v9A1.5 1.5 0 0 0 1.5 14h8a1.5 1.5 0 0 0 1.5-1.5v-2a.5.5 0 0 0-1 0v2z"}]
                     [:path {:d "M15.854 8.354a.5.5 0 0 0 0-.708l-3-3a.5.5 0 0 0-.708.708L14.293 7.5H5.5a.5.5 0 0 0 0 1h8.793l-2.147 2.146a.5.5 0 0 0 .708.708l3-3z"}]]
   :calendar         [[:path {:fill-rule "evenodd"
                              :d "M1 4v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V4H1zm1-3a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V3a2 2 0 0 0-2-2H2z"}]
                      [:path {:fill-rule "evenodd"
                              :d "M3.5 0a.5.5 0 0 1 .5.5V1a.5.5 0 0 1-1 0V.5a.5.5 0 0 1 .5-.5zm9 0a.5.5 0 0 1 .5.5V1a.5.5 0 0 1-1 0V.5a.5.5 0 0 1 .5-.5z"}]]
   :check            [[:path {:d "M13.854 3.646a.5.5 0 010 .708l-7 7a.5.5 0 01-.708 0l-3.5-3.5a.5.5 0 11.708-.708L6.5 10.293l6.646-6.647a.5.5 0 01.708 0z"}]]
   :check-box        [[:path {:d "M15.354 2.646a.5.5 0 010 .708l-7 7a.5.5 0 01-.708 0l-3-3a.5.5 0 11.708-.708L8 9.293l6.646-6.647a.5.5 0 01.708 0z"}]
                      [:path {:d "M1.5 13A1.5 1.5 0 003 14.5h10a1.5 1.5 0 001.5-1.5V8a.5.5 0 00-1 0v5a.5.5 0 01-.5.5H3a.5.5 0 01-.5-.5V3a.5.5 0 01.5-.5h8a.5.5 0 000-1H3A1.5 1.5 0 001.5 3v10z"}]]
   :collection       [[:path {:d "M14.5 13.5h-13A.5.5 0 011 13V6a.5.5 0 01.5-.5h13a.5.5 0 01.5.5v7a.5.5 0 01-.5.5zm-13 1A1.5 1.5 0 010 13V6a1.5 1.5 0 011.5-1.5h13A1.5 1.5 0 0116 6v7a1.5 1.5 0 01-1.5 1.5h-13zM2 3a.5.5 0 00.5.5h11a.5.5 0 000-1h-11A.5.5 0 002 3zm2-2a.5.5 0 00.5.5h7a.5.5 0 000-1h-7A.5.5 0 004 1z"}]]
   :download         [[:path {:d "M.5 8a.5.5 0 0 1 .5.5V12a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V8.5a.5.5 0 0 1 1 0V12a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V8.5A.5.5 0 0 1 .5 8z"}]
                      [:path {:d "M5 7.5a.5.5 0 0 1 .707 0L8 9.793 10.293 7.5a.5.5 0 1 1 .707.707l-2.646 2.647a.5.5 0 0 1-.708 0L5 8.207A.5.5 0 0 1 5 7.5z"}]
                      [:path {:d "M8 1a.5.5 0 0 1 .5.5v8a.5.5 0 0 1-1 0v-8A.5.5 0 0 1 8 1z"}]]
   :dot              [[:path {:d "M8 9.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z"}]]
   :eye              [[:path {:d "M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.134 13.134 0 001.66 2.043C4.12 11.332 5.88 12.5 8 12.5c2.12 0 3.879-1.168 5.168-2.457A13.134 13.134 0 0014.828 8a13.133 13.133 0 00-1.66-2.043C11.879 4.668 10.119 3.5 8 3.5c-2.12 0-3.879 1.168-5.168 2.457A13.133 13.133 0 001.172 8z"}]
                      [:path {:d "M8 5.5a2.5 2.5 0 100 5 2.5 2.5 0 000-5zM4.5 8a3.5 3.5 0 117 0 3.5 3.5 0 01-7 0z"}]]
   :file-arrow-up    [[:path {:d "M4 1h8a2 2 0 012 2v10a2 2 0 01-2 2H4a2 2 0 01-2-2V3a2 2 0 012-2zm0 1a1 1 0 00-1 1v10a1 1 0 001 1h8a1 1 0 001-1V3a1 1 0 00-1-1H4z"}]
                      [:path {:d "M4.646 7.854a.5.5 0 00.708 0L8 5.207l2.646 2.647a.5.5 0 00.708-.708l-3-3a.5.5 0 00-.708 0l-3 3a.5.5 0 000 .708z"}]
                      [:path {:d "M8 12a.5.5 0 00.5-.5v-6a.5.5 0 00-1 0v6a.5.5 0 00.5.5z"}]]
   :gear             [[:path {:d "M9.405 1.05c-.413-1.4-2.397-1.4-2.81 0l-.1.34a1.464 1.464 0 0 1-2.105.872l-.31-.17c-1.283-.698-2.686.705-1.987 1.987l.169.311c.446.82.023 1.841-.872 2.105l-.34.1c-1.4.413-1.4 2.397 0 2.81l.34.1a1.464 1.464 0 0 1 .872 2.105l-.17.31c-.698 1.283.705 2.686 1.987 1.987l.311-.169a1.464 1.464 0 0 1 2.105.872l.1.34c.413 1.4 2.397 1.4 2.81 0l.1-.34a1.464 1.464 0 0 1 2.105-.872l.31.17c1.283.698 2.686-.705 1.987-1.987l-.169-.311a1.464 1.464 0 0 1 .872-2.105l.34-.1c1.4-.413 1.4-2.397 0-2.81l-.34-.1a1.464 1.464 0 0 1-.872-2.105l.17-.31c.698-1.283-.705-2.686-1.987-1.987l-.311.169a1.464 1.464 0 0 1-2.105-.872l-.1-.34zM8 10.93a2.929 2.929 0 1 0 0-5.86 2.929 2.929 0 0 0 0 5.858z"}]]
   :pencil           [[:path {:d "M11.293 1.293a1 1 0 011.414 0l2 2a1 1 0 010 1.414l-9 9a1 1 0 01-.39.242l-3 1a1 1 0 01-1.266-1.265l1-3a1 1 0 01.242-.391l9-9zM12 2l2 2-9 9-3 1 1-3 9-9z"}]
                      [:path {:d "M12.146 6.354l-2.5-2.5.708-.708 2.5 2.5-.707.708zM3 10v.5a.5.5 0 00.5.5H4v.5a.5.5 0 00.5.5H5v.5a.5.5 0 00.5.5H6v-1.5a.5.5 0 00-.5-.5H5v-.5a.5.5 0 00-.5-.5H3z"}]]
   :paperclip        [[:path {:d "M4.5 3a2.5 2.5 0 0 1 5 0v9a1.5 1.5 0 0 1-3 0V5a.5.5 0 0 1 1 0v7a.5.5 0 0 0 1 0V3a1.5 1.5 0 1 0-3 0v9a2.5 2.5 0 0 0 5 0V5a.5.5 0 0 1 1 0v7a3.5 3.5 0 1 1-7 0V3z"}]]
   :play             [[:path {:d "M10.804 8L5 4.633v6.734L10.804 8zm.792-.696a.802.802 0 010 1.392l-6.363 3.692C4.713 12.69 4 12.345 4 11.692V4.308c0-.653.713-.998 1.233-.696l6.363 3.692z"}]]
   :plus             [[:path {:d "M8 3.5a.5.5 0 01.5.5v4a.5.5 0 01-.5.5H4a.5.5 0 010-1h3.5V4a.5.5 0 01.5-.5z"}]
                      [:path {:d "M7.5 8a.5.5 0 01.5-.5h4a.5.5 0 010 1H8.5V12a.5.5 0 01-1 0V8z"}]]
   :plus-circle      [[:path {:d "M8 3.5a.5.5 0 01.5.5v4a.5.5 0 01-.5.5H4a.5.5 0 010-1h3.5V4a.5.5 0 01.5-.5z"}]
                      [:path {:d "M7.5 8a.5.5 0 01.5-.5h4a.5.5 0 010 1H8.5V12a.5.5 0 01-1 0V8z"}]
                      [:path {:d "M8 15A7 7 0 108 1a7 7 0 000 14zm0 1A8 8 0 108 0a8 8 0 000 16z"}]]
   :search           [[:path {:d "M10.442 10.442a1 1 0 0 1 1.415 0l3.85 3.85a1 1 0 0 1-1.414 1.415l-3.85-3.85a1 1 0 0 1 0-1.415z"}]
                      [:path {:d "M6.5 12a5.5 5.5 0 1 0 0-11 5.5 5.5 0 0 0 0 11zM13 6.5a6.5 6.5 0 1 1-13 0 6.5 6.5 0 0 1 13 0z"}]]
   :stop             [[:path {:d "M3.5 5A1.5 1.5 0 015 3.5h6A1.5 1.5 0 0112.5 5v6a1.5 1.5 0 01-1.5 1.5H5A1.5 1.5 0 013.5 11V5zM5 4.5a.5.5 0 00-.5.5v6a.5.5 0 00.5.5h6a.5.5 0 00.5-.5V5a.5.5 0 00-.5-.5H5z"}]]
   :tools            [[:path {:d "M0 1l1-1 3.081 2.2a1 1 0 0 1 .419.815v.07a1 1 0 0 0 .293.708L10.5 9.5l.914-.305a1 1 0 0 1 1.023.242l3.356 3.356a1 1 0 0 1 0 1.414l-1.586 1.586a1 1 0 0 1-1.414 0l-3.356-3.356a1 1 0 0 1-.242-1.023L9.5 10.5 3.793 4.793a1 1 0 0 0-.707-.293h-.071a1 1 0 0 1-.814-.419L0 1zm11.354 9.646a.5.5 0 0 0-.708.708l3 3a.5.5 0 0 0 .708-.708l-3-3z"}]
                      [:path {:d "M15.898 2.223a3.003 3.003 0 0 1-3.679 3.674L5.878 12.15a3 3 0 1 1-2.027-2.027l6.252-6.341A3 3 0 0 1 13.778.1l-2.142 2.142L12 4l1.757.364 2.141-2.141zm-13.37 9.019L3.001 11l.471.242.529.026.287.445.445.287.026.529L5 13l-.242.471-.026.529-.445.287-.287.445-.529.026L3 15l-.471-.242L2 14.732l-.287-.445L1.268 14l-.026-.529L1 13l.242-.471.026-.529.445-.287.287-.445.529-.026z"}]]
   :unchecked-box    [[:path {:d "M1.5 13A1.5 1.5 0 003 14.5h10a1.5 1.5 0 001.5-1.5V8a.5.5 0 00-1 0v5a.5.5 0 01-.5.5H3a.5.5 0 01-.5-.5V3a.5.5 0 01.5-.5h8a.5.5 0 000-1H3A1.5 1.5 0 001.5 3v10z"}]]
   :x                [[:path {:d "M11.854 4.146a.5.5 0 010 .708l-7 7a.5.5 0 01-.708-.708l7-7a.5.5 0 01.708 0z"}]
                      [:path {:d "M4.146 4.146a.5.5 0 000 .708l7 7a.5.5 0 00.708-.708l-7-7a.5.5 0 00-.708 0z"}]]
   :x-circle         [[:path {:fill-rule "evenodd"
                              :d "M8 15A7 7 0 108 1a7 7 0 000 14zm0 1A8 8 0 108 0a8 8 0 000 16z"}]
                      [:path {:fill-rule "evenodd"
                              :d "M11.854 4.146a.5.5 0 010 .708l-7 7a.5.5 0 01-.708-.708l7-7a.5.5 0 01.708 0z"}]
                      [:path {:fill-rule "evenodd"
                              :d "M4.146 4.146a.5.5 0 000 .708l7 7a.5.5 0 00.708-.708l-7-7a.5.5 0 00-.708 0z"}]]})

(def ^:private attrs
  {:xmlns "http://www.w3.org/2000/svg"
   :viewBox "0 0 16 16"
   :width 32
   :height 32
   :fill "currentColor"})

(def ^:private weights
  {:ultra-light 0.5
   :thin        1
   :light       1.5
   :regular     2
   :medium      2.5
   :bold        3
   :heavy       3.5})

(defn- apply-weight
  [{:keys [weight] :as opts}]
  (let [width (get-in weights [weight])]
    (cond-> (dissoc opts :weight)
      width (assoc :stroke-width width))))

(def ^:private sizes
  {:small 16
   :large 32})

(defn- apply-size
  [{:keys [size] :as opts}]
  (let [pixels (get-in sizes [size])]
    (cond-> (dissoc opts :size)
      pixels (assoc :width pixels
                    :height pixels))))

(defn- apply-style
  [{:keys [style] :as opts}]
  (cond-> (dissoc opts :style)
    (= :round style) (assoc :stroke-linejoin :round
                            :stroke-linecap :round)
    (= :bevel style) (assoc :stroke-linejoin :bevel
                            :stroke-linecap :butt)
    (= :miter style) (assoc :stroke-linejoin :miter
                            :stroke-linecap :butt)))

(defn- prepare-opts
  [opts]
  (-> opts
      apply-weight
      apply-size
      apply-style))

(defn icon
  ([icon-id] (icon icon-id {}))
  ([icon-id opts]
   (apply vector
          :svg.bi
          (->> opts
               prepare-opts
               (merge attrs))
          (get-in icon-data [icon-id]))))

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
    [:button.btn-close {:aria-label "Close"
                        :on-click #(notify/untoast id)}]]
   [:div.toast-body
    body]])

(defn alert
  [{:keys [id severity message] :as a}]
  ^{:key (str "alert-" id)}
  [:div.alert.alert-dismissible {:class (str "alert-" (name severity))
                                 :role :alert}
   message
   [:button.btn-close {:type :button
                       :on-click #(notify/unnotify a)
                       :aria-label "Close"}]])

(defn icon-with-text
  ([icon-key text options]
   [:span.d-flex.align-items-center
    (icon icon-key options)
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
