(ns passengers.core
  (:require [notespace.api :as nsp]
            [notespace.kinds :as k]
            [tablecloth.api :as tc]
            [tech.viz.vega :as viz]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.datetime :as dtype-dt]))

(def path "../data/AirPassengers.csv")

(def passengers
  (-> path
      (tc/dataset {:parser-fn {"Month"
                               [:string (fn [d] (str d "-01 00:00:00"))]}
                   :key-fn keyword})
      (tc/rows :as-maps)
      (tc/dataset {:parser-fn {:Month
                               [:local-date-time "yyyy-MM-dd hh:mm:ss"]}})))


passengers

(-> passengers
     tc/columns)

(-> passengers
    :Month
    first
    )

^k/vega
(-> passengers
    (tc/rows :as-maps)
    (viz/time-series :Month :#Passengers))

(->> passengers
    :Month
    (dtype-dt/long-temporal-field :milliseconds))
