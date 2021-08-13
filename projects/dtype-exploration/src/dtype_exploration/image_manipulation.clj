(ns dtype-exploration.image-manipulation
  (:require [notespace.api]
            [notespace.kinds :as kinds]))

(require '[clojure.java.io :as io]
         '[tech.v3.datatype :as dtype]
         '[tech.v3.libs.buffered-image :as bufimg])

;; (slurp "./resources/public/kitty.jpg")
;; (def gradient-tens (delay (-> (io/resource "kitty.jpg")
;;                               #_(bufimg/load)
;;                               #_(dtt/ensure-tensor))))

