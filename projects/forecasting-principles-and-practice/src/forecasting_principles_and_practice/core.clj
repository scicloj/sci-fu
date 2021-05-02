(ns forecasting-principles-and-practice.core)

(require ;;'[scicloj.metamorph.core :as morph]
 '[tech.v3.datatype.datetime :as datetime]
 '[tech.v3.datatype.functional :as fun]
 '[scicloj.ml.core :as ml]
         ;;'[scicloj.ml.metamorph :as mm]
 '[scicloj.ml.dataset :as ds]

         ;; '[tech.v3.dataset :as ds]
 '[tablecloth.api :as tbl]
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

 '[tablecloth.time.index :as index])

^kind/hidden
(comment
  (notespace/init-with-browser))

  ;; data
(def data (ds/dataset "./data/aus-production.csv" {:key-fn keyword}))
;; => #'forecasting-principles-and-practice.core/data

;;(ds/dataset? data)
;;(def tbldata (tbl/dataset data))
;;(tbl/info tbldata)

(def indexed-data (index/index-by data :Quarter))

^kind/dataset
indexed-data

;; define pipeline

;; y-hat(T+h|T) = y-bar = (y(1) + y(2) ... y(T))/T
(defn calc-mean [dataset col-name]
  (-> (col-name dataset) (fun/mean)))

;; y-hat(T+h|T) = y(T)
(defn calc-naive [dataset col-name]
  (-> (col-name dataset) last))

;; y-hat(T+h|T) = y(T+h-m(k+1))
;; m is seasonal period
(defn calc-seasonal-naive [dataset col-name]
  (-> (col-name dataset) last))

(defn calc-drift [dataset col-name]
  (-> (col-name dataset) last))

;; (defn snaive
;;   [dataset m]
;;   (fn
;;     [T h]
;;     (let [k (quot (- h 1) m)]
;;       (- (+ T h) (* m (+ k 1))))))

;; (defn snaive2
;;   [dataset m]
;;   (fn
;;     [h]
;;     (let [k (quot (- h 1) m)]
;;       (- h (* m (+ k 1))))))


;; (def sdf (snaive "x" 4))

;; (def hs (take 16 (range)))

;; <................><....>


(defn ts-prediction-model []
  (fn [{:metamorph/keys [id data mode] :as ctx}]
    (case mode
      :fit (assoc ctx id {:mean (calc-mean data :Beer)
                          :naive (calc-naive data :Beer)
                          :snaive (calc-seasonal-naive data :Beer)
                          :drift (calc-drift data :Beer)})

      :transform (assoc ctx id {:mean (:mean (:model ctx))
                                :naive (:naive (:model ctx))}))))

(def pipeline
  (ml/pipeline
   {:metamorph/id :model}
   (ts-prediction-model)))

;; returns a ctx
(def training-run
  (pipeline
   {:metamorph/mode :fit
    :metamorph/data data}))

(def prediction-run
  (pipeline
   (merge training-run {:metamorph/mode :transform})))


(keys training-run)
(get training-run :model)

(keys prediction-run)
(get prediction-run :model)





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
