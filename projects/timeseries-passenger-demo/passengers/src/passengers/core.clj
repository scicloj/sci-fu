(ns passengers.core
  (:require [notespace.api :as notespace]
            [notespace.kinds :as kind]
            [tech.v3.dataset :as dataset]
            [tablecloth.api :as tablecloth]
            [tech.viz.vega :as viz]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as dtype-fun]
            [tech.v3.datatype.datetime :as dtype-dt]
            [aerial.hanami.common :as hanami-common]
            [aerial.hanami.templates :as hanami-templates]))

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

