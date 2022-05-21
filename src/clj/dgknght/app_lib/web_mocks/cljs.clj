(ns dgknght.app-lib.web-mocks.cljs
  (:require [cljs.test :as test]
            [dgknght.app-lib.web-mocks.impl :as impl]))

(defmethod test/assert-expr 'dgknght.app-lib.web-mocks/called?
  [_env msg form]
  `(test/do-report
     ~(impl/called? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.web-mocks/not-called?
  [_env msg form]
  `(test/do-report
     ~(impl/not-called? msg form)))

(defmethod test/assert-expr 'dgknght.app-lib.web-mocks/called-with-headers?
  [_env msg form]
  `(test/do-report
     ~(impl/called-with-headers? msg form)))
