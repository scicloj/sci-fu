(ns scifu33-scicloj-ml)

(require '[scicloj.ml.core :as ml]
         '[scicloj.ml.metamorph :as mm]
         '[scicloj.ml.dataset :as ds]
         '[tech.v3.datatype :as dtype]
         '[tech.v3.dataset.modelling :as tmd-model])

(def data (ds/dataset {:x (repeatedly 50 rand)}))


(def data (ds/add-column data :y (dtype/emap #(* % 3) :float64 (:x data))))

data

(def trained-model (-> data
                      (tmd-model/set-inference-target :y)
                      (scicloj.metamorph.ml/train {:model-type :smile.regression/ordinary-least-square})))

(keys trained-model)

(scicloj.metamorph.ml/predict data trained-model)



