(ns forecasting-principles-and-practice.fpp3_chapter_5_2
  (:require
   [clojure.string :refer [replace join]]

   [time-literals.data-readers]
   [time-literals.read-write]

   [tech.v3.datatype :as dtype]
   [tech.v3.datatype.casting :as casting]
   [tech.v3.datatype.functional :as dfn]
   
   [tablecloth.api :as tbl]
   [tablecloth.time.api :refer [slice]]
   [tablecloth.time.utils.indexing :refer [index-by]]

   [aerial.hanami.common :as hc]
   [aerial.hanami.templates :as ht]
   ;; [aerial.hanami.core :as hmi]
            
   [notespace.api :as notespace]
   [notespace.kinds :as kind]
))
            
(import [org.threeten.extra YearQuarter])

(comment
  require
 ;; '[scicloj.metamorph.core :as morph]
 ;; '[tech.v3.datatype.datetime :as datetime]
 ;; '[scicloj.ml.metamorph :as mm]
 ;; '[tablecloth.pipeline :as tbl-pipe]
 ;; '[tech.v3.libs.smile.metamorph :as smile]
 ;; '[tech.v3.dataset.metamorph :as ds-mm] ;; tech.dataset support for metamorph
 ;; '[tech.v3.dataset.modelling :as ds-mod]
 ;; '[tech.v3.ml.metamorph :as ml-mm] ;; tech.ml support for metamorph


 ;; '[notespace.state :as state]
 ;; '[notespace.paths :as paths]
  )

;; (require '[tech.viz.vega :as vega])
;; (require '[tech.v3.dataset :as ds])


^kind/hidden
(comment
  (notespace/init-with-browser)
  (notespace/eval-this-notespace)
  (notespace/eval-and-realize-this-notespace))


(casting/add-object-datatype! :year-quarter YearQuarter true)

(defn year-quarter->localdate-end-of-period
  [year-quarter]
  (-> year-quarter
      YearQuarter/parse
      .atEndOfQuarter))

(defn year-quarter->localdate-begin-of-period
  [year-quarter]
  (-> year-quarter
      YearQuarter/parse
      (.atDay 1)))

;; data
(def ausdata 
  (-> "./data/aus-production.csv"
      
      (tbl/dataset {:dataset-name "aus-production"
                    :key-fn keyword
                    :parser-fn {"Quarter" [:year-quarter
                                           (fn [date-str]
                                             (-> date-str
                                                 (replace #" " "-")
                                                 (YearQuarter/parse)))]}})
      
      ;; new column :QuarterEnd which is the last date of the :Quarter in localdate
      (tbl/add-or-replace-column :QuarterEnd
                                 #(dtype/emap (fn [x] (.atEndOfQuarter x)) :local-date (:Quarter %)))


      ;; indexed
      (index-by :QuarterEnd)))


(def train-ausdata (slice ausdata
                          (year-quarter->localdate-begin-of-period "2002-Q1")
                          (year-quarter->localdate-begin-of-period "2006-Q4")))

(def test-ausdata (slice ausdata
                         (year-quarter->localdate-begin-of-period "2007-Q1")
                         (year-quarter->localdate-begin-of-period "2009-Q4")))

(meta ausdata)
(meta train-ausdata)
(meta test-ausdata)


;; We look at 4 simple "models" thar are usedin the book
;;   - mean, naive, seasonal-naive and drift
;; So, we define these models, and then use it in prediction

;; a model in this example is really simple and how it is used in the prediction
;; We do this using two seperate approaches
;;   1. use model and predcition functions to directly compute and predict
;;   2. wrap model and prediction functions in a pipeline function


;; calc(s) are the "model" implementations
;; they compute and return the model specific parameters
;; here, all calc functions return a dictionary

(defn model-mean
  [col]
  {:mean (dfn/mean col)})

(defn model-naive
  [col]
  {:naive (last col)})


(defn model-snaive-fn
  [seasonal-period]
  (fn [col] 
    {:snaive (take-last seasonal-period col)}))


(def model-snaive
  (model-snaive-fn 4))


(defn model-drift
  [col]
  (let [yT (last col)
        y1 (first col)
        slope (/ (- yT y1) (count col))]
    {:yT yT :slope slope}))


;; predictions: using "model" parameters
(defn predict-mean
  [col model]
  (vec (repeat (tbl/row-count col) (:mean model))))

(defn predict-naive
  [col model]
  (vec (repeat (tbl/row-count col) (:naive model))))

;; helper to calculate the seasonal index. see book for the formula
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
    (vec (map #(nth val %) idx))))

(defn predict-drift
  [col model]
  (let [yT (:yT model)
        slope (float (:slope model))
        n (count col)]
    (vec (map #(+ yT (/ (* slope (inc %)) n)) (range n)))))


;; test it now
(predict-mean (test-ausdata :Beer)
              (model-mean (train-ausdata :Beer)))

(predict-naive (test-ausdata :Beer)
               (model-naive (train-ausdata :Beer)))

(predict-snaive (test-ausdata :Beer)
                (model-snaive (train-ausdata :Beer)))

(predict-drift (test-ausdata :Beer)
               (model-drift (train-ausdata :Beer)))


(defn residuals
  [ds y y-hat]
  (dfn/- (ds y) (ds y-hat)))

;; a simple workflow
;; simple because it is tailor made to what we are doing here
;; workflow is
;;  1. model
;;  2. fit
;;  3. merge fit values to test data
;;  4. compute residuals from #3 and merge it to #3

(defn simple-workflow
  [train-ds test-ds model predict col-name]
  (let [train-data (train-ds col-name)
        test-data (test-ds col-name)
        col-name-fitted (-> col-name name (str "-fitted") keyword)
        col-name-residuals (-> col-name-fitted name (str "-residuals") keyword)]

    (->> train-data ;; data for training
         model ;; start training
         (predict test-data) ;; use model for prediction with test-data
         (#(tbl/add-column test-ds col-name-fitted %)) ;; add fitted values and then residuals
         (#(tbl/add-column % col-name-residuals (residuals % col-name col-name-fitted)))))) 

;;; ^kind/dataset
(def test-ausdata-mean
  (simple-workflow train-ausdata test-ausdata model-mean predict-mean :Beer))

;;; ^kind/dataset
(def test-ausdata-naive
  (simple-workflow train-ausdata test-ausdata model-naive predict-naive :Beer))

;;; ^kind/dataset
(def test-ausdata-snaive
  (simple-workflow train-ausdata test-ausdata model-snaive predict-snaive :Beer))

;;; ^kind/dataset
(def test-ausdata-drift
  (simple-workflow train-ausdata test-ausdata model-drift predict-drift :Beer))


;; plotting

(defn prep-traindata-for-plotting [data time-axis data-col]
  (-> data
      (tbl/select-columns [time-axis data-col])
      (tbl/convert-types time-axis :string)
      (tbl/rows :as-maps)))

(def plot-train-data (-> train-ausdata
                         (prep-traindata-for-plotting :QuarterEnd :Beer)))

(defn prep-testdata-for-plotting [data time-axis data-col predict-data-col]
  (-> data
      (tbl/select-columns [time-axis data-col predict-data-col])
      (tbl/convert-types time-axis :string)
      (tbl/rows :as-maps)))

(def plot-test-data (-> test-ausdata-drift
                        (prep-testdata-for-plotting :QuarterEnd :Beer :Beer-fitted)))

;; 1 plot for 1 model
^kind/vega
(hc/xform
 ht/layer-chart
 :DATA (concat plot-train-data plot-test-data)
 :LAYER [(hc/xform
          ht/line-chart
          :X :QuarterEnd
          :XTYPE :temporal
          :Y :Beer)
         (hc/xform
          ht/point-chart
          :X :QuarterEnd
          :XTYPE :temporal
          :Y :Beer-fitted
          :YTPE :quantitative
          :YSCALE {:zero false}          
          :MCOLOR "red")])

;; 1 plot for 4 models

(defn- mergeds
  [left-ds right-ds data-col new-col]
  (let [right-ds-sel (-> right-ds
                         (tbl/select-columns [:QuarterEnd data-col])
                         (tbl/rename-columns {data-col new-col}))]
    (-> left-ds
        (tbl/right-join right-ds-sel :QuarterEnd)
        (tbl/drop-columns [:aus-production.QuarterEnd]))))

(def merged-4-models-data (-> test-ausdata
                              (mergeds test-ausdata-mean :Beer-fitted :Beer-mean-fitted)
                              (mergeds test-ausdata-naive :Beer-fitted :Beer-naive-fitted)
                              (mergeds test-ausdata-snaive :Beer-fitted :Beer-snaive-fitted)
                              (mergeds test-ausdata-drift :Beer-fitted :Beer-drift-fitted)))


(defn prep-merged-data-for-plotting [data time-col data-col predicts-cols]
  (let [col-sels (concat (vector time-col data-col) predicts-cols)]
    (-> data
        (tbl/select-columns col-sels)
        (tbl/convert-types time-col :string)
        (tbl/rows :as-maps))))

^kind/vega
(hc/xform
 ht/layer-chart
 :DATA (concat (-> train-ausdata
                   (prep-traindata-for-plotting :QuarterEnd :Beer))

               (-> merged-4-models-data
                   (prep-merged-data-for-plotting :QuarterEnd :Beer [:Beer-mean-fitted :Beer-naive-fitted :Beer-snaive-fitted :Beer-drift-fitted])))
 
 :LAYER [(hc/xform
          ht/line-chart
          :X :QuarterEnd
          :XTYPE :temporal
          :Y :Beer)
         
         (hc/xform
          ht/point-chart
          :X :QuarterEnd
          :XTYPE :temporal
          :Y :Beer-mean-fitted
          :YTPE :quantitative
          :YSCALE {:zero false}          
          :MCOLOR "red")
         
         (hc/xform
          ht/point-chart
          :X :QuarterEnd
          :XTYPE :temporal
          :Y :Beer-naive-fitted
          :YTPE :quantitative
          :YSCALE {:zero false}          
          :MCOLOR "green")
         
         (hc/xform
          ht/point-chart
          :X :QuarterEnd
          :XTYPE :temporal
          :Y :Beer-snaive-fitted
          :YTPE :quantitative
          :YSCALE {:zero false}
          :MCOLOR "blue")
         
         (hc/xform
          ht/line-chart
          :X :QuarterEnd
          :XTYPE :temporal
          :Y :Beer-drift-fitted
          :YTPE :quantitative
          :YSCALE {:zero false}
          :MCOLOR "brown")])

