(ns scifu-38.workshop
  (:require [criterium.core :refer [quick-bench]]))

(require '[tech.v3.datatype :as dtype]
         '[tech.v3.datatype.functional :as fun]
         '[tech.v3.tensor :as tensor])

; Goals
;; this workshop will focus on working with dtype-next.
;; by the end you will understand
;; 1. what dtype-next is
;; 3. how it's currently used in data science ecosystem
;; 2. why it's useful
;; 4. its key concepts needed to grasp when working with the library
;; 5. simple examples illustrating its use

;; The workshop will not explore realistic real world examples but
;; rather focus on introducing the features and use of the library

; 1.
; Intro
; view on GitHub
; https://github.com/cnuernber/dtype-next

; Chris Nuernberger - company use, open-sourced
; Low-level library for efficiently processing collections of typed entities (abstract)
;

; 3. 
; TMD, Tablecloth, etc, ...

; 2.
(quick-bench (reduce + (range 1000000)))
(quick-bench (fun/sum (dtype/->reader (range 1000000) :int32)))

;; 4.
;; Core concept: Buffers
;; - "arrays"
;; - More-generic reader buffer/write buffer
;; - Reader buffer source of values (immutable) update -> copy
;; - Writer buffer (mutable) data updated in place (destructive operations)

;; Buffer properties
;; - countable, random access, sequence of typed elements
;; - lazy and non-caching
(dtype/as-buffer [1 2 3 4])

(class (dtype/as-buffer [1 2 3 4]))
;; Why reify? Buffer is interface 

(dtype/datatype [2 4 6])
(dtype/datatype (dtype/as-buffer [1 2 3 4]))

(dtype/reader? (dtype/as-buffer [1 2 3 4]))

(dtype/elemwise-datatype (dtype/as-buffer [1 2 3 4]))

(def rdr (dtype/->reader [1 2 3 4] :int32))
(dtype/elemwise-datatype rdr)

(dtype/->reader [1 2 3 4.2] :int32)
(dtype/elemwise-datatype (dtype/->reader [1 2 3 4.2] :int32))

(dtype/ecount rdr)
(count rdr)
(nth rdr 2)

;;   Significant, non-intuitive?
(def big-rdr (dtype/make-reader :int64 1000000 (* idx (rand-int 100))))
(take 5 big-rdr)

(def realized-br (dtype/make-container big-rdr))
(take 5 realized-br)

(dtype/writer? rdr)
(dtype/writer? (dtype/make-container rdr))

;; 5.
;; Integrated into Clojure - change in result type

;; Preserve type - tech.v3.datatype.functional
;; Evaluate chain of operations w/o committing to storage
;; (-> rdr
;;     (fun/
