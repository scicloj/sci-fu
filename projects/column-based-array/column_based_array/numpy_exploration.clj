(ns column-based-array.numpy-exploration
  (:require [column-based-array.core :as core :refer [column]]
            [tech.v3.datatype :as dtype]))


["## Array creation"]

;; a = np.array([2, 3, 4])
(def a (column [2, 3, 4]))

;; a.dtype
(core/typeof a)

;; b = np.array([1.2, 3.5, 5.1])
(def b (column [1.2, 3.5, 5.1]))

;; b.dtype
(core/typeof b)

;; b = np.array([(1.5, 2, 3), (4, 5, 6)])
;; => creates multidimensional array
;; We have no equivalent for this. there is a pathway to enable
;; multidimensional arrays on the column but needs work in tmd.


;; Skipped over sections on "printing" and "basic operations"

["## Indexing, Slicing, and Iterating"]

;; np.arange(10)**3
(def a (tech.v3.datatype.functional/pow (column (range 10)) 3))

;; a[2]
(nth a 2)

;; a[2:5]
;; not a great approximation, not really the same thing
;; need a slice i think
(tech.v3.datatype/sub-buffer a 2 3)


["## Advanced indexing"]

;; a = np.arrange(12)**2
;; i = np.array([1, 1, 3, 8, 5])
;; a[i]
(let [a (tech.v3.datatype.functional/pow (column (range 10)) 2)
      idxs (column [1 1 3 8 5])]
  a
  )

