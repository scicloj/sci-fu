(ns time-series-index.core
  (:import java.util.TreeMap)
  (:require [tech.v3.datatype.datetime :as dt]
            [tech.v3.dataset :as ds]
            [tablecloth.api :as tablecloth]
            [notespace.api :as notespace]
            [notespace.kinds :as kind]))

(def path "./data/air-passengers.csv")

(def data
  (-> path
      (tablecloth/dataset
       {:key-fn keyword
        :parser-fn {"Month" [:packed-local-date
                             (fn [date-str]
                               (java.time.LocalDate/parse
                                (str date-str "-01")))]}})))

^kind/dataset
data

(defn make-index [data index-column-key]
  (-> data
      (tablecloth/rows :as-maps)
      (->> (map-indexed (fn [row-number row-map]
                          [(index-column-key row-map) row-number]))
           (into {})
           (TreeMap.))))

(def dt-index (make-index data :Month))

(defn slice [ds index from to]
  (let [from-key (java.time.LocalDate/parse from)
        to-key (java.time.LocalDate/parse to)
        row-numbers (-> index
                        (.subMap from-key to-key)
                        (.values))]
    (tablecloth/select-rows ds
                            row-numbers)))

(slice data dt-index "1949-01-01" "1949-07-01")

;; doesn't work. Would need to perhaps guess when the user
;; included just a month and do some extra processing.
;; e.g.: https://stackoverflow.com/users/2670892/greg-449
(slice data dt-index "1949" "1951")

;; What about having a fn that includes the index?
;; (defn indexed-dataset [data index-column options]
;;   (tablecloth/dataset data ))
