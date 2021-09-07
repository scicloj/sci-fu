(ns dtype-exploration.tensors
  (:require [tech.v3.datatype :as dtype]
            [tech.v3.tensor :as tensor]))


(dtype/datatype (dtype/->array-buffer [1 2 3]))


(def computed-buffer (dtype/make-reader :int8 10 10))
;; => #'dtype-exploration.tensors/computed-buffer

computed-buffer
;; => [10 10 10 10 10 10 10 10 10 10]

(type computed-buffer)
;; => dtype_exploration.tensors$fn$reify__43384

(dtype/datatype computed-buffer)
;; => :buffer

(def cloned-computed-buffer (dtype/clone computed-buffer))
;; => #'dtype-exploration.tensors/cloned-computed-buffer

(type cloned-computed-buffer)
;; => tech.v3.datatype.array_buffer.ArrayBuffer

(dtype/datatype cloned-computed-buffer)
;; => :array-buffer

cloned-computed-buffer


(def computed-tensor (tensor/compute-tensor [10 10]
                                            (fn [_ _] 10 10)
                                            :int8))
;; => #'dtype-exploration.tensors/computed-tensor

computed-tensor

(type (dtype/->buffer computed-tensor))

(type computed-tensor)
;; => tech.v3.tensor$compute_tensor$reify__17756

(dtype/datatype computed-tensor)
;; => :tensor

(tensor/tensor->buffer computed-tensor)
;; => nil

(def cloned-computed-tensor (tensor/clone computed-tensor))
;; => #'dtype-exploration.tensors/cloned-computed-tensor

(type cloned-computed-tensor)
;; => tech.v3.tensor.DirectTensor

(dtype/datatype cloned-computed-tensor)
;; => :tensor

cloned-computed-tensor

(tensor/tensor->buffer cloned-computed-tensor)
;; => #array-buffer<int8>[10]
;;    [10, 10, 10, 10, 10, 10, 10, 10, 10, 10]


