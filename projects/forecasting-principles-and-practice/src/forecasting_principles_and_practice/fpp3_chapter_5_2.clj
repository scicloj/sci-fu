(ns forecasting-principles-and-practice.fpp3_chapter_5_2
  (:require
   [clojure.string :refer [replace join]]

   [time-literals.data-readers]
   [time-literals.read-write]

   [tech.v3.datatype :as dtype]
   [tech.v3.datatype.casting :as casting]
   [tech.v3.datatype.functional :as fun]
   
   [tablecloth.api :as tbl]
   [tablecloth.time.index :as idx]
   [tablecloth.time.api :refer [slice]]

   ;; [aerial.hanami.common :as hc]
   ;; [aerial.hanami.templates :as ht]
   ;; [aerial.hanami.core :as hmi]
            
   [notespace.api :as notespace]
   [notespace.kinds :as kind]
))
            

#_(import java.time.LocalDate)
#_(import java.util.Locale)
#_(import java.time.format.TextStyle)
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


^kind/hidden
(comment
  (notespace/init-with-browser)
  (notespace/eval-this-notespace)
  (notespace/eval-and-realize-this-notespace))


(casting/add-object-datatype! :year-quarter YearQuarter true)

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
      (idx/index-by :Quarter)))


(def train-ausdata (slice ausdata
                       (YearQuarter/parse "1992-Q1")
                       (YearQuarter/parse "2006-Q4")))

(def test-ausdata (slice ausdata
                      (YearQuarter/parse "2007-Q1")
                      (YearQuarter/parse "2009-Q4")))


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


(predict-mean (test-ausdata :Beer)
              (calc-mean (train-ausdata :Beer)))

(predict-naive (test-ausdata :Beer)
               (calc-naive (train-ausdata :Beer)))

(predict-snaive (test-ausdata :Beer)
                (calc-snaive (train-ausdata :Beer) 4))

(predict-drift (test-ausdata :Beer)
               (calc-drift (train-ausdata :Beer)))

;; R-lang augment approach
;; data <- data(...)
;; model <- model(...)
;; wf <- workflow(model, ...)
;; fit <- fit(wf, data)
;; augmented_data <- augment(fit, data) 

;; proposed augment approach
;; (augment ds predictions)
;; (-> ds
;;     (augment predictions)
;;     residuals
;;     ...)


;; proposed augment approach
;;  - allow multiple models
;; (augment ds predictions)
;; (-> ds
;;     (augment predictions1)
;;     (augment predictions2)
;;     residuals
;;     ...)


(defn augment-simple
  [ds col-name col-data]
  (-> ds
      (tbl/add-column col-name col-data)))

(defn- drop-col-name
  [ds col-name]
  (keyword (str (tbl/dataset-name ds) "." (name col-name))))

(defn augment-match
  [ds-left ds-right match]
  (let [drop-col (drop-col-name ds-left match)]
    (-> ds-left
        (tbl/left-join ds-right match)
        (tbl/drop-columns drop-col))))


;; (def c1 (calc-mean (train-ausdata :Beer)))
;; (def p-c1 (predict-mean (test-ausdata :Beer) c1))

;; (def aug-test-ausdata (augment-simple test-ausdata :Beer-fit p-c1))

;; (def aug-data-ausdata
;;   (augment-match ausdata (tbl/select-columns aug-test-ausdata [:column-0 :Beer-fit]) :column-0))

;; ^kind/dataset
;; aug-data-ausdata

(def model-mean (calc-mean (train-ausdata :Beer)))
(def model-naive (calc-naive (train-ausdata :Beer)))
(def model-snaive (calc-snaive (train-ausdata :Beer) 4))
(def model-drift (calc-drift (train-ausdata :Beer)))

(def forecast-mean (predict-mean (test-ausdata :Beer) model-mean))
(def forecast-naive (predict-naive (test-ausdata :Beer) model-naive))
(def forecast-snaive (predict-snaive (test-ausdata :Beer) model-snaive))
(def forecast-drift (predict-drift (test-ausdata :Beer) model-drift))

(def augment-mean (augment-simple test-ausdata :Beer-mean forecast-mean))
(def augment-naive (augment-simple test-ausdata :Beer-naive forecast-naive))
(def augment-snaive (augment-simple test-ausdata :Beer-snaive forecast-snaive))
(def augment-drift (augment-simple test-ausdata :Beer-drift forecast-drift))

^kind/dataset
(-> ausdata
    (augment-match (tbl/select-columns augment-mean [:column-0 :Beer-mean]) :column-0)
    (augment-match (tbl/select-columns augment-naive [:column-0 :Beer-naive]) :column-0)
    (augment-match (tbl/select-columns augment-snaive [:column-0 :Beer-snaive]) :column-0)
    (augment-match (tbl/select-columns augment-drift [:column-0 :Beer-drift]) :column-0))

