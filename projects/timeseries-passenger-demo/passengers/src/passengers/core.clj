(ns passengers.core
  (:require [notespace.api :as notespace]
            [notespace.kinds :as kind]
            [tablecloth.api :as tablecloth]
            [tech.viz.vega :as viz]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.datetime :as dtype-dt]))

(def path "../data/AirPassengers.csv")

(def passegers
  (-> path
      (tablecloth/dataset
       {:parser-fn {"Month" [:packed-local-date
                             (fn [date-str]
                               (java.time.LocalDate/parse
                                (str date-str "-01")))]}})))

(-> passengers
    tablecloth/columns)
