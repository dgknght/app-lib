(ns dgknght.app-lib.forms-test
  (:require [cljs.test :refer-macros [deftest is]]
            [reagent.core :as r]
            [dgknght.app-lib.forms :as forms]))

(deftest integer-input-renders-min-max-html-attributes
  (let [model (r/atom {:amount 5})
        field [:amount]
        render-fn (forms/integer-input model field {:min 0 :max 100})
        [tag {min-attr :min max-attr :max}] (render-fn)]
    (is (= :input tag))
    (is (= 0 min-attr))
    (is (= 100 max-attr))))
