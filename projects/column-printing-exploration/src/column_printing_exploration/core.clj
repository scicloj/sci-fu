(ns column-printing-exploration.core)


(require '[tech.v3.dataset :as ds]
         '[tablecloth.api :as tbl])


(def dataset (tbl/dataset {:x (range 100)
                           :y (take 100 (repeatedly #(rand 10)))} :mydataset))


dataset

(:x dataset)
