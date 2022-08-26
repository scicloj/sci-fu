(ns r4ds-timeseries.core
  (:require [notespace.api :as notespace]
            [notespace.kinds :as kind]))


(require '[tablecloth.api :refer [dataset] :as tbl]
         '[tablecloth.time.api :as time]
         '[tech.v3.datatype.functional :as fun]
         '[tech.v3.datatype :refer [emap] :as dtype]
         '[aerial.hanami.common :as hc]
         '[aerial.hanami.templates :as ht]
         '[scicloj.viz.api :as viz]
         '[scicloj.viz.templates :as vt]
         '[scicloj.metamorph.ml :as mmml])
      (->> (tech.v3.datatype/emap #(make-date (:year %)
                                              (:month %)
                                              (:day %))
                                  :local-date))))

(def flights-with-date
  (-> flights-raw
      (tbl/add-column :date #(build-date-column %))))

^kind/dataset
flights-with-date

(def flights-by-day
  (-> flights-with-date
      (tbl/group-by :date)
      (tbl/aggregate tbl/row-count)
      (tbl/rename-columns {:$group-name :date})))

^kind/dataset
flights-by-day

(-> flights-by-day
    (tbl/add-or-replace-column :date #(emap str :string (:date %)))
    (viz/data)
    (viz/type ht/line-chart)
    (viz/x :date {:type :temporal})
    (viz/y :summary)
    (assoc :YSCALE {:zero false})
    (viz/viz)
    )


(def boxplot
  (assoc ht/view-base
         :mark (merge ht/mark-base {:type "boxplot"})))

(def boxdata
  (-> flights-by-day
      (tbl/add-or-replace-column :weekday
                                  (fn [ds]
                                    (emap #(-> (.getDayOfWeek %) str)
                                          :string
                                          (:date ds))))
      (tbl/select-columns [:summary :weekday])
      (tbl/rows :as-maps)))

boxdata

^kind/vega
(hc/xform
 boxplot
 :DATA boxdata
 :X :weekday :XTYPE :ordinal
 :Y :summary :YTPE :quantitative :YSCALE {:zero false})

(scicloj)


;; 
;; (defn ->linear-model [dataset target-column explanatory-columns]
;;   (let [model (-> dataset
;;                   (ds/select-columns (conj explanatory-columns target-column))
;;                   (ds/set-inference-target target-column)
;;                   (mmml/train {:model-type :smile.regression/ordinary-least-square}))]
;;     model))
