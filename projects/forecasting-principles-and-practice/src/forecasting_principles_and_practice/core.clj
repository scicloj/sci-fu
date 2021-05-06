(ns forecasting-principles-and-practice.core
  (:require [tablecloth.api :as tbl]
            [tablecloth.time.index :as idx]
            [tablecloth.time.api :refer [slice]]))
            

(require ;;'[scicloj.metamorph.core :as morph]
 '[clojure.string :as string]
 '[tech.v3.datatype.casting :as casting]
 '[tech.v3.datatype.datetime :as datetime]
 '[tech.v3.datatype.functional :as fun]
 '[scicloj.ml.core :as ml]
         ;;'[scicloj.ml.metamorph :as mm]
 '[scicloj.ml.dataset :as ds]

         ;; '[tech.v3.dataset :as ds]

         ;; '[tablecloth.pipeline :as tbl-pipe]
         ;; '[tech.v3.libs.smile.metamorph :as smile]
         ;; '[tech.v3.dataset.metamorph :as ds-mm] ;; tech.dataset support for metamorph
         ;; '[tech.v3.dataset.modelling :as ds-mod]
         ;; '[tech.v3.ml.metamorph :as ml-mm] ;; tech.ml support for metamorph

 '[time-literals.data-readers]
 '[time-literals.read-write]
 '[notespace.api :as notespace]
 '[notespace.kinds :as kind]

        ;;'[notespace.state :as state]
        ;;'[notespace.paths :as paths]
 )

(import [org.threeten.extra YearQuarter])

^kind/hidden
(comment
  (notespace/init-with-browser))

(casting/add-object-datatype! :year-quarter YearQuarter true)

;; data - parsed quarters
(def data (-> (ds/dataset "./data/aus-production.csv"
                          {:key-fn    keyword
                           :parser-fn {"Quarter" [:year-quarter
                                                  (fn [date-str]
                                                    (-> date-str
                                                        (string/replace #" " "-")
                                                        (YearQuarter/parse)))]}})
              (ds/add-or-replace-column :QuarterEnd
                                        #(tech.v3.datatype/emap (fn [x] (.atEndOfQuarter x)) :local-date (:Quarter %)))))


(def indexed-data (idx/index-by data :Quarter))

(def train-data (slice indexed-data
                       (YearQuarter/parse "1992-Q1")
                       (YearQuarter/parse "2006-Q4")))

(def test-data (slice indexed-data
                      (YearQuarter/parse "2007-Q1")
                      (YearQuarter/parse "2009-Q4")))



;; We look at 4 "models" - mean, naive, seasonal-naive and drift
;; First, we compute the models, and then use it's parameters in prediction

;; the calc(s) are the "model" implementation that compute the parameters

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


;; predictions use the "model" parameters

(defn predict-mean
  [col model]
  (repeat (tbl/row-count col) (get-in model [:model-mean :mean])))

(defn predict-naive
  [col model]
  (repeat (tbl/row-count col) (get-in model [:model-naive :naive])))

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
        val (get-in model [:model-snaive :snaive])]
    (map #(nth val %) idx)))


(defn predict-drift
  [col model]
  (let [yT (get-in model [:model-drift :yT])
        slope (float (get-in model [:model-drift :slope]))
        n (count col)]
     (map #(+ yT (/ (* slope (inc %)) n)) (range n))))


;; calc & predict
(predict-mean (test-data :Beer) {:model-mean (calc-mean (train-data :Beer))})

(predict-naive (test-data :Beer) {:model-naive (calc-naive (train-data :Beer))})

(predict-snaive (test-data :Beer) {:model-snaive (calc-snaive (train-data :Beer) 4)})

(predict-drift (test-data :Beer) {:model-drift (calc-drift (train-data :Beer))})


;; now for the metamorph pipeline
(defn ts-forecast-model []
  (fn [{:metamorph/keys [id data mode] :as ctx}]
    (case mode
      :fit (assoc ctx id {:model-mean (calc-mean (data :Beer))
                          :model-naive (calc-naive (data :Beer))
                          :model-snaive (calc-snaive (data :Beer) 4)
                          :model-drift (calc-drift (data :Beer))})

      :transform (assoc ctx id {:model-mean (predict-mean (data :Beer) (:model ctx)) 
                                :model-naive (predict-naive (data :Beer) (:model ctx))
                                :model-snaive (predict-snaive (data :Beer) (:model ctx))
                                :model-drift (predict-drift (data :Beer) (:model ctx))}))))

(def pipeline
  (ml/pipeline
   {:metamorph/id :model}
   (ts-forecast-model)))

(def training-run
  (pipeline
   {:metamorph/mode :fit
    :metamorph/data train-data}))

(:model training-run)

(def prediction-run
  (pipeline
   (merge training-run {:metamorph/mode :transform
                        :metamorph/data test-data})))

(:model prediction-run)






