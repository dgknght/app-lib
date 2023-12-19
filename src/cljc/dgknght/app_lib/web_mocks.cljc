(ns dgknght.app-lib.web-mocks
  #?(:cljs (:require-macros [dgknght.app-lib.web-mocks.cljs]))
  (:require [lambdaisland.uri :refer [query-string->map]]
            [dgknght.app-lib.web-mocks.matching :refer [match?]]
            [dgknght.app-lib.web-mocks.impl :as impl]
            ; If I reference clj-http and cljs-http here, it causes
            ; a compliation failure when running lein doo
            ; It appears the CLJ phase of the CLJS compilcation
            ; is unable to resolve clj-http
            #?(:clj [clojure.pprint :refer [pprint]]
               :cljs [cljs.pprint :refer [pprint]])
            #?(:cljs [cljs.core.async :as a])
            #?(:clj [clojure.test :as test])))

(defn parse-query-string
  [{:keys [query-string] :as m}]
  (if query-string
    (assoc m :query-params (query-string->map query-string))
    m))

(defn- call-matching-mock
  [req mocks]
  (or (->> mocks
           (filter #(match? req (first %)))
           (map #((second %) req))
           first)
      (pprint {::unable-to-match-request req
               ::mocks mocks})))

(defn request-handler
  [calls mocks]
  (fn [req]
    (swap! calls conj req)
    #?(:clj (call-matching-mock req mocks)
       :cljs (let [ch (:channel req)
                   res (call-matching-mock req mocks)]
               (if res
                 (a/put! ch res)
                 (pprint {::unable-to-match-request req
                          ::mocks mocks}))
               (a/close! ch)
               ch))))

(defmacro with-web-mocks
  [bindings mocks-map & body]
  `(let [f# (fn* [~(first bindings)] ~@body)
         calls# (atom [])]
     (with-redefs [http/request (comp (request-handler calls# ~mocks-map)
                                      parse-query-string)]
       (f# calls#))))

#?(:clj
   (defmethod test/assert-expr 'called?
     [msg form]
     `(test/do-report
        ~(impl/called? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.web-mocks/called?
       [_env msg form]
       `(test/do-report
        ~(impl/called? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'not-called?
     [msg form]
     `(test/do-report
        ~(impl/not-called? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.web-mocks/not-called?
       [_env msg form]
       `(test/do-report
        ~(impl/not-called? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'matches-headers?
     [msg form]
     `(test/do-report
        ~(impl/matches-headers? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.web-mocks/matches-headers?
       [_env msg form]
       `(test/do-report
        ~(impl/matches-headers? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'called-with-headers?
     [msg form]
     `(test/do-report
        ~(impl/called-with-headers? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.web-mocks/called-with-headers?
       [_env msg form]
       `(test/do-report
        ~(impl/called-with-headers? msg form)))))
