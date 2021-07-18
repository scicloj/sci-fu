(ns time-series-intro.tutorial-2
  (:require [notespace.api :as notespace]
            [notespace.kinds :as kind]))

^kind/md-nocode
["# Time Series: Rolling Statistics"]

(import '[java.time.format DateTimeFormatter]
        '[org.apache.commons.math3.fitting PolynomialCurveFitter]
        '[org.apache.commons.math3.fitting WeightedObservedPoints])

(require '[tablecloth.api :as tbl]
         '[tablecloth.time.api :as time]
         '[tech.v3.datatype :as dtype]
         '[tech.v3.datatype.functional :as dtype-fun]
         '[tech.v3.datatype.base :refer [->reader]]
         '[clojure.string :refer [lower-case]]
         '[fastmath.core :refer [makepoly]]
         '[aerial.hanami.common :as hc]
         '[aerial.hanami.templates :as ht])

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

(meta regressor)

(def data-with-linear-regression
  (-> data
      (tbl/add-column :LinearRegresion (map regressor (:Week data)))))

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
          :Y :LinearRegression)])

(defn polyfit [degree xs ys]
  (let [fitter (PolynomialCurveFitter/create degree)
        obs    (WeightedObservedPoints.)]
    ;;this is irregular, but makes use of map's multiple
    ;;arity.
    (doall (map (fn [x y] (.add obs (double x) (double y))) xs ys))
    (.fit fitter (.toList obs))))

(def regressor-degree2
  (makepoly (polyfit 2 (:Week data) (:Hits data))))


(def data-with-regression-degree2
  (-> data
      (tbl/add-column :2DegreeLinearRegression
                      (map regressor-degree2 (:Week data)))))

^kind/dataset
data-with-regression-degree2

^kind/vega
(hc/xform
 ht/layer-chart
 :DATA (-> data-with-regression-degree2
           (tbl/drop-columns :WeekOf)
           (tbl/rows :as-maps))
 :LAYER [(hc/xform
          ht/line-chart
          :X :Week
          :Y :Hits)
         (hc/xform
          ht/line-chart
          :X :Week
          :Y :2DegreeLinearRegression)])

^kind/md-nocode
["#### Plot with Dates"]

^kind/vega
(hc/xform
 ht/layer-chart
 :DATA (-> data-with-regression-degree2
           (tbl/convert-types :WeekOf :string)
           (tbl/rows :as-maps))
 :LAYER [(hc/xform
          ht/line-chart
          :X :WeekOf
          :XTYPE :temporal
          :Y :Hits)
         (hc/xform
          ht/line-chart
          :X :WeekOf
          :XTYPE :temporal
          :Y :2DegreeLinearRegression)])

^kind/md-nocode
["### Practice"]

(def rossmann-raw (tbl/dataset "data/rossmann.csv"
                               {:key-fn keyword}))

^kind/dataset
rossmann-raw

(tbl/info rossman-raw)

(defn ->year [local-date]
  (time/convert-to local-date :year))

(defn ->month [local-date]
  (.getMonthValue local-date))

(def rossmann-data
  (-> rossmann-raw
      (tbl/add-column :Year #(dtype/emap ->year :int64 (:Date %)))
      (tbl/add-column :Month #(dtype/emap ->month :int64 (:Date %)))))

^kind/dataset
rossmann-data

^kind/dataset
(-> rossmann-data
    (time/slice "2015-05-01" "2015-05-31"))

(tbl/row-count rossmann-data)

(def rossman-data-store-1
  (-> rossmann-data
      (tbl/select-rows (comp #(= % 1) :Store))
      (tbl/select-rows (comp #(= % 1) :Open))))

(tbl/row-count rossman-data-store-1)


^kind/md-nocode
["### Plot the sales data"]

(def boxplot
  (assoc ht/view-base
         :mark (merge ht/mark-base {:type "boxplot"})))

^kind/vega
(hc/xform
 boxplot
 :DATA (-> rossman-data-store-1
           (tbl/select-columns [:Promo :Sales])
           (tbl/rows :as-maps))
 :X :Promo :XTYPE :ordinal
 :Y :Sales :YTPE :quantitative :YSCALE {:zero false})


