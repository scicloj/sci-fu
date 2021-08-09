(ns epoch-month-exploratioin.core
  (:require [tech.v3.dataset :as ds]
            [tech.v3.dataset.reductions :as ds-reduce]
            [tech.v3.datatype.datetime :as dtype-dt]
            ))

(def test-ds (ds/->dataset {:person-id (repeatedly 1000 #(rand-int 100))
                            :start-date (dtype-dt/minus-temporal-amount (dtype-dt/local-date) (repeatedly 1000 #(rand-int 180)) :days)
                            :end-date (dtype-dt/plus-temporal-amount (dtype-dt/local-date) (repeatedly 1000 #(rand-int 180)) :days)
                            :location-id (repeatedly 1000 #(rand-int 100))}))

(ds/head test-ds)

(def with-months (assoc test-ds
                        :sd-month (dtype-dt/long-temporal-field :months (:start-date test-ds))
                        :ed-month (dtype-dt/long-temporal-field :months (:end-date test-ds))))

(ds/head with-months)

(import '[tech.v3.datatype ArrayHelpers])


(def reducer
  (reify tech.v3.datatype.IndexReduction
    (prepareBatch [this dataset] dataset)
    (reduceIndex [this dataset obj-ctx idx]
      (let [^longs obj-ctx (or obj-ctx (long-array 12))
            sd (long ((dataset :sd-month) idx))
            ed (long ((dataset :ed-month) idx))
            diff (- ed sd)
            diff (if (< diff 0)
                   (+ diff 12)
                   diff)]
        (dotimes [idx diff]
          (ArrayHelpers/accumPlus obj-ctx (rem (+ sd idx) 12) 1))
        obj-ctx))
    (reduceReductions [this lhs-ctx rhs-ctx]
      (tech.v3.datatype.functional/+ lhs-ctx rhs-ctx))
    (finalize [this ctx]
      (vec ctx))))

(tech.v3.dataset.reductions/group-by-column-agg
 :location-id
 {:counts-by-month reducer
  :location-id (ds-reduce/first-value :location-id)}
 [(ds/select-rows with-months (range 100))])

;; Try with tablecloth.time

(require '[tablecloth.api :as tbl]
         '[tablecloth.time.api :as tbl-time])

(-> test-ds
    ;; (tbl-time/adjust-frequency tbl-time/->months-end)
    tbl/head
    )


  (transduce
   (comp
    (mapcat (fn [{:keys [start end location]}]
              (map (fn [d] {:location location :date d})
                   (tick/range start end)))))
   (fn
     ([] {})
     ([acc] acc (-> (tbl/dataset
                     (into []
                           (map (fn [[[location date] v]]
                                  {:location location :date date :person-days v}))
                           acc))
                    (tbl/map-columns :year-month [:date] (fn [d] (tick/year-month d)))
                    (tbl/group-by [:location :year-month])
                    (tbl/aggregate {:person-days-per-month #(tech.v3.datatype.functional/sum (:person-days %))})
                    (tbl/order-by [:year-month :location])
                    (tbl/reorder-columns [:year-month :location :person-days-per-month])))
     ([acc {:keys [location date]}]
      (update acc [location date] (fnil inc 0))))
   [{:id 1 :start (#time/date "2021-01-22") :end (#time/date "2021-02-06") :location "Barry Buddon"}
    {:id 1 :start (#time/date "2021-02-07") :end (#time/date "2021-02-08") :location "Tentsmuir Woods"}
    {:id 2 :start (#time/date "2021-01-23") :end (#time/date "2021-01-31") :location "Barry Buddon"}])


(require '[tick.alpha.api :as tick])

(def mydata [{:id 1 :start (tick/date "2021-01-22") :end (tick/date "2021-02-06") :location "Barry Buddon"}
             {:id 1 :start (tick/date "2021-02-07") :end (tick/date "2021-02-08") :location "Tentsmuir Woods"}
             {:id 2 :start (tick/date "2021-01-23") :end (tick/date "2021-01-31") :location "Barry Buddon"}])

(def expanded (->> mydata
                  (mapcat (fn [{:keys [start end location]}]
                   (map (fn [d] {:location location :date d})
                        (tick/range start end))))))

(tbl/head (tbl/dataset expanded))
tech.v3.datatype.functional/

(require '[tablecloth.api :as tbl]
         '[tablecloth.time.api :as tbl-time]
         '[tick.alpha.api :as tick])

(-> [{:id 1 :start (tick/date "2021-01-22") :end (tick/date "2021-02-06") :location "Barry Buddon"}
     {:id 1 :start (tick/date "2021-02-07") :end (tick/date "2021-02-08") :location "Tentsmuir Woods"}
     {:id 2 :start (tick/date "2021-01-23") :end (tick/date "2021-01-31") :location "Barry Buddon"}]
    (->> (mapcat (fn [{:keys [start end location]}]
                   (map (fn [d] {:location location :date d})
                        (tick/range start end)))))
    (tbl/dataset)
    (tbl-time/index-by :date)
    (tbl-time/adjust-frequency tbl-time/->months-end {:categories [:location]})
    (tbl/aggregate #(tbl/row-count %)))
;; => _unnamed [3 3]:
;;    | :summary |       :location |      :date |
;;    |---------:|-----------------|------------|
;;    |       18 |    Barry Buddon | 2021-01-31 |
;;    |        5 |    Barry Buddon | 2021-02-28 |
;;    |        1 | Tentsmuir Woods | 2021-02-28 |
