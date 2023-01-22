(ns dgknght.app-lib.validation-helpers
  (:require [dgknght.app-lib.forms-validation :as v]
            [cljs.core.async :as a]))

(defmacro validate
  [model field validators & body]
  `(let [xf# (v/validator ~field ~validators)
         ch# (a/chan 1 xf#)
         f# (fn* [] ~@body)]
     (a/go (a/<! ch#)
           (f#))
     (a/go (a/put! ch# ~model)
           (a/close! ch#))))
