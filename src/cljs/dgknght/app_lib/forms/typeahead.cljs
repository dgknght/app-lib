(ns dgknght.app-lib.forms.typeahead
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [dgknght.app-lib.core :as lib]
            [dgknght.app-lib.html :as html]
            [dgknght.app-lib.forms-validation :as v]
            [dgknght.app-lib.forms.common :refer [->id
                                                  ->name]]))

(defn- identity-callback
  [v callback]
  (callback v))

(defmulti ^:private handle-model-change
  (fn [_ _ {:keys [mode]}] mode))

(defmethod handle-model-change :default
  [before after {:keys [field
                        find-fn
                        text-value
                        caption-fn
                        index]}]
  (if-let [after-v (get-in after field)]
    (when (not= (get-in before field)
                after-v)
      (find-fn after-v
               #(reset! text-value (caption-fn %))))
    (do
      (reset! text-value "")
      (reset! index nil))))

(defmethod handle-model-change :direct
  [_ after {:keys [field text-value]}]
  (reset! text-value (get-in after field)))

(defn watch-model
  [{:keys [model field] :as options}]
  (add-watch model
             (cons ::typeahead field)
             (fn [_ _ before after]
               ; TODO: How to know if the model has changed externally
               ; and now needs to be re-rendered the the text-value
               ; vs. having been changed somewhere here and does
               ; not now need to be rerendered?
               (when-not before
                 (handle-model-change before after options)))))

(defn set-value
  [{:keys [model field find-fn text-value caption-fn]}]
    (when-let [current-value (get-in @model field)]
      (find-fn current-value #(reset! text-value (caption-fn %)))))

(defn list-elem
  [{:keys [field
           list-attr
           items
           index
           select-item
           list-caption-fn]
    :or {list-attr {}}}]
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

(defn- select-item
  [{:keys [value-fn
           caption-fn
           on-change
           items
           text-value
           model
           field
           mode]
    :or {on-change identity
         caption-fn identity}}]
  (fn [index]
    (let [item (lib/safe-nth @items index)]
      (reset! items nil)
      (if item
        (do
          (swap! model
                 assoc-in
                 field
                 (value-fn item))
          (if (= :direct mode)
            (reset! text-value (value-fn item))
            (reset! text-value (caption-fn item)))
          (on-change item))
        (when-not (= :direct mode)
          (swap! model assoc-in field nil)))
      (v/validate model field))))

(defn- on-key-down
  [{:keys [model
           field
           find-fn
           caption-fn
           items
           index
           text-value
           select-item]
    :or {find-fn identity
         caption-fn identity}}]
  (fn [e]
    (if @items
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
        nil)
      (when (and (not (seq @text-value))
                 (#{:enter :tab} (html/key-code e)))
        (select-item nil)))))

(defn- nil-search
  [_term callback]
  (callback []))

(defn- on-change
  [{:keys [text-value
           items
           max-items
           search-fn
           model
           field
           mode]
    :or {search-fn nil-search}}]
  (fn [e]
    (let [raw-value (-> e html/target html/value)]
      (reset! text-value raw-value)
      (when (= :direct mode)
        (swap! model assoc-in field raw-value)
        (v/validate model field))
      (if (empty? raw-value)
        (reset! items nil)
        (search-fn raw-value #(->> %
                                   (take max-items)
                                   (reset! items)))))))

(defn- on-focus
  [{:keys [model
           field
           search-fn
           max-items
           items
           min-input-length]}]
  (fn [_e]
    (when (and (nil? (get-in @model field))
               (zero? min-input-length))
      (search-fn nil #(->> %
                           (take max-items)
                           (reset! items))))))

(defn- assoc-with-fn
  [m k f]
  (assoc m k (f m)))

(defn state
  [model field {:keys [mode
                       caption-fn
                       list-caption-fn
                       value-fn
                       find-fn]
                :or {caption-fn identity}
                :as options}]
  {:pre [(contains? #{:direct :indirect nil}
                    (:mode options))]}
  (-> {:max-items 10
       :min-input-length 1}
      (merge options
             {:model model
              :field field
              :text-value (r/atom "")
              :items (r/atom nil)
              :index (r/atom nil)
              :caption-fn caption-fn
              :value-fn (or value-fn (if (= mode :direct)
                                       #(get-in % field)
                                       identity))
              :list-caption-fn (or list-caption-fn
                                   caption-fn)
              :find-fn (or find-fn identity-callback)})
      (update-in [:html :id] (fnil identity (->id field)))
      (assoc-with-fn :select-item select-item)
      (assoc-with-fn :on-key-down on-key-down)
      (assoc-with-fn :on-change on-change)
      (assoc-with-fn :on-focus on-focus)))

(defn elem
  [{:keys [model
           field
           items
           validations
           html
           text-value
           on-change
           on-key-down
           on-focus]
    {:keys [on-key-up]} :html}]
  (v/add-rules model field validations)
  [:input
   (merge {:id (->id field)}
          html
          {:type :text
           :auto-complete :off
           :name (->name field)
           :value @text-value
           :on-key-down on-key-down
           :on-key-up (when on-key-up
                        #(when-not @items
                           (on-key-up %)))
           :on-change on-change
           :on-focus on-focus})])
