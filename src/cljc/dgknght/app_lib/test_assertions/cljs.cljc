(ns dgknght.app-lib.test-assertions.cljs
  (:require [cljs.test :as test]
            [dgknght.app-lib.test-assertions.impl :as impl]))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/valid?
  [_env msg form]
  `(test/do-report
     ~(impl/valid? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/invalid?
  [_env msg form]
  `(test/do-report
     ~(impl/invalid? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/comparable?
  [_env msg form]
  `(test/do-report
     ~(impl/comparable? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/seq-of-maps-like?
  [_env msg form]
  `(test/do-report
     ~(impl/seq-of-maps-like? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/seq-with-map-like?
  [_env msg form]
  `(test/do-report
     ~(impl/seq-with-map-like? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/seq-with-no-map-like?
  [_env msg form]
  `(test/do-report
     ~(impl/seq-with-no-map-like? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/seq-containing-value?
  [_env msg form]
  `(test/do-report
     ~(impl/seq-containing-value? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/seq-excluding-value?
  [_env msg form]
  `(test/do-report
     ~(impl/seq-excluding-value? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/seq-containing-model?
  [_env msg form]
  `(test/do-report
     ~(impl/seq-containing-model? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/seq-excluding-model?
  [_env msg form]
  `(test/do-report
     ~(impl/seq-excluding-model? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/http-redirect-to?
  [_env msg form]
  `(test/do-report
     ~(impl/http-redirect-to? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/http-success?
  [_env msg form]
  `(test/do-report
     ~(impl/http-success? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/http-created?
  [_env msg form]
  `(test/do-report
     ~(impl/assert-http-status 201 msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/http-no-content?
  [_env msg form]
  `(test/do-report
     ~(impl/assert-http-status 204 msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/http-bad-request?
  [_env msg form]
  `(test/do-report
     ~(impl/assert-http-status 400 msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/http-unauthorized?
  [_env msg form]
  `(test/do-report
     ~(impl/assert-http-status 401 msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/http-forbidden?
  [_env msg form]
  `(test/do-report
     ~(impl/assert-http-status 403 msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/http-not-found?
  [_env msg form]
  `(test/do-report
     ~(impl/assert-http-status 404 msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/http-teapot?
  [_env msg form]
  `(test/do-report
     ~(impl/assert-http-status 418 msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/http-unprocessable?
  [_env msg form]
  `(test/do-report
     ~(impl/assert-http-status 422 msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/url-like?
  [_env msg form]
  `(test/do-report
     ~(impl/url-like? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/not-url-like?
  [_env msg form]
  `(test/do-report
     ~(impl/not-url-like? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/mime-msg-containing?
  [_env msg form]
  `(test/do-report
     ~(impl/mime-msg-containing? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/mime-msg-not-containing?
  [_env msg form]
  `(test/do-report
     ~(impl/mime-msg-not-containing? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/http-response-with-cookie?
  [_env msg form]
  `(test/do-report
     ~(impl/http-response-with-cookie? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/http-response-without-cookie?
  [_env msg form]
  `(test/do-report
     ~(impl/http-response-without-cookie? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/same-date?
  [_env msg form]
  `(test/do-report
     ~(impl/same-date? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.test-assertions/conformant?
  [_env msg form]
  `(test/do-report
     ~(impl/conformant? msg form)))
