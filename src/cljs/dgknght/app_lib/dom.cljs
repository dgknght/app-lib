(ns dgknght.app-lib.dom
  (:refer-clojure :exclude [comment])
  (:require [dgknght.app-lib.client-macros :refer-macros [with-retry]]
            [goog.object :as obj]))

(defn body []
  (.-body js/document))

(defn target
  [event]
  (.-target event))

(defn value
  [elem]
  (.-value elem))

(defn prevent-default
  "Instruct the DOM event that the default behavior should be suppressed."
  [e]
  (.preventDefault e))

(defn bounding-client-rect
  ([elem] (bounding-client-rect elem {}))
  ([elem {:keys [as-clojure?]}]
   (let [raw (.getBoundingClientRect elem)]
     (if as-clojure?
       {:x (.-x raw)
        :y (.-y raw)
        :width (.-width raw)
        :height (.-height raw)
        :top (.-top raw)
        :bottom (.-bottom raw)
        :left (.-left raw)
        :right (.-right raw)}
       raw))))

(def ^:private key-codes
  {9  :tab
   13 :enter
   27 :escape
   37 :left
   38 :up
   39 :right
   40 :down})

(defn key-code
  "Get a keyword representing the keycode of the DOM event"
  [event]
  (get-in key-codes [(.-keyCode event)]))

(defn tab?
  "Return a boolean value indicating whether or not the event
  represents a press of the tab key"
  [event]
  (= :tab (key-code event)))

(defn enter?
  "Return a boolean value indicating whether or not the event
  represents a press of the enter key"
  [event]
  (= :enter (key-code event)))

(defn ctrl-key?
  "Return a boolean value indicating whether or not the event
  indicates the control key was pressed"
  [event]
  (.-ctrlKey event))

(defn shift-key?
  "Return a boolean value indicating whether or not the event
  indicates the shift key was pressed"
  [event]
  (.-shiftKey event))

(defn checked?
  [event]
  (.-checked (target event)))

(defn set-focus
  [id]
  (with-retry
    (.focus (.getElementById js/document id))))

(defn- resolve-elem
  [id-or-elem]
  (if (string? id-or-elem)
               (.getElementById js/document id-or-elem)
               id-or-elem))

(defn set-style
  [id-or-elem k v]
  (-> id-or-elem
      resolve-elem
      (obj/get "style")
      (obj/set k v)))

(defn get-style
  [id-or-elem k]
  (-> id-or-elem
      resolve-elem
      (obj/get "style")
      (obj/get k)))

(defn set-attribute
  [elem a-name a-value]
  (.setAttribute elem a-name a-value))

(defn debounce
  "Accepts a function and returns a function that will execute the given
  function after a delay (300 milliseconds unless otherwise specified) if
  the function is not invoked again during the delay (in which case case
  the timer starts again and the given fn invoked after the delay)."
  ([f] (debounce f 300))
  ([f timeout]
   (let [id (atom nil)]
     (fn [& args]
       (js/clearTimeout @id)
       (reset! id (js/setTimeout #(apply f args)
                                 timeout))))))
