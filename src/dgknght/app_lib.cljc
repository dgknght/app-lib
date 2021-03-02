(ns dgknght.app-lib
  #?(:clj (:import java.lang.Integer)))

(defn parse-int
  [v]
  (when v
    #?(:clj (Integer/parseInt v)
       :cljs (js/parseInt v))))
