(ns dgknght.app-lib.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            dgknght.app-lib.test-assertions-test
            dgknght.app-lib.web-mocks-test
            dgknght.app-lib.api-async-test
            dgknght.app-lib.api-3-test
            dgknght.app-lib.forms-validation-test
            dgknght.app-lib.test-test
            dgknght.app-lib.core-test
            dgknght.app-lib.decimal-test
            dgknght.app-lib.dates-test
            dgknght.app-lib.math-test
            dgknght.app-lib.web-test
            dgknght.app-lib.html-test
            dgknght.app-lib.time-test
            dgknght.app-lib.notifications-test
            dgknght.app-lib.busy-test))

(doo-tests 'dgknght.app-lib.test-assertions-test
           'dgknght.app-lib.web-mocks-test
           'dgknght.app-lib.forms-validation-test
           'dgknght.app-lib.core-test
           'dgknght.app-lib.test-test
           'dgknght.app-lib.decimal-test
           'dgknght.app-lib.dates-test
           'dgknght.app-lib.math-test
           'dgknght.app-lib.web-test
           'dgknght.app-lib.html-test
           'dgknght.app-lib.time-test
           'dgknght.app-lib.notifications-test
           'dgknght.app-lib.busy-test
           'dgknght.app-lib.api-3-test
           'dgknght.app-lib.api-async-test)
