(ns dgknght.app-lib.validation-fixtures)

(def complex-explain-data
  #:clojure.spec.alpha{:problems [{:path [:transaction/items :unilateral],
                                   :pred '(clojure.core/fn [%] (clojure.core/contains? % :transaction-item/account)),
                                   :val #:transaction-item{:debit-account {:id 17592186045425},
                                                           :credit-account {:id 17592186045427},
                                                           :credit-memo "conf # 123",
                                                           :value 1000M,
                                                           :quantity -1000M},
                                   :via [:clj-money.entities/transaction
                                         :clj-money.transactions/bilateral-transaction
                                         :clj-money.transactions/complex-transaction
                                         :clj-money.transactions/common-transaction
                                         :transaction/items
                                         :clj-money.entities/transaction-item
                                         :clj-money.transactions/unilateral-item],
                                   :in [:transaction/items 0]}
                                  {:path [:transaction/items :unilateral],
                                   :pred '(clojure.core/fn [%] (clojure.core/contains? % :transaction-item/action)),
                                   :val #:transaction-item{:debit-account {:id 17592186045425},
                                                           :credit-account {:id 17592186045427},
                                                           :credit-memo "conf # 123",
                                                           :value 1000M,
                                                           :quantity -1000M},
                                   :via [:clj-money.entities/transaction
                                         :clj-money.transactions/bilateral-transaction
                                         :clj-money.transactions/complex-transaction
                                         :clj-money.transactions/common-transaction
                                         :transaction/items
                                         :clj-money.entities/transaction-item
                                         :clj-money.transactions/unilateral-item],
                                   :in [:transaction/items 0]}
                                  {:path [:transaction/items :unilateral :transaction-item/quantity],
                                   :pred 'clojure.core/pos?,
                                   :val -1000M,
                                   :via [:clj-money.entities/transaction
                                         :clj-money.transactions/bilateral-transaction
                                         :clj-money.transactions/complex-transaction
                                         :clj-money.transactions/common-transaction
                                         :transaction/items
                                         :clj-money.entities/transaction-item
                                         :clj-money.transactions/unilateral-item
                                         :transaction-item/quantity],
                                   :in [:transaction/items 0 :transaction-item/quantity]}
                                  {:path [:transaction/items :bilateral :transaction-item/quantity],
                                   :pred 'clojure.core/pos?,
                                   :val -1000M,
                                   :via [:clj-money.entities/transaction
                                         :clj-money.transactions/bilateral-transaction
                                         :clj-money.transactions/complex-transaction
                                         :clj-money.transactions/common-transaction
                                         :transaction/items
                                         :clj-money.entities/transaction-item
                                         :clj-money.transactions/bilateral-item
                                         :transaction-item/quantity],
                                   :in [:transaction/items 0 :transaction-item/quantity]}
                                  {:path [:transaction/items :unilateral],
                                   :pred '(clojure.core/fn [%] (clojure.core/contains? % :transaction-item/account)),
                                   :val #:transaction-item{:debit-account {:id 17592186045425},
                                                           :credit-account {:id 17592186045427},
                                                           :credit-memo "conf # 123",
                                                           :value 1000M,
                                                           :quantity -1000M},
                                   :via [:clj-money.entities/transaction
                                         :clj-money.transactions/bilateral-transaction
                                         :clj-money.transactions/complex-transaction
                                         :transaction/items
                                         :clj-money.entities/transaction-item
                                         :clj-money.transactions/unilateral-item],
                                   :in [:transaction/items 0]}
                                  {:path [:transaction/items :unilateral],
                                   :pred '(clojure.core/fn [%] (clojure.core/contains? % :transaction-item/action)),
                                   :val #:transaction-item{:debit-account {:id 17592186045425},
                                                           :credit-account {:id 17592186045427},
                                                           :credit-memo "conf # 123",
                                                           :value 1000M,
                                                           :quantity -1000M},
                                   :via [:clj-money.entities/transaction
                                         :clj-money.transactions/bilateral-transaction
                                         :clj-money.transactions/complex-transaction
                                         :transaction/items
                                         :clj-money.entities/transaction-item
                                         :clj-money.transactions/unilateral-item],
                                   :in [:transaction/items 0]}
                                  {:path [:transaction/items :unilateral :transaction-item/quantity],
                                   :pred 'clojure.core/pos?,
                                   :val -1000M,
                                   :via [:clj-money.entities/transaction
                                         :clj-money.transactions/bilateral-transaction
                                         :clj-money.transactions/complex-transaction
                                         :transaction/items
                                         :clj-money.entities/transaction-item
                                         :clj-money.transactions/unilateral-item
                                         :transaction-item/quantity],
                                   :in [:transaction/items 0 :transaction-item/quantity]}
                                  {:path [:transaction/items :bilateral :transaction-item/quantity],
                                   :pred 'clojure.core/pos?,
                                   :val -1000M,
                                   :via [:clj-money.entities/transaction
                                         :clj-money.transactions/bilateral-transaction
                                         :clj-money.transactions/complex-transaction
                                         :transaction/items
                                         :clj-money.entities/transaction-item
                                         :clj-money.transactions/bilateral-item
                                         :transaction-item/quantity],
                                   :in [:transaction/items 0 :transaction-item/quantity]}
                                  {:path [:transaction/items :unilateral],
                                   :pred '(clojure.core/fn [%] (clojure.core/contains? % :transaction-item/account)),
                                   :val #:transaction-item{:debit-account {:id 17592186045425},
                                                           :credit-account {:id 17592186045427},
                                                           :credit-memo "conf # 123",
                                                           :value 1000M,
                                                           :quantity -1000M},
                                   :via [:clj-money.entities/transaction
                                         :transaction/items
                                         :clj-money.entities/transaction-item
                                         :clj-money.transactions/unilateral-item],
                                   :in [:transaction/items 0]}
                                  {:path [:transaction/items :unilateral],
                                   :pred '(clojure.core/fn [%] (clojure.core/contains? % :transaction-item/action)),
                                   :val #:transaction-item{:debit-account {:id 17592186045425},
                                                           :credit-account {:id 17592186045427},
                                                           :credit-memo "conf # 123",
                                                           :value 1000M,
                                                           :quantity -1000M},
                                   :via [:clj-money.entities/transaction
                                         :transaction/items
                                         :clj-money.entities/transaction-item
                                         :clj-money.transactions/unilateral-item],
                                   :in [:transaction/items 0]}
                                  {:path [:transaction/items :unilateral :transaction-item/quantity],
                                   :pred 'clojure.core/pos?,
                                   :val -1000M,
                                   :via [:clj-money.entities/transaction
                                         :transaction/items
                                         :clj-money.entities/transaction-item
                                         :clj-money.transactions/unilateral-item
                                         :transaction-item/quantity],
                                   :in [:transaction/items 0 :transaction-item/quantity]}
                                  {:path [:transaction/items :bilateral :transaction-item/quantity],
                                   :pred 'clojure.core/pos?,
                                   :val -1000M,
                                   :via [:clj-money.entities/transaction
                                         :transaction/items
                                         :clj-money.entities/transaction-item
                                         :clj-money.transactions/bilateral-item
                                         :transaction-item/quantity],
                                   :in [:transaction/items 0 :transaction-item/quantity]}],
                       :spec :clj-money.entities/transaction,
                       :value #:transaction{:transaction-date "2016-03-02",
                                            :description "Paycheck",
                                            :memo "final, partial",
                                            :entity {:id 17592186045420},
                                            :items [#:transaction-item{:debit-account {:id 17592186045425},
                                                                       :credit-account {:id 17592186045427},
                                                                       :credit-memo "conf # 123",
                                                                       :value 1000M,
                                                                       :quantity -1000M}]}})
