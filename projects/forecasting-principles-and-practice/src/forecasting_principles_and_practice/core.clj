(ns forecasting-principles-and-practice.core
  (:require
   
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :refer [replace join]]

   [tech.v3.datatype :as dtype]
   [tech.v3.datatype.casting :as casting]
   [tech.v3.datatype.functional :as fun]
   [tech.v3.dataset :as tds]
   
   ;; '[scicloj.ml.metamorph :as mm]
   
   [scicloj.ml.core :as ml]
   [scicloj.ml.dataset :as ds]
   
   [tablecloth.api :as tbl]
   [tablecloth.time.index :as idx]
   [tablecloth.time.api :refer [slice]]

   [aerial.hanami.common :as hc]
   [aerial.hanami.templates :as ht]
            ;;[aerial.hanami.core :as hmi]
            
   [notespace.api :as notespace]
   [notespace.kinds :as kind]
))
            

(import java.time.LocalDate)
(import java.util.Locale)
(import java.time.format.TextStyle)
(import [org.threeten.extra YearQuarter])

(require
 ;;'[scicloj.metamorph.core :as morph]
 ;; '[tech.v3.datatype.datetime :as datetime]
 ;; '[scicloj.ml.metamorph :as mm]
 ;; '[tablecloth.pipeline :as tbl-pipe]
 ;; '[tech.v3.libs.smile.metamorph :as smile]
 ;; '[tech.v3.dataset.metamorph :as ds-mm] ;; tech.dataset support for metamorph
 ;; '[tech.v3.dataset.modelling :as ds-mod]
 ;; '[tech.v3.ml.metamorph :as ml-mm] ;; tech.ml support for metamorph

 '[time-literals.data-readers]
 '[time-literals.read-write]

 ;; '[notespace.state :as state]
 ;; '[notespace.paths :as paths]
)



^kind/hidden
(comment
  (notespace/init-with-browser)
  (notespace/eval-this-notespace)
  (notespace/eval-and-realize-this-notespace)
  )


(casting/add-object-datatype! :year-quarter YearQuarter true)

;; data
(def data (-> (ds/dataset "./data/aus-production.csv"
                          {:key-fn    keyword
                           :parser-fn {"Quarter" [:year-quarter
                                                  (fn [date-str]
                                                    (-> date-str
                                                        (replace #" " "-")
                                                        (YearQuarter/parse)))]}})
              (ds/add-or-replace-column :QuarterEnd
                                        #(dtype/emap
                                          (fn [x] (.atEndOfQuarter x)) :local-date (:Quarter %)))))

(def indexed-data (idx/index-by data :Quarter))

(def train-data (slice indexed-data
                       (YearQuarter/parse "1992-Q1")
                       (YearQuarter/parse "2006-Q4")))

(def test-data (slice indexed-data
                      (YearQuarter/parse "2007-Q1")
                      (YearQuarter/parse "2009-Q4")))


;; We look at 4 "models" - mean, naive, seasonal-naive and drift
;; First, we compute the models, and then use it's parameters in prediction

;; calc(s) are the "model" implementation that compute the parameters
(defn calc-mean
  [col]
  {:mean (fun/mean col)})

(defn calc-naive
  [col]
  {:naive (last col)})

(defn calc-snaive
  [col seasonal-period]
  {:snaive (take-last seasonal-period col)})

(defn calc-drift
  [col]
  (let [yT (last col)
        y1 (first col)
        slope (/ (- yT y1) (count col))]
    {:yT yT :slope slope}))


;; predictions: use "model" parameters
(defn predict-mean
  [col model]
  (repeat (tbl/row-count col) (:mean model)))

(defn predict-naive
  [col model]
  (repeat (tbl/row-count col) (:naive model)))

(defn- snaive-index
  [T m]
  (fn [h]
    (let [k (quot (- h 1) m)
          ix (- (+ T h) (* m (+ k 1)))]
      (dec (+ ix m)))))

(defn predict-snaive
  [col model]
  (let [T 0
        m 4
        idx (map (snaive-index T m) (map inc (range (count col))))
        val (:snaive model)]
    (map #(nth val %) idx)))

(defn predict-drift
  [col model]
  (let [yT (:yT model)
        slope (float (:slope model))
        n (count col)]
     (map #(+ yT (/ (* slope (inc %)) n)) (range n))))


;; calc & predict. verify this works before doing the pipeline
(predict-mean (test-data :Beer) (calc-mean (train-data :Beer)))

(predict-naive (test-data :Beer) (calc-naive (train-data :Beer)))

(predict-snaive (test-data :Beer) (calc-snaive (train-data :Beer) 4))

(predict-drift (test-data :Beer) (calc-drift (train-data :Beer)))


;; now for the metamorph pipeline
(defn ts-forecast-model []
  (fn [{:metamorph/keys [id data mode] :as ctx}]
    (case mode
      :fit (assoc ctx id {:model1 (calc-mean (data :Beer))
                          :model2 (calc-naive (data :Beer))
                          :model3 (calc-snaive (data :Beer) 4)
                          :model4 (calc-drift (data :Beer))})

      :transform (assoc ctx :metamorph/data {:model1 (predict-mean (data :Beer) (-> ctx :model :model1))
                                             :model2 (predict-naive (data :Beer) (-> ctx :model :model2))
                                             :model3 (predict-snaive (data :Beer) (-> ctx :model :model3))
                                             :model4 (predict-drift (data :Beer) (-> ctx :model :model4))}))))

(def pipeline
  (ml/pipeline
   {:metamorph/id :model}
   (ts-forecast-model)))

(def training-run
  (pipeline
   {:metamorph/mode :fit
    :metamorph/data train-data}))

(def prediction-run
  (pipeline
   (merge training-run {:metamorph/mode :transform
                        :metamorph/data test-data})))

(:metamorph/data prediction-run) ;; results



;; if it is only one model at a time, we do not have to tag models
;; 1. assoc train model into ctx in id
;; 2. assoc test results into ctx in data

(defn one-model []
  (fn [{:metamorph/keys [id data mode] :as ctx}]
    (case mode
      :fit (assoc ctx id (calc-mean (data :Beer)))
      :transform (assoc ctx :metamorph/data (predict-mean (data :Beer) (-> ctx (get id)))))))

(def pipeline-one-model
  (ml/pipeline
   (one-model)))

(def training-one-model
  (pipeline-one-model
   {:metamorph/mode :fit
    :metamorph/data train-data}))

(def prediction-one-model
  (pipeline-one-model
   (merge training-one-model {:metamorph/mode :transform
                              :metamorph/data test-data})))

(:metamorph/data prediction-one-model)

;; test if plotting works
(.exists (io/file "/home/sid/projects/hanami/samples/data/cars.json"))
(def cars-filename "/home/sid/projects/hanami/samples/data/cars.json")

(def cars (tds/->dataset (json/read-str (slurp cars-filename )
                                        :key-fn keyword)))

(defn point-chart
  [ds template & subs]
  (apply hc/xform template :DATA (tbl/rows ds :as-maps) subs))


^kind/vega
(-> cars
    (point-chart ht/point-chart :X "Horsepower" :Y "Miles_per_Gallon" :COLOR "Origin"))

;; end test

(defn prep-data-for-plotting [data]
  (-> data
      (tbl/select-columns [:QuarterEnd :Beer :Cement])
      (tds/column-cast :QuarterEnd :string)
      (ds/rows :as-maps)))

(def plotdata (take 3 (-> data prep-data-for-plotting)))

(first plotdata)

^kind/vega
(hc/xform
 ht/line-chart
 :DATA plotdata
 :X :QuarterEnd
 :XTYPE :temporal
 :Y :Beer)

(defn plot
  [ds template & subs]
  (kind/override (apply hc/xform template :DATA ds subs) kind/vega))

(-> data
    prep-data-for-plotting
    (plot ht/point-chart :X :QuarterEnd :XTYPE :temporal :Y :Beer))


^kind/vega
(hc/xform
 ht/layer-chart
 :DATA plotdata
 :LAYER [(hc/xform
          ht/line-chart
          :X :QuarterEnd
          :XTYPE :temporal
          :Y :Beer)
         (hc/xform
          ht/point-chart
          :X :QuarterEnd
          :XTYPE :temporal
          :Y :Cement)])



(comment
  defn plotX
  [ds template & subs]
  (kind/override (apply hc/xform template :DATA ds subs) kind/vega)

  (-> data
      prep-data-for-plotting
      (plot ht/line-chart :X :QuarterEnd :XTYPE :temporal :Y :Beer)
      (plot ht/point-chart :X :QuarterEnd :XTYPE :temporal :Y :Cement))


  (defn plotXYZ
    [ds]
    (fn [template & subs]
      (apply hc/xform template :DATA ds subs)))

  (def closure_pltXYZ (plotXYZ plotdata))

  (closure_pltXYZ ht/line-chart :X :QuarterEnd :XTYPE :temporal :Y :Beer)

  (closure_pltXYZ ht/line-chart :X :QuarterEnd :XTYPE :temporal :Y :Cement))



(def gafadata (-> (ds/dataset "./data/gafa-stock.csv" {:key-fn keyword})))


(defn ->year
  [ldate]
  (.getYear ldate))

(defn ->year-month
  [ldate]
  (let [yyyy (.getYear ldate)
        mmm (-> (.getMonth ldate)
                (.getDisplayName TextStyle/SHORT (Locale/getDefault)))]
    [yyyy mmm]))


(def ld (LocalDate/of 2016 1 4))
(->year ld)
(join " " (->year-month ld))


(require '[tablecloth.time.converters])


(def goog-train-data
  (-> gafadata
      (tbl/select-rows (comp #(= % 2015) ->year :Date))
      (tbl/select-rows (comp #(= % "GOOG") :Symbol))))


(def goog-test-data
  (-> gafadata
      (tbl/select-rows (comp #(= % "2016 Jan") #(join " " (->year-month %)) :Date))
      (tbl/select-rows (comp #(= % "GOOG") :Symbol))))

^kind/dataset
goog-train-data

goog-test-data

;; how does it work on transducers















