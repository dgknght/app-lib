(ns dgknght.app-lib.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            dgknght.app-lib.decimal-test
            dgknght.app-lib.html-test
            dgknght.app-lib.time-test
            dgknght.app-lib.notifications-test))

(doo-tests 'dgknght.app-lib.decimal-test
           'dgknght.app-lib.html-test
           'dgknght.app-lib.time-test
           'dgknght.app-lib.notifications-test)
