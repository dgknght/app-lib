(ns dgknght.app-lib.math
  (:refer-clojure :exclude [eval])
  (:require #?(:clj [clojure.core :as cc])
            #?(:clj [clojure.pprint :refer [pprint]]
               :cljs [cljs.pprint :refer [pprint]])
            #?(:cljs [dgknght.app-lib.decimal :as decimal])
            [dgknght.app-lib.core :as lib :refer [conj-to-last]]))

(defn- nest-parens
  [elems]
  (first
    (reduce (fn [lists elem]
              (case elem
                "(" (conj lists [])
                ")" (conj-to-last (pop lists)
                                  (peek lists))
                (conj-to-last lists elem)))
            '([])
            elems)))

#?(:cljs (def ^:private operations
           {"+" decimal/+
            "-" decimal/-
            "*" decimal/*
            "/" decimal//}))

(declare eval*)

(defn- eval-statement
  "Evaluates a traditional, simple math operation.

  E.g.:
  (eval-statement [1 \"+\" 1]) => 2"
  [[o1 oper o2]]
  (let [args (map eval* [o1 o2])]
    #?(:clj (cc/eval (apply
                       list (symbol "clojure.core" oper)
                       args))
       :cljs (apply (operations oper)
                    args))))

(defn- eval-one
  "Takes a sequence describing a mathematical expression and
  evaluates one subexpression, returning the original expression
  with the one evaluated subexpression resolved"
  [elems opers]
  (->> elems
       (partition-all 3 2)
       (reduce
         (fn [result [o1 oper :as stm]]
           (if (:processed? result)
             (update-in result [:result] concat (rest stm))
             (if (= 3 (count stm))
               (if (opers oper)
                 (-> result
                     (assoc :processed? true) ; the one sub expression has been evaluated, don't evalute more this pass
                     (update-in [:result] conj (eval-statement stm)))
                 (update-in result [:result] conj o1 oper))
               (update-in result [:result] concat stm))))
         {:result []})
       :result))

(defn- until-same
  "Performs the function f on the initial value init,
  plus any additional arguments, comparing the result
  to the initial value. If they match, the result is
  returned. If not, the function is applied to the result
  until the result matches the input."
  [f init & args]
  (let [last-result (atom init)]
    (loop [result (apply f init args)]
      (if (= @last-result result)
        result
        (do
          (reset! last-result result)
          (recur (apply f result args)))))))

(defn- perform-opers
  [elems opers]
  (until-same eval-one elems opers))

(defn- mdas
  [elems]
  (loop [elems elems
         oper-sets [#{"*" "/"}
                    #{"+" "-"}]]
    (if (= 1 (count elems))
      (eval* (first elems))
      (when (odd? (count elems))
        (recur (perform-opers elems (first oper-sets))
               (rest oper-sets))))))

; make mulitple passes for each operator to enforce pemdas
; perform one calculation per operater set pass
; when an operator set pass returns the same result 2 times, move to the next operator set
; e.g.
; apply #{"*" "/"} to 1 + 2 + 3 => 1 + 2 + 3 (will return the unchanged input)
; apply #{"+" "-"} to 1 + 2 + 3 => 3 + 3
; apply #{"+" "-"} to 3 + 3     => 6
;
; apply #{"*" "/"} to 1 + 2 * 3 => 1 + 6    (when processing the result of the 1st pass will return the result of the 1st pass)
; apply #{"+" "-"} to 1 + 6     => 7


(defmulti eval* type)

(defmethod eval* :default
  [elem]
  elem)

(defmethod eval* ::lib/vector
  [elems]
  (mdas elems))

(defn- parse-decimal
  [v]
  #?(:clj (bigdec v)
     :cljs (decimal/->decimal v)))

(defmethod eval* ::lib/string
  [s]
  (when (re-find #"-?0?\.\d+|-?\d+(?:\.\d+)?" s)
    (parse-decimal s)))

(defn eval
  [input]
  (->> (re-seq #"-?0?\.\d+|-?\d+(?:\.\d+)?|[)(*+/-]" input)
       nest-parens
       eval*))
