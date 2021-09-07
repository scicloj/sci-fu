(ns dtype-exploration.dtype-column-basis
  (:require [tech.v3.dataset :as ds]
            [tech.v3.datatype :as dtype]))


(def stocks
  (ds/->dataset "https://raw.githubusercontent.com/techascent/tech.ml.dataset/master/test/data/stocks.csv"))

(ds/head stocks)


(ds/head (ds/add-or-update-column stocks "id"
                                  (dtype/make-reader :int64
                                                     (ds/row-count stocks)
                                                     idx)))

(stocks "symbol")

(require '[tech.v3.datatype.functional :as dfn])

(def stocks-lag
  (assoc stocks "price-lag"
         (let [price-data (dtype/->reader (stocks "price"))]
           (dtype/make-reader :float64 (.lsize price-data)
                              (.readDouble price-data
                                           (max 0 (dec idx)))))))

(assoc stocks-lag "price-lag-diff" (dfn/- (stocks-lag "price")
                                          (stocks-lag "price-lag")))

(-> (stocks-lag "price-lag")
    .data
    dtype/datatype)
;; => :buffer

(-> (stocks-lag "price-lag")
    .data
    type)
;; => dtype_exploration.dtype_column_basis$fn$reify__43103


(def cloned (ds/update-column stocks-lag "price-lag" dtype/clone))

(-> (cloned "price-lag")
    .data
    dtype/datatype)
;; => :array-buffer

