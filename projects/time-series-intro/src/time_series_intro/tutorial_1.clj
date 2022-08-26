(ns time-series-intro.tutorial-1
  (:require [notespace.api :as notespace]
            [notespace.kinds :as kind]))

(import '[java.time.format DateTimeFormatter])

(require '[tablecloth.api :as tbl]
         '[tablecloth.time.api :as time]
         '[clojure.string :refer [lower-case]])


(def data (tbl/dataset "data/aapl.csv"
                       {:key-fn keyword
                        :parser-fn {:Date [:local-date
                                           (DateTimeFormatter/ofPattern "d-MMM-yy")]}}))



^kind/dataset
(tbl/head data)

^kind/dataset
(tbl/info data)

^kind/md-nocode
["One way to get all entries for a given year is to use slice."]

;; Could slice be made more flexible now so the user could also do justify
;; (time/slice "2017")?
^kind/dataset
(-> data
    (time/slice "2017-01-01" "2017-12-31"))

;; Again here the story around years is confusing. Probably just need a
;; ->year, and this function will return a number?
^kind/dataset
(-> data
    (tbl/select-rows (comp #(= % 2017) #(.getYear %) :Date)))
