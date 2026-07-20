(ns dgknght.app-lib.forms-test
  (:require [cljs.test :refer-macros [deftest testing is]]
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

(deftest decimal-input-arithmatic
  (testing "default behavior"
    (let [model (r/atom {:amount nil})
          field [:amount]
          render-fn (forms/decimal-input model field {})
          [_tag {on-change :on-change}] (render-fn)]
      (on-change #js {:target #js {:value "9.99 * 1.0825"}})
      (is (= "10.814175" (str (get-in @model field)))
          "The value is not rounded.")))
  (testing "with :fraction-digits 2"
    (let [model (r/atom {:amount nil})
          field [:amount]
          render-fn (forms/decimal-input model field {:fraction-digits 2})
          [_tag {on-change :on-change}] (render-fn)]
      (on-change #js {:target #js {:value "9.99 * 1.0825"}})
      (is (= "10.81" (str (get-in @model field)))
          "The value is rounded to 2 digits."))))
