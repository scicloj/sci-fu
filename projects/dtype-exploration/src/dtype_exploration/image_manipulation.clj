(ns dtype-exploration.image-manipulation
  (:require [notespace.api :as notespace]
            [notespace.kinds :as kinds]
            [clojure.java.io :as io]))

;; Based on:
;; https://realpython.com/numpy-tutorial/#practical-example-2-manipulating-images-with-matplotlib

(require '[clojure.java.io :as io]
         '[tech.v3.datatype :as dtype]
         '[tech.v3.datatype.functional :as fun]
         '[tech.v3.tensor :as tensor]
         '[tech.v3.libs.buffered-image :as bufimg])

(def our-image
  (-> "https://picsum.photos/id/1015/800"
      (bufimg/load)))

;; (dtype/shape img-tensor)

(defn show-image [image-buffer]
  (let [filename (str "img_" (str (rand-int 99999)) ".jpg")]
    (->> filename
         notespace/file-target-path
         (bufimg/save! image-buffer))
    (notespace/img-file-tag filename {:width 400})))

(show-image our-image)

(bufimg/image-channel-format our-image)

(def our-image-tensor (tensor/ensure-tensor our-image))

(dtype/elemwise-datatype our-image-tensor)

(def our-image-shape (dtype/shape our-image-tensor))

(def blue-image
  (let [new-img (bufimg/new-image (first our-image-shape)
                                  (second our-image-shape)
                                  :byte-bgr)
        computed-tensor (tensor/compute-tensor (dtype/shape our-image-tensor)
                                               (fn [i j k] (if (= k 0)
                                                             (our-image-tensor i j k)
                                                             0))
                                               :int32)]
    (dtype/copy! computed-tensor (tensor/as-tensor new-img))
    new-img))

(show-image blue-image)

(def gray-image-mean
  (let [new-img         (bufimg/new-image (first our-image-shape)
                                          (second our-image-shape)
                                          :byte-bgr)
        means           (tensor/reduce-axis fun/mean our-image-tensor 2) 
        computed-tensor (tensor/compute-tensor (dtype/shape our-image-tensor)
                                               (fn [i j k] (means i j))
                                               :int32)]
    (dtype/copy! computed-tensor (tensor/as-tensor new-img))
    new-img))

(show-image gray-image-mean)

(def gray-image-luminosity-method
  (let [new-img           (bufimg/new-image (first our-image-shape) 
                                            (second our-image-shape)
                                            :byte-bgr)
        dot-products    (tensor/reduce-axis (partial fun/dot-product
                                                     [0.3 0.59 0.11])
                                            our-image-tensor 2) 
          computed-tensor (tensor/compute-tensor (dtype/shape our-image-tensor)
                                                 (fn [i j k] (dot-products i j))
                                                 :int32)]
      (dtype/copy! computed-tensor (tensor/as-tensor new-img))
      new-img))


(show-image gray-image-luminosity-method)
