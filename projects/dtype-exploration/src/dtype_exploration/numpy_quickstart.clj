(ns dtype-exploration.numpy-quickstart
  (:require [notespace.api]
            [notespace.kinds :as kinds]))


(require '[tech.v3.datatype :as dtype]
         '[tech.v3.datatype.functional :as fun]
         '[tech.v3.tensor :as tensor])

["### Array creation"]

["Create a simple array of ints"]

;; a = np.array([2, 3, 4])
(def ary (dtype/->reader [2 3 4] :int8))

;; a.dtype 
(dtype/elemwise-datatype ary)

["No type specified, dtype next doesn't know what type the elements are"]
(-> [1 2 3]
    dtype/->reader
    dtype/elemwise-datatype)

["### Array creation with values"]

;; zp.zeros(10, dtype=int)
(dtype/emap (constantly 0) :int8 (range 10))

;; np.ones(2)
(dtype/emap (constantly 1) :int8 (range 10))

;; np.empty(3) => creates random elements that depend on state of memory
nil

;; >>> np.arange(10, 30, 5)
;; array([10, 15, 20, 25])
(dtype/emap identity :int8 (range 10 30 5))

;; >>> np.arange(0, 2, 0.3)  # it accepts float arguments
;; array([0. , 0.3, 0.6, 0.9, 1.2, 1.5, 1.8])
(dtype/emap identity :float32 (range 0 2 0.3))

;; >>> np.linspace(0, 2, 9)                   # 9 numbers from 0 to 2
;; array([0.  , 0.25, 0.5 , 0.75, 1.  , 1.25, 1.5 , 1.75, 2.  ])

;; built by @holyjak, see https://github.com/scicloj/scicloj-data-science-handbook/blob/live/src/scicloj/02_numpy.clj
(defn ^{:doc "Temporary workaround, maybe there is a better way to do this"}
  linspace
  ([start stop] (linspace start stop 50))
  ([start stop n]
   (let [delta (- stop start)
         end   (dec n)]
     (dtype/emap (fn [i] (/ (* i delta) end))
                 :float32
                 (range n)))))

(linspace 0 2 9)

["### Creating Tensors"]

(-> [[1 2 3]
     [4 5 6]]
    tensor/->tensor)

(-> [[:a :b :c]
     [:d :e :f]]
    tensor/->tensor)


["### Basic Operations"]

(def a (dtype/->reader [20 30 40 50]))

(def b (dtype/->reader (range 4)))

;; >>> a - b
;; array([20, 29, 38, 47])
(fun/- a b)

;; >>> b**2
;; array([0, 1, 4, 9])
(fun/pow b 2)

;; >>> 10 * np.sin(a)
;; array([ 9.12945251, -9.88031624,  7.4511316 , -2.62374854])
(-> a
    fun/sin
    (fun/* 10))

;; >>> a < 35
;; array([ True,  True, False, False])
(fun/< a 35)

(def A (tensor/->tensor [[1 1]
                         [0 1]]))

(def B (tensor/->tensor [[2 0]

                         [3 4]]))

;; >>> A * B     # elementwise product
;; array([[2, 0],
;;        [0, 4]])
(fun/* A B)


;; >>> A @ B     # matrix multipliation
;; array([[5, 4],
;;        [3, 4]])
nil ;; have coverage for this?

(def a-rand (dtype/->reader
             (vec (take 10 (repeatedly #(rand 1))))
             :float32))

a-rand

(fun/sum a-rand)

;; Why do these functions not operate on the whole buffer?
(apply fun/min a-rand)

(apply fun/max a-rand)

["##### Upcasting

When we have to arrays of different types, the type of the resulting array
will be the more precise type."]

(def a-ints (dtype/->reader [1 1 1] :int8))

a-ints

(dtype/elemwise-datatype a-ints)


(def b-floats (dtype/->reader (linspace 0 Math/PI 3)  :float32))
b-floats

(dtype/elemwise-datatype b-floats)

;; "upcasting" - the resulting column contains floats not ints, i.e. the more detailed type
(dtype/elemwise-datatype (fun/+ a-ints b-floats))


["Familiar Math functions"]

(-> [0 1 2]
    (dtype/->reader :int8)
    (fun/exp))

(-> [0 1 2]
    (dtype/->reader :int8)
    (fun/sqrt))

["#### Operations on tensor rows/columns"]

(def C (-> (range 12)
           (dtype/->reader :int8)
           (tensor/reshape [3 4])))

(tensor/reduce-axis fun/sum C 0)

(tensor/reduce-axis #(apply fun/min %) C 1)

;; erroring why?
;; (tensor/reduce-axis fun/cumsum C 1)

["### Indexing, Slicing, Iterating"]


(-> (range 10)
    (dtype/->reader :int8)
    (fun/pow 3)
    (nth 2))

