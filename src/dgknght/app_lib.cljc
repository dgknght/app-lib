(ns dgknght.app-lib
  (:require #?(:clj [clojure.pprint :refer [pprint]]))
  #?(:clj (:import java.lang.Integer)))

(defn trace
  [msg]
  #?(:clj (pprint msg)
     :cljs (.log js/console (prn-str msg))))

(defn parse-int
  [v]
  (when v
    #?(:clj (Integer/parseInt v)
       :cljs (js/parseInt v))))
