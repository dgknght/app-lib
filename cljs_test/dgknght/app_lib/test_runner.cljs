(ns dgknght.app-lib.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            dgknght.app-lib.core-test
            dgknght.app-lib.decimal-test
            dgknght.app-lib.dates-test
            dgknght.app-lib.math-test
            dgknght.app-lib.web-test
            dgknght.app-lib.html-test
            dgknght.app-lib.time-test
            dgknght.app-lib.notifications-test
            dgknght.app-lib.busy-test))

(doo-tests 'dgknght.app-lib.core-test
           'dgknght.app-lib.decimal-test
           'dgknght.app-lib.dates-test
           'dgknght.app-lib.math-test
           'dgknght.app-lib.web-test
           'dgknght.app-lib.html-test
           'dgknght.app-lib.time-test
           'dgknght.app-lib.notifications-test
           'dgknght.app-lib.busy-test)
