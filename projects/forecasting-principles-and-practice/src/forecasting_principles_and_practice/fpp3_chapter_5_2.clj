(ns forecasting-principles-and-practice.fpp3_chapter_5_2
  (:require
   [clojure.string :refer [replace join]]

   [time-literals.data-readers]
   [time-literals.read-write]

   [tech.v3.datatype :as dtype]
   [tech.v3.datatype.casting :as casting]
   [tech.v3.datatype.functional :as dfn]
   
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


(defn model-drift
  [col]
  (let [yT (last col)
        y1 (first col)
        slope (/ (- yT y1) (count col))]
    {:yT yT :slope slope}))


;; predictions: using "model" parameters
(defn fit-mean
  [col model]
  (vec (repeat (tbl/row-count col) (:mean model))))

(defn fit-naive
  [col model]
  (vec (repeat (tbl/row-count col) (:naive model))))

;; helper to calculate the seasonal index. see book for the formula
(defn- snaive-index
  [T m]
  (fn [h]
    (let [k (quot (- h 1) m)
          ix (- (+ T h) (* m (+ k 1)))]
      (dec (+ ix m)))))

(defn fit-snaive
  [col model]
  (let [T 0
        m 4
        idx (map (snaive-index T m) (map inc (range (count col))))
        val (:snaive model)]
    (vec (map #(nth val %) idx))))

(defn fit-drift
  [col model]
  (let [yT (:yT model)
        slope (float (:slope model))
        n (count col)]
    (vec (map #(+ yT (/ (* slope (inc %)) n)) (range n)))))


(def model-snaive
  (model-snaive-fn 4))

(fit-mean (test-ausdata :Beer)
              (model-mean (train-ausdata :Beer)))

(fit-naive (test-ausdata :Beer)
               (model-naive (train-ausdata :Beer)))

(fit-snaive (test-ausdata :Beer)
                (model-snaive (train-ausdata :Beer)))

(fit-drift (test-ausdata :Beer)
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
  [train-ds test-ds model fit col-name]
  (let [train-data (train-ds col-name)
        test-data (test-ds col-name)
        col-name-fitted (str col-name ".fitted")
        col-name-residuals (str col-name-fitted ".residuals")]
    
    (->> train-data
         model
         (fit test-data)
         (#(tbl/add-column test-ds col-name-fitted %))
         (#(tbl/add-column % col-name-residuals (residuals % col-name col-name-fitted))))))

^kind/dataset
(simple-workflow train-ausdata test-ausdata model-mean fit-mean :Beer)

^kind/dataset
(simple-workflow train-ausdata test-ausdata model-naive fit-naive :Beer)

^kind/dataset
(simple-workflow train-ausdata test-ausdata model-snaive fit-snaive :Beer)

^kind/dataset
(simple-workflow train-ausdata test-ausdata model-drift fit-drift :Beer)


;; TODO:

;; Plot #1
;; append train data, with test data. test data will have additional fitted/residual columns
;; plot X - temporal - :QuarterEnd         (from train-ausdata, test-ausdata)
;; plot Y - data - :Beer                   (from train-ausdata, test-ausdat)
;; plot Y - data - :Beer.mean.fitted       (from fit-mean-data)
;; plot Y - data - :Beer.naive.fitted      (from fit-naive-data)
;; plot Y - data - :Beer.snaive.fitted     (from fit-snaive-data)
;; plot Y - data - :Beer.drift.fitted      (from fit-drift-data)

;; Plot #2
;; plot X - temporal - :QuarterEnd         (from test-ausdata)
;; plot Y - data - :Beer.mean.resiual      (from fit-mean-data)
;; plot Y - data - :Beer.naive.residual    (from fit-naive-data)
;; plot Y - data - :Beer.snaive.residual   (from fit-snaive-data)
;; plot Y - data - :Beer.drift.residual    (from fit-drift-data)





