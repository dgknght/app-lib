(ns dgknght.app-lib.web-mocks.impl
  (:require [dgknght.app-lib.core :as lib]
            [dgknght.app-lib.web-mocks.matching :refer [match?
                                                        meets-spec?
                                                        readable]]))

(defn called?
  [msg form]
  (let [spec (lib/safe-nth form 1)
        calls (lib/safe-nth form 2)
        matcher (lib/safe-nth form 3)]
    `(let [calls# (deref ~calls)
           matches# (filter #(match? % ~matcher) calls#)
           pass?# (meets-spec? (count matches#) ~spec)]
       ; TODO: this renders a readable error in clojurescript test results, but not in clojure test results
       {:expected (lib/format "%s called %s" (readable ~matcher) ~spec)
        :actual (lib/format "Found %s match(es) among %s"
                            (count matches#)
                            (mapv #(dissoc % :channel) calls#))
        :message ~msg
        :type (if pass?# :pass :fail)})))

(defn not-called?
  [msg form]
  (let [calls (lib/safe-nth form 1)
        matcher (lib/safe-nth form 2)]
    `(let [matches# (filter #(match? % ~matcher) (deref ~calls))
           pass?# (empty? matches#)]
       {:expected "no matching calls"
        :actual matches#
        :message ~msg
        :type (if pass?# :pass :fail)})))

(defn matches-headers?
  [{:keys [headers]} expected]
  (every? (fn [[k v]]
            (= v (get-in headers [k])))
          expected))

(defn called-with-headers?
  [msg form]
  (let [spec (lib/safe-nth form 1)
        calls (lib/safe-nth form 2)
        expected (lib/safe-nth form 3)]
    `(let [matches# (filter #(matches-headers? % ~expected) (deref ~calls))
           pass?# (meets-spec? (count matches#) ~spec)]
       {:expected {:headers ~expected
                   :spec ~spec}
        :actual {:matches (count matches#)
                 :calls (deref ~calls)}
        :message ~msg
        :type (if pass?# :pass :fail)})))
