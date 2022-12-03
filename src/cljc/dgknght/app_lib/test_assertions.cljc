(ns dgknght.app-lib.test-assertions
  #?(:cljs (:require-macros [dgknght.app-lib.test-assertions.cljs]))
  (:require #?(:clj [clojure.test :as test]
               :cljs [cljs.test :as test])
            [dgknght.app-lib.test-assertions.impl :as impl]))

#?(:clj
   (defmethod test/assert-expr 'valid?
     [msg form]
     `(test/report
        ~(impl/valid? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/valid?
       [_env msg form]
       `(test/report
        ~(impl/valid? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'invalid?
     [msg form]
     `(test/report
        ~(impl/invalid? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/invalid?
       [_env msg form]
       `(test/report
        ~(impl/invalid? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'comparable?
     [msg form]
     `(test/report
        ~(impl/comparable? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/comparable?
       [_env msg form]
       `(test/report
        ~(impl/comparable? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'seq-of-maps-like?
     [msg form]
     `(test/report
        ~(impl/seq-of-maps-like? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/seq-of-maps-like?
       [_env msg form]
       `(test/report
        ~(impl/seq-of-maps-like? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'seq-with-map-like?
     [msg form]
     `(test/report
        ~(impl/seq-with-map-like? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/seq-with-map-like?
       [_env msg form]
       `(test/report
        ~(impl/seq-with-map-like? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'seq-with-no-map-like?
     [msg form]
     `(test/report
        ~(impl/seq-with-no-map-like? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/seq-with-no-map-like?
       [_env msg form]
       `(test/report
        ~(impl/seq-with-no-map-like? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'seq-containing-value?
     [msg form]
     `(test/report
        ~(impl/seq-containing-value? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/seq-containing-value?
       [_env msg form]
       `(test/report
        ~(impl/seq-containing-value? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'seq-excluding-value?
     [msg form]
     `(test/report
        ~(impl/seq-excluding-value? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/seq-excluding-value?
       [_env msg form]
       `(test/report
        ~(impl/seq-excluding-value? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'seq-containing-model?
     [msg form]
     `(test/report
        ~(impl/seq-containing-model? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/seq-containing-model?
       [_env msg form]
       `(test/report
        ~(impl/seq-containing-model? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'seq-excluding-model?
     [msg form]
     `(test/report
        ~(impl/seq-excluding-model? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/seq-excluding-model?
       [_env msg form]
       `(test/report
        ~(impl/seq-excluding-model? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'http-redirect-to?
     [msg form]
     `(test/report
        ~(impl/http-redirect-to? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/http-redirect-to?
       [_env msg form]
       `(test/report
          ~(impl/http-redirect-to? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'http-success?
     [msg form]
     `(test/report
        ~(impl/http-success? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/http-success?
       [_env msg form]
       `(test/report
          ~(impl/http-success? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'http-created?
     [msg form]
     `(test/report
        ~(impl/assert-http-status 201 msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/http-created?
       [_env msg form]
       `(test/report
          ~(impl/assert-http-status 201 msg form)))))

#?(:clj
   (defmethod test/assert-expr 'http-no-content?
     [msg form]
     `(test/report
        ~(impl/assert-http-status 204 msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/http-no-content?
       [_env msg form]
       `(test/report
          ~(impl/assert-http-status 204 msg form)))))

#?(:clj
   (defmethod test/assert-expr 'http-bad-request?
     [msg form]
     `(test/report
        ~(impl/assert-http-status 400 msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/http-bad-request?
       [_env msg form]
       `(test/report
          ~(impl/assert-http-status 400 msg form)))))

#?(:clj
   (defmethod test/assert-expr 'http-unauthorized?
     [msg form]
     `(test/report
        ~(impl/assert-http-status 401 msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/http-unauthorized?
       [_env msg form]
       `(test/report
          ~(impl/assert-http-status 401 msg form)))))

#?(:clj
   (defmethod test/assert-expr 'http-forbidden?
     [msg form]
     `(test/report
        ~(impl/assert-http-status 403 msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/http-forbidden?
       [_env msg form]
       `(test/report
          ~(impl/assert-http-status 403 msg form)))))

#?(:clj
   (defmethod test/assert-expr 'http-not-found?
     [msg form]
     `(test/report
        ~(impl/assert-http-status 404 msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/http-not-found?
       [_env msg form]
       `(test/report
          ~(impl/assert-http-status 404 msg form)))))

#?(:clj
   (defmethod test/assert-expr 'http-teapot?
     [msg form]
     `(test/report
        ~(impl/assert-http-status 418 msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/http-teapot?
       [_env msg form]
       `(test/report
          ~(impl/assert-http-status 418 msg form)))))

#?(:clj
   (defmethod test/assert-expr 'http-unprocessable?
     [msg form]
     `(test/report
        ~(impl/assert-http-status 422 msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/http-unprocessable?
       [_env msg form]
       `(test/report
          ~(impl/assert-http-status 422 msg form)))))

#?(:clj
   (defmethod test/assert-expr 'url-like?
     [msg form]
     `(test/report
        ~(impl/url-like? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/url-like?
       [_env msg form]
       `(test/report
          ~(impl/url-like? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'not-url-like?
     [msg form]
     `(test/report
        ~(impl/not-url-like? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/not-url-like?
       [_env msg form]
       `(test/report
          ~(impl/not-url-like? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'mime-msg-containing?
     [msg form]
     `(test/report
        ~(impl/mime-msg-containing? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/mime-msg-containing?
       [_env msg form]
       `(test/report
        ~(impl/mime-msg-containing? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'mime-msg-not-containing?
     [msg form]
     `(test/report
        ~(impl/mime-msg-not-containing? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/mime-msg-not-containing?
       [_env msg form]
       `(test/report
        ~(impl/mime-msg-not-containing? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'http-response-with-cookie?
     [msg form]
     `(test/report
        ~(impl/http-response-with-cookie? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/http-response-with-cookie?
       [_env msg form]
       `(test/report
        ~(impl/http-response-with-cookie? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'http-response-without-cookie?
     [msg form]
     `(test/report
        ~(impl/http-response-without-cookie? msg form)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/http-response-without-cookie?
       [_env msg form]
       `(test/report
        ~(impl/http-response-without-cookie? msg form)))))

#?(:clj
   (defmethod test/assert-expr 'html-response-with-content?
     [msg form]
     `(test/report
        ~(impl/html-response-with-content? msg form))))

#?(:clj
   (defmethod test/assert-expr 'same-date?
     [msg form]
     `(test/report
        ~(impl/same-date? msg form))))

#?(:clj
   (defmethod test/assert-expr 'conformant?
     [msg form]
     `(test/report
        ~(impl/conformant? msg
                           form
                           'clojure.spec.alpha/valid?
                           'clojure.spec.alpha/explain-str)))
   :cljs
   (when (exists? js/cljs.test$macros)
     (defmethod js/cljs.test$macros.assert_expr 'dgknght.app-lib.test-assertions/conformant?
       [_env msg form]
       `(test/report
        ~(impl/conformant? msg
                           form
                           'cljs.spec.alpha/valid?
                           'cljs.spec.alpha/explain-str)))))
