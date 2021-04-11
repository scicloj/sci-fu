(ns forecasting-principles-and-practice.core)

(require '[scicloj.metamorph.core :as morph]
         '[tech.v3.datatype.datetime :as datetime]
         '[tech.v3.datatype.functional :as fun]
         '[scicloj.ml.core :as ml]
         '[scicloj.ml.metamorph :as mm]
         '[scicloj.ml.dataset :as ds]
         ;; '[tech.v3.dataset :as ds]
         ;; '[tablecloth.api :as tbl]
         ;; '[tablecloth.pipeline :as tbl-pipe]
         ;; '[tech.v3.libs.smile.metamorph :as smile]
         ;; '[tech.v3.dataset.metamorph :as ds-mm] ;; tech.dataset support for metamorph
         ;; '[tech.v3.dataset.modelling :as ds-mod]
         ;; '[tech.v3.ml.metamorph :as ml-mm] ;; tech.ml support for metamorph
         '[time-literals.data-readers]
         '[time-literals.read-write]
         )


;; get data

(def data (ds/dataset "./aus-production.csv" {:key-fn keyword}))

data

;; define pipeline

(defn calc-mean [dataset col-name]
  (-> (col-name dataset) (fun/mean)))


(defn calc-naive [dataset col-name]
  (-> (col-name dataset) last ))


(defn naive-ts-prediction-model []
  (fn [{:metamorph/keys [id data mode] :as ctx}]
    (case mode
      :fit (assoc ctx id {:mean (calc-mean data :Beer)
                          :naive (calc-naive data :Beer)}))))

(def pipeline
  (ml/pipeline
   {:metamorph/id :model}
   (naive-ts-prediction-model)))


(def test-run
  (pipeline
    {:metamorph/mode :fit
     :metamorph/data data}))

(keys test-run)

(get test-run :model)










;; (time-literals.read-write/print-time-literals-clj!)

;
; generate some sample data

;; (defn time-index [start-inst n tf]
;;   (datetime/plus-temporal-amount start-inst (range n) tf))

;; (defn get-test-ds [start-time num-rows temporal-unit]
;;   (tbl/dataset {:idx (time-index start-time num-rows temporal-unit)
;;                 :value (take num-rows (repeatedly #(rand 200)))}))

;; (get-test-ds #time/date "1970-01-01" 10 :days)
;; => _unnamed [10 2]:
;;    |        :idx |       :value |
;;    |-------------|--------------|
;;    |  1970-01-01 | 158.80935033 |
;;    |  1970-01-02 |  68.70225457 |
;;    |  1970-01-03 | 138.60236201 |
;;    |  1970-01-04 | 165.35792971 |
;;    |  1970-01-05 | 111.34070679 |
;;    |  1970-01-06 |   3.26339136 |
;;    |  1970-01-07 | 176.26695269 |
;;    |  1970-01-08 |  62.31989768 |
;;    |  1970-01-09 | 150.90881728 |
;;    |  1970-01-10 |  35.81470956 |

;; define a pipeline for naive time forecasting



;; first we splite the data into test & training sets




;; run the pipeline fn in :fit mode on the training data





;; prediction step
