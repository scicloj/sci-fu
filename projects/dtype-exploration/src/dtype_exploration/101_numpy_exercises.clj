(ns dtype-exploration.100-numpy-exercises)



;; https://Www.machinelearningplus.com/python/101-numpy-exercises-python/

;; 1. Import and print verssion

(require '[tech.v3.datatype :as dtype])
(require '[tech.v3.datatype.functional :as fun])

;; 2. Create a 1d array 0 to 9

(dtype/->reader (range 10))

;; 3. Create a boolean array

(require '[tech.v3.tensor :as tensor])

(tensor/compute-tensor [3 3] (fn [i r] true) :boolean)

;; 4. How to extract items that satisfy a given condition form a 1d array. Extract all oddd numbers from arr.


;; but this returns a lazy sequence, we don't have a buffer anymore
(->> (dtype/->reader (range 10) :int64)
     (keep #(when (odd? %) %)))

;; using argops
(require '[tech.v3.datatype.argops :as argops])

(let [rdr (dtype/->reader (range 10) :int32)
      indices (->> (dtype/->reader (range 10) :int64)
                   (argops/argfilter odd? {}))]
  (dtype/indexed-buffer indices rdr))



;; 5 & 6. How to replace items that satisfy a condition with another value.
;;    Replace all odd numbers with -1

(->> (dtype/->reader (range 10))
     (dtype/emap #(if (odd? %) -1 %) :int32))

;; 7. How to reshape an array

;; would be nice to auto determine either columns or rows
(tensor/reshape
 (dtype/->reader (range 10) :int32)
 [2 5])


;; 8. How to stack two arrays vertically.

;; skipping


;; 9. How to stack to arrays horizontally.

;; skipping

;; 10. How to generate custom sequences in numpy without hardcoding

;; skipping but you can do this with index selection? index-buffer?


;; 11. How to get the common items between two arrays? Get commong items between a and b.

;; no intersect?


;; 12. How to remove from one  array those items that exist in another?

;; setdiff in python 


;; 13. How to get the positions where elements of two arrays match?

;; np.where(a == b)

;; 14. How to extract all numbers between a given range from a numpy array?

;; index = np.where((a >= 5) & (a <= 10))
;; a[index]

;; 15. How to make a python function that handles scalars to work on numpy arrays?

;; 18. How to reverse rows of a 2d array?

(-> (dtype/->reader (range 9) :int32)
    (tensor/reshape [3 3])
    ;; ? there is tensor/rotate but not quite what we need, need a reversE?
    )

;; 19. How to reverse the columns of a 2d array?

;; same as above.


(set (dtype/->reader [1 1 2 2 3 3]))
