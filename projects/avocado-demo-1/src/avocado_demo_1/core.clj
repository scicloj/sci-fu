(ns avocado-demo-1.core
  (:require [tablecloth.api :as t]
            [tech.v3.dataset :as ds]
            [tech.v3.datatype :as dtype]
            [tech.v3.tensor :as dtt]
            [tech.v3.datatype.statistics :as stats]
            [tech.v3.datatype.rolling :as rolling]
            [clojure.java.io :as io]))

(def data
  (t/dataset "resources/data/avocado.csv.gz"
             {:key-fn keyword}))

(-> data
    (t/select-rows #(and (= (:type %) "organic")
                         (= (:region %) "Albany")))
    (t/order-by :Date)
    (t/add-or-replace-column :smoothed-price
                             #(rolling/fixed-rolling-window (:AveragePrice %)
                                                            10
                                                            stats/mean))
    (t/add-or-replace-column :week-of-year
                             #(map (partial tech.v3.datatype.datetime/long-temporal-field :week-of-year) (:Date %)))
    (t/select-columns [:week-of-year :Date :AveragePrice :smoothed-price]))

