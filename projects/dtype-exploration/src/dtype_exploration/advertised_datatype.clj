(ns dtype-exploration.advertised-datatype
  (:require [tech.v3.datatype :as dtype]))

;; make-reader has a signature that includes a datatype and an "advertised" datatype

(def rdr-with-advertised-dtype (dtype/make-reader :int8 :int32 10 10))

(dtype/elemwise-datatype rdr-with-advertised-dtype);; => :int32

(dtype/make-container rdr-with-advertised-dtype)


(def rdr-no-advertised-dtype (dtype/make-reader :int8 10 10))

(dtype/elemwise-datatype rdr-no-advertised-dtype);; => :int32

(dtype/make-container rdr-no-advertised-dtype)
