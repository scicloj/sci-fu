(ns column-based-array.core
 (:require [tech.v3.dataset.impl.column :refer [new-column]]
           [tech.v3.dataset.column :as col]
           [tech.v3.dataset :as ds]
           [tech.v3.datatype :as dtype]
           [tech.v3.datatype.functional :as fun]))


(def datatype-hierarchy
  (-> (make-hierarchy)
      (derive :int8 :int)
      (derive :int16 :int)
      (derive :int32 :int)
      (derive :int64 :int)
      (derive :float32 :float)
      (derive :float64 :float)
      (derive :uint8 :uint)
      (derive :uint16 :uint)
      (derive :uint32 :uint)
      (derive :uint64 :uint)))

(defn typeof [col]
  (dtype/elemwise-datatype col))

(defn typeof? [col datatype]
  (let [detected-dtype (dtype/elemwise-datatype col)]
    (or (= datatype detected-dtype)
        (isa? datatype-hierarchy
              detected-dtype
              datatype))))

(defn column
  ([data]
   (column data {:name nil}))
  ([data {:keys [name]}]
   (col/new-column data)))


(comment
  ;; working through r4ds atomic vectors: https://r4ds.had.co.nz/vectors.html 

  (defn mysample [n]
    (column (take n (repeatedly #(rand-int 20)))))

  ;; implicit coercion

  (def x
    (column (take 100 (repeatedly #(rand-int 20)))))

  x

  (def y (fun/> x 10))

  y

  (fun/sum y)

  (fun/mean y)

  ;; unlike R, this defaults to :object when mixed
  (contains (column ["a" 1.5]))

  ;; test functions
  (contains (column [1 2]))
  (contains? (column [1 2]) :int64)
  (contains? (column [1.0 2.0]) :float)

  ;; scalars

  ;; (sample(10) + 100)
  (fun/+ (mysample 10) 100)

  ;; runif(10) > 0.5
  (fun/> (mysample 10)  10)

;; adding vectors of differnet lengths?

  ;; 1:10 + 1:2
  ;; nope! but this is probably good. R's tibble doesn't allow this
  (fun/+
   (column (range 10))
   (column (range 2)))

  ;; named - this could be added. partly a printing issue
  ;; c(x = 1, y = 2, z = 4)

  ;; subsetting

  ;; x <- c("one", "two", "three", "four", "five")
  ;; x[c(3, 2, 5)]
  (def x2 (column ["one" "two" "three" "four" "five"]))

  x2

  (col/select x2 [2 1 4])

  ;; also sub buffer or "subvec"
  (dtype/sub-buffer x2 1 2)

;; repeating a position
  ;; x[c(1, 1, 5, 5, 5, 2)]
  (col/select x2 [0 0 4 4 4 1])

  ;; negative values drop elements - i.e. exclude drop
  ;; x[c(-1, -3, -5)]
  ;; I don't think we have a good way to do this

  ;; subsetting with logical vector
  (defn indices [pred coll]
    (keep-indexed #(when (pred %2) %1) coll))

  ;;x[!is.na(x)] 
  ;; hmmm this is not great
  (def x3 (column [10 3 nil 5 8 1 nil]))

  x3
  ;; is.na(x)
  (fun/nan? x3)

  (col/select x3 (fun/nan? x3))

  (col/select x3 (indices false? (fun/nan? x3)))


  (column (fun/+ (column [1 2 3]) 10))


;; missing values
  (tech.v3.datatype.functional/+
   (column [1 nil 3])
   (column [1 2 3]));; => [2.0 ##NaN 6.0]

  ;; comment on this fn say it is not NaN aware
  (tech.v3.datatype.functional/sum
   (column [1 nil 3]));; => -9.223372036854776E18

  (tech.v3.datatype.statistics/sum
   (column [1 nil 3]));; => 4.0

  (tech.v3.datatype.functional/+
   (column [-9223372036854775806 nil -9223372036854775806])
   (column [1 1 1]));; => [-9.223372036854776E18 ##NaN -9.223372036854776E18]

  (tech.v3.datatype.functional/+
   (column ["a" "b" "c"])
   (column ["a" "b" "c"]))

  (column (fun/+ (column (range 10))
                 (rand-int 10))))
