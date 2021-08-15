(ns dgknght.app-lib.busy
  (:require [reagent.core :as r]
            [reagent.ratom :refer [make-reaction]]
            [dgknght.app-lib.core :refer [fmin]]))

(defn busy
  "Given a state atom, create a derefable value that indicates whether or not
  the application is running any background processes."
  [state]
  (let [bg-proc-count (r/cursor state [:bg-proc-count])]
    (make-reaction #(when @bg-proc-count
                      (not (zero? @bg-proc-count))))))

(defn derefable-apply
  [target f]
  (if (satisfies? IDeref target)
    (swap! target f)
    (f target)))

(defn +busy
  "Given a state map, increment the :bg-proc-count value, indicating
  another background process has been started.

  The state map can be wrapped in an atom or other derefable type, or
  it can be a simple clojure map."
  ([state] (+busy state :bg-proc-count))
  ([state k]
   (derefable-apply state #(update-in % [k] (fnil inc 0)))))

(defn -busy
  "Given a state map, decrement the :bg-proc-count value, indicating
  another background process has completed..

  The state map can be wrapped in an atom or other derefable type, or
  it can be a simple clojure map. "
  ([state] (-busy state :bg-proc-count))
  ([state k]
   (derefable-apply state #(update-in % [k] (fmin (fnil dec 1) 0)))))
