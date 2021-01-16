(ns passengers.core
  (:require [notespace.api :as notespace]))

["Note: We purposely overridden the `clojure.core` arithmetic operations with the `dtype-next` ones, to support nice array-arithmetic."]

(require '[notespace.kinds :as kind]
         '[tech.v3.dataset :as dataset]
         '[tablecloth.api :as tablecloth]
         '[tech.viz.vega :as viz]
         '[tech.v3.datatype :as dtype]
         '[tech.v3.datatype.functional :as dtype-fun
           :refer [+ - * /]]
         '[tech.v3.datatype.rolling :as dtype-roll]
         '[tech.v3.datatype.datetime :as dtype-dt]
         '[aerial.hanami.common :as hanami-common]
         '[aerial.hanami.templates :as hanami-templates])

(+ 4 5)

(+ [2 4] [3 5])

(def path "../data/AirPassengers.csv")

(def passengers
  (-> path
      (tablecloth/dataset
       {:key-fn keyword
        :parser-fn {"Month" [:packed-local-date
                             (fn [date-str]
                               (java.time.LocalDate/parse
                                (str date-str "-01")))]}})))

(-> passengers
    tablecloth/columns)

(->> (tablecloth/rows passengers)
     (take 3))

(->> (tablecloth/rows passengers :as-maps)
     (take 3))

(defn prep-data-for-plotting [data]
  (-> data
      (dataset/column-cast :Month :string)
      (tablecloth/rows :as-maps)))

(->> passengers
     prep-data-for-plotting
     (take 3))

^kind/vega
(-> (hanami-common/xform
     hanami-templates/line-chart
     :DATA (prep-data-for-plotting passengers)
     :X :Month
     :XTYPE :temporal
     :Y :#Passengers))

(def passengers-with-log
  (-> passengers
      (tablecloth/add-or-replace-column :log-passengers #(dtype-fun/log (:#Passengers %)))))

^kind/dataset
passengers-with-log

^kind/vega
(hanami-common/xform
 hanami-templates/line-chart
 :DATA (prep-data-for-plotting passengers-with-log)
 :X :Month
 :XTYPE :temporal
 :Y :log-passengers
 :YSCALE {:zero false})

(def passengers-with-rolling
  (-> passengers-with-log
      (tablecloth/add-or-replace-columns
       ;; TODO: How can we not duplicate code here?
       {:rolling-mean
        #(-> %
             (:log-passengers)
             (dtype-roll/fixed-rolling-window
              12
              dtype-fun/mean
              {:relative-window-position :left}))
        :rolling-standard-deviation
        #(-> %
             (:log-passengers)
             (dtype-roll/fixed-rolling-window
              12
              dtype-fun/standard-deviation
              {:relative-window-position :left}))})))

;; This seems not to use the window on the left. 
(dtype-roll/fixed-rolling-window (range 20) 10 dtype-fun/sum {:relative-window-position :left})

^kind/dataset
passengers-with-rolling

^kind/vega
(hanami-common/xform
 hanami-templates/line-chart
 :DATA (prep-data-for-plotting passengers-with-rolling)
 :X :Month
 :XTYPE :temporal
 :Y :rolling-mean
 :YSCALE {:zero false})

^kind/hiccup
[:div
 [:p/vega
  (-> (hanami-common/xform
       hanami-templates/line-chart
       :DATA (prep-data-for-plotting passengers)
       :X :Month
       :XTYPE :temporal
       :Y :#Passengers))]
 [:p/vega
  (hanami-common/xform
   hanami-templates/line-chart
   :DATA (prep-data-for-plotting passengers-with-log)
   :X :Month
   :XTYPE :temporal
   :Y :log-passengers
   :YSCALE {:zero false})]
 [:p/vega
  ^kind/vega
  (hanami-common/xform
   hanami-templates/line-chart
   :DATA (prep-data-for-plotting passengers-with-rolling)
   :X :Month
   :XTYPE :temporal
   :Y :rolling-mean
   :YSCALE {:zero false})]]


^kind/vega
(hanami-common/xform
 hanami-templates/layer-chart
 :DATA (prep-data-for-plotting passengers-with-rolling)
 :LAYER [(hanami-common/xform
          hanami-templates/line-chart
          :X :Month
          :XTYPE :temporal
          :Y :log-passengers
          :YSCALE {:zero false})
         (hanami-common/xform
          hanami-templates/line-chart
          :X :Month
          :XTYPE :temporal
          :Y :rolling-mean
          :YSCALE {:zero false}
          :MCOLOR "red")])

(def passengers-with-difference
  (-> passengers-with-rolling
      (tablecloth/add-or-replace-column
       :difference #(- (:log-passengers %)
                       (:rolling-mean %)))))


^kind/vega
(hanami-common/xform
 hanami-templates/layer-chart
 :DATA (prep-data-for-plotting passengers-with-difference)
 :LAYER [(hanami-common/xform
          hanami-templates/line-chart
          :X :Month
          :XTYPE :temporal
          :Y :difference
          :YSCALE {:zero false})])
