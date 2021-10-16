(ns scifu-33.main
  (:require [criterium.core :refer [quick-bench]]))

(require '[tech.v3.datatype :as dtype]
         '[tech.v3.datatype.functional :as fun])

;; what is dtype-next?

;; it lets us have collections of typed things and operate on them efficiently

(quick-bench (reduce + (range 1000000)))
(quick-bench (fun/sum (dtype/->buffer (range 1000000) :int32)))


;; it is therefore something comparable to Python's numpy and R's "atomic" vectors

;; R => c(1, 2, 3)
;; Numpy => numpy.array([1, 2, 3], dtype=int64)
(dtype/->reader [1 2 3] :int64)


;; but just as these libraries are not really comparable, neither is dtype-next
;; - it is lower-level than Numpy or R vectors.
;; - it is novel in that it's key abstractions for holding vectors are lazy & non-caching


;; this workshop will focus on working with dtype-next.
;; by the end you will understand
;; - what dtype-next is
;; - what are the key entities / abstractions for holding and maniuplating data in 2d & 3d
;; - what it's lazy non-caching features aren
;; - how they can get you in trouble
;; - how they can help you out
