(ns dgknght.app-lib.dom
  (:refer-clojure :exclude [comment])
  (:require [dgknght.app-lib.client-macros :refer-macros [with-retry]]
            [goog.object :as obj]))

(defn target
  [event]
  (.-target event))

(defn value
  [elem]
  (.-value elem))

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
  [event]
  (get-in key-codes [(.-keyCode event)]))

(defn ctrl-key?
  [event]
  (.-ctrlKey event))

(defn shift-key?
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

(defn debounce
  ([f] (debounce f 300))
  ([f timeout]
   (let [id (atom nil)]
     (fn [& args]
       (js/clearTimeout @id)
       (reset! id (js/setTimeout #(apply f args)
                                 timeout))))))
