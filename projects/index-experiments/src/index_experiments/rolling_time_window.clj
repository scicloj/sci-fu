(ns index-experiments.rolling-time-window
  (:require [notespace.api]))

["# Rolling time-window -- checking different implementations"]

(require '[notespace.kinds :as kind]
         '[tablecloth.api :as tablecloth]
         '[tech.v3.datatype :as dtype]
         '[tech.v3.datatype.functional :as fun]
         '[tech.v3.datatype.datetime :as datetime]
         '[tech.v3.datatype.argops :as argops])
(import java.util.TreeMap)

["## Example data"]

(def n 10000)

(defn random-datetime []
  (datetime/milliseconds->datetime :local-date-time
                                   (* 1000 (rand-int 999999999))))

(random-datetime)

(def ds
  (-> {:datetime (dtype/make-reader :local-date-time n (random-datetime))
       :x        (repeatedly n rand)}
      tablecloth/dataset
      (tablecloth/order-by :datetime)
      (tablecloth/add-or-replace-column :i (dtype/make-reader :int64 n idx))))

^kind/dataset
ds

["## Finding the row-numbers of the last few days"]

(def milliseconds-in-an-day (* 24 60 60 1000))

milliseconds-in-an-day

["Given a point in time, we want to find the row-numbers of the last few days."]

["First, let us do it using `argfilter`. We will use milliseconds arithmetic, though we could work directly with datetimes."]

(defn row-numbers-of-last-few-days-by-argfilter [reference-datetime num-days]
  (let [end   (datetime/local-date-time->milliseconds-since-epoch
               reference-datetime)
        start (- end
                 (* num-days milliseconds-in-an-day))]
    (->> ds
         :datetime
         (argops/argfilter (fn [datetime]
                             (<= start
                                 (datetime/local-date-time->milliseconds-since-epoch
                                  datetime)
                                 end))))))

["Let us try this on some point in time."]

(def datetime20
  (-> ds
      :datetime
      (nth 20)))

datetime20

["What row numbers are relevant for the last 5 with respect to that instance?"]

(row-numbers-of-last-few-days-by-argfilter
 datetime20
 5)

["You can verify by looking at the dataset above."]

["Now, let us do the same using an index structure."]

(def index
  (TreeMap. ^java.util.Map
            (zipmap (dtype/emap datetime/local-date-time->milliseconds-since-epoch
                                :int64
                                (:datetime ds))
                    (dtype/make-reader :int64 n idx))))

(defn row-numbers-of-last-few-days-by-index [reference-datetime num-days]
  (let [end   (datetime/local-date-time->milliseconds-since-epoch
               reference-datetime)
        start (- end
                 (* num-days milliseconds-in-an-day))]
    (-> (.subMap ^TreeMap
                 index
                 start
                 true
                 end
                 true)
        (.values))))

["Checking the same example:"]

(row-numbers-of-last-few-days-by-index
 datetime20
 14)

["## Measuring time"]

(defn compute-with-time-measurement [f]
  (let [start-time (datetime/instant)
        result (f)
        end-time (datetime/instant)]
    {:result result
     :duration (datetime/between start-time end-time :milliseconds)}))

(compute-with-time-measurement #(Thread/sleep 1000))

["## Computing a rolling time window average"]

["For every moment in time, we will compute the average of the `:x` column valeus at the last 5 days. Then, we will compute the standard deviation of all these averages."]

["First, let us do it in the argfilter way."]

(compute-with-time-measurement
 (fn []
   (->> ds
        :datetime
        (dtype/emap (fn [datetime]
                      (let [row-numbers (row-numbers-of-last-few-days-by-argfilter
                                         datetime
                                         5)]
                        (-> ds
                            (tablecloth/select-rows row-numbers)
                            :x
                            fun/mean)))
                    :float64)
        fun/standard-deviation)))

["Now, let us do it in the index way."]

(compute-with-time-measurement
 (fn []
   (->> ds
        :datetime
        (dtype/emap (fn [datetime]
                      (let [row-numbers (row-numbers-of-last-few-days-by-index
                                         datetime
                                         5)]
                        (-> ds
                            (tablecloth/select-rows row-numbers)
                            :x
                            fun/mean)))
                    :float64)
        fun/standard-deviation)))

["Same result, different durations."]

["."]
