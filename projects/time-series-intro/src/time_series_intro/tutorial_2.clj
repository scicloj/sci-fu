(ns time-series-intro.tutorial-2
  (:require [notespace.api :as notespace]
            [notespace.kinds :as kind]))

^kind/md-nocode
["# Time Series: Rolling Statistics"]

(import '[java.time.format DateTimeFormatter])

(require '[tablecloth.api :as tbl]
         '[tablecloth.time.api :as time]
         '[tech.v3.datatype.functional :as dtype-fun]
         '[clojure.string :refer [lower-case]]
         '[aerial.hanami.common :as hc]
         '[aerial.hanami.templates :as ht]
         '[fastmath.interpolation :refer [linear linear-smile]])

(def data (tbl/dataset "data/mapquest_google_trends.csv"
                       {:key-fn keyword
                        :parse-fn {:WeekOf :local-date}}))
^kind/dataset
data

(-> data tbl/info)

(def data
  (-> data
      (tbl/rename-columns {:mapquest :Hits})
      (tbl/add-column :Week (range (tbl/row-count data)))))

^kind/dataset
(tbl/head data)

^kind/vega
(hc/xform
 ht/line-chart
 :DATA (-> data
           (tbl/drop-columns :WeekOf)
           (tbl/rows :as-maps))
 :X :Week
 :Y :Hits)

(def regressor (dtype-fun/linear-regressor (:Week data) (:Hits data)))

(def data-with-linear-regression
  (-> data
      (tbl/add-column :Linear (map regressor (:Week data)))))

^kind/dataset
data-with-linear-regression

^kind/vega
(hc/xform
 ht/layer-chart
 :DATA (-> data-with-linear-regression
           (tbl/drop-columns :WeekOf)
           (tbl/rows :as-maps))
 :LAYER [(hc/xform
          ht/line-chart
          :X :Week
          :Y :Hits)
         (hc/xform
          ht/line-chart
          :X :Week
          :Y :Linear)])
