(ns dgknght.app-lib.busy-test
  (:require [cljs.test :refer-macros [deftest are]]
            [dgknght.app-lib.busy :as b]))

(deftest indicate-a-started-process
  (are [input expected] (= expected (b/+busy input))
       {}                   {:bg-proc-count 1}
       {:bg-proc-count nil} {:bg-proc-count 1}
       {:bg-proc-count 1}   {:bg-proc-count 2}))

(deftest indicate-a-started-process-with-a-custom-key
  (are [input expected] (= expected (b/+busy input :my-key))
       {}                   {:my-key 1}
       {:my-key nil} {:my-key 1}
       {:my-key 1}   {:my-key 2}))

(deftest indicate-a-completed-process
  (are [input expected] (= expected (b/-busy input))
       {}                   {:bg-proc-count 0}
       {:bg-proc-count nil} {:bg-proc-count 0}
       {:bg-proc-count 1}   {:bg-proc-count 0}
       {:bg-proc-count 2}   {:bg-proc-count 1}))

(deftest indicate-a-completed-process-with-a-custom-key
  (are [input expected] (= expected (b/-busy input :my-key))
       {}                   {:my-key 0}
       {:my-key nil} {:my-key 0}
       {:my-key 1}   {:my-key 0}
       {:my-key 2}   {:my-key 1}))
