(ns scifu-38.buffers-intro
  (:require [criterium.core :refer [quick-bench]]))


(require '[tech.v3.datatype :as dtype]
         '[tech.v3.datatype.functional :as fun])

;; Assuming an introduction that introduces the use-case
;; argument for dtype-next, and part of that is explaining
;; that dtype-next allows us to work efficiently over typed
;; collections of elements called buffers.


;; dtype-next offers up a range of abstractions that allow us to work with
;; typed collections of things.

;; The buffer is the core abstraction. It is:
;; - a countable
;; - random accesss
;; - set of elements
;; - of things that are all the same type

;; You can create a buffer in many ways

;; We can represent data as a vector and turn it into a buffer
(dtype/as-buffer [1 2 3 4])


(type (type/as-buffer [1 2 3 4]))
;; That is unususual! What does that mean. Well, `reify` is a way of making a protocol/interface
;; concrete on the fly.  So why do we see this here?

;; Because dtype-next buffers are highly abstract, they are in fact just an interface.
;; We can see it here:
;; https://github.com/cnuernber/dtype-next/blob/master/java/tech/v3/datatype/Buffer.java#L20-L41

;; The way each buffer works with data depends on the type of buffer that is created, and the
;; way each of these buffers' functions is implemented. But this is the library internals that
;; we do not need to think about too much.

;; The thing we want to know is we what we are working with in the world of dtype-next.

(dtype/datatype [1 2 3])

(dtype/datatype (dtype/as-buffer [1 2 3]))

(dtype/datatype (dtype/make-container (dtype/as-buffer [1 2 3])))

;; So this way we can know what kind of thing we are working with, but what about what is inside?

(dtype/get-datatype (dtype/as-buffer [1 2 3]))

;; But why is the type :object? Anyone know why?

(-> (int-array [1 2 3])
    (dtype/as-buffer)
    (dtype/get-datatype))

(-> (double-array [1 2 3])
    (dtype/as-buffer)
    (dtype/get-datatype))

;; These are some of the very basic properties of buffers. Often times when we work with buffers,
;; we don't use the parts of the API that speak about buffers specifically. More frequently we
;; work with "readers".

(dtype/->reader [1 2 3])

;; A reader is just a buffer that is readable, meaning you can read values from it. 
(dtype/datatype (dtype/->reader [1 2 3]))
(dtype/reader? (dtype/->reader [1 2 3]))

;; There are writers too! We don't use them much. You might use them if you need to mutate data in order
;; to optimize processing over large amounts of data. I.e. sometimes mutability is needed b/c it is faster.

;; Most importantly...readers are lazy and non-caching
(def a-big-reader (dtype/make-reader :int64 1000000 (rand-int 100)))

;; Notice:
;;   - lazy - no 
;;   - non-caching
(take 10 a-big-reader)

;; Note that many clojure functions will work with a reader
(map inc (take 10 a-big-reader))
(reduce + (take 10 a-big-reader))


;; 


