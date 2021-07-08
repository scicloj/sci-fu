(ns time-series-intro.tutorial-1
  (:require [notespace.api :as notespace]
            [notespace.kinds :as kind]))

(import '[java.time.format DateTimeFormatter])

(require '[tablecloth.api :as tbl]
         '[clojure.string :refer [lower-case]])

(def data (tbl/dataset "data/aapl.csv"
                       {:key-fn keyword
                        :parser-fn {:Date [:local-date
                                           (DateTimeFormatter/ofPattern "d-MMM-yy")]}}))


^kind/dataset
(tbl/head data)

^kind/dataset
(tbl/info data)
