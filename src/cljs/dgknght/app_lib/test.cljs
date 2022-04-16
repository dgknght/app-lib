(ns dgknght.app-lib.test
  (:require-macros [cljs.test]
                   [dgknght.app-lib.core :refer [safe-nth
                                                 prune-to]]
                   [dgknght.app-lib.test-cljs]))

(when (exists? js/cljs.test$macros)
  (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test/comparable?
    [_menv msg form]
    (let [expected (safe-nth form 1)
          actual (safe-nth form 2)
          ks (->> form
                  (drop 3)
                  (into []))]
      `(let [expected# (if (seq ~ks)
                         (select-keys ~expected ~ks)
                         ~expected)
             actual# (prune-to ~actual expected#)
             result# (if (= expected# actual#)
                       :pass
                       :fail)
             diffs# (when (= :fail result#)
                      (d/diff expected# actual#))]
         (t/do-report
           {:expected expected#
            :actual actual#
            :message ~msg
            :type result#
            :diffs [[actual# (take 2 diffs#)]]})))))
