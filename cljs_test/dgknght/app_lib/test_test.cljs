(ns dgknght.app-lib.test-test
  (:require [cljs.test :refer [deftest is]]
            [clojure.data]
            [dgknght.app-lib.core]
            [dgknght.app-lib.test]))

(deftest compare-maps
  (is (comparable? {:one 1}
                   {:one 1
                    :two 2})
      "A map compared to an expectation that is a subset of the map succeeds"))
