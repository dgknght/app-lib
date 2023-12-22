(ns dgknght.app-lib.log-capture
  (:require [clojure.test :refer [assert-expr do-report]]))

(defmacro with-log-capture
  [bindings & body]
  {:pre [(= 1 (count bindings))]}
  `(let [logs# (atom [])
         orig-fn# clojure.tools.logging/log*
         f# (fn* [~(first bindings)]
                 ~@body)]
     (with-redefs [clojure.tools.logging/log*
                   (fn [& args#]
                     (swap! logs# conj args#)
                     (apply orig-fn# args#))]
       (f# logs#))))

(defn extract-log-expectation
  ([body logs]
   {:body body
    :logs logs})
  ([level body logs]
   {:level level
    :body body
    :logs logs})
  ([level ex body logs]
   {:level level
    :ex ex
    :body body
    :logs logs}))

(defn log-matcher
  [exp]
  (->> exp
       (map (fn [[k v]]
              (case k
                :level #(= v (nth % 1))
                :ex (case (type v)
                      java.lang.Class #(instance? (nth % 2))
                      java.lang.String #(= v (.getMessage (nth % 2)))
                      java.util.regex.Pattern #(re-find v (nth % 2)))
                :body (case (type v)
                        java.lang.String #(= v (nth % 3))
                        java.util.regex.Pattern #(re-find v (nth % 3))))))
       (reduce comp)))

(defmethod assert-expr 'logged?
  [msg form]
  (let [body (nth form 1)
        logs (nth form 2)]
    `(let [logs# (deref ~logs)
           found# (->> logs#
                      (filter #(= ~body (nth % 3)))
                      first)
           type# (if found# :pass :fail)]
       (do-report {:type type#
                   :expected ~body
                   :actual logs#
                   :message ~msg}))))
