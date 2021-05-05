(ns index-experiments.tmd-column-extension-demo
  (:require [tech.v3.dataset :as ds]
            [tech.v3.dataset.column :as ds-col]
            [tech.v3.dataset.column-index-structure :as col-idx]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.casting :as casting]
            [tech.v3.protocols.column :as col-proto]
            [tablecloth.api :as tablecloth]
            [geo
             [geohash :as geohash]
             [jts :as jts]
             [spatial :as spatial]
             [io :as geoio]
             [crs :as crs]]
            [notespace.api]))

(import (org.locationtech.jts.index.strtree STRtree)
        (org.locationtech.jts.geom Geometry Point Polygon Coordinate)
        (org.locationtech.jts.geom.prep PreparedGeometry
                                        PreparedLineString
                                        PreparedPolygon
                                        PreparedGeometryFactory)
        (java.util TreeMap))


;; Make sure tech.datatype knows about the datatype you are working with.
;; Which you test by calling tech.v3.datatype.casting/elemwise-datatype on the data.
(casting/add-object-datatype! :geometry Geometry true)

;; https://epsg.io/4326
(def wgs84-crs (crs/create-crs 4326))


;; https://epsg.io/6539
;; Center coordinates
;; 1252460.55256480 263192.27520000
;; Projected bounds:
;; 911908.37699881 110617.87078182
;; 1588713.88595833 420506.52709693
(def nad83-2011-crs (crs/create-crs 6539))

(def crs-transform
  (crs/create-transform wgs84-crs nad83-2011-crs))

(defn wgs84->nad83-2011
  "Transforming latitude-longitude coordinates
  to local Eucledian coordinates around NYC."
  [geometry]
  (jts/transform-geom geometry crs-transform))

(def get-neighbourhoods-data
  (memoize
   (fn []
     (-> "data/nyc-neighbourhoods.json"
         slurp
         geoio/read-geojson))))

(take 1 (get-neighbourhoods-data))

(defn polygon->latlngs
  "Convert a polygon to a sequence of longitude-latitude pairs, assuming it is in the appropriate coordinate reference system."
  [polygon]
  (->> polygon
       jts/coordinates
       (map (juxt spatial/latitude
                  spatial/longitude))))


(def get-neighbourhoods
  ;; (memoize)
  (fn []
    (-> (get-neighbourhoods-data)
        (->> (map (fn [{:keys [geometry properties]}]
                    (assoc properties :geometry (wgs84->nad83-2011 geometry)))))
        tablecloth/dataset
        (tablecloth/rename-columns {(keyword "@id") :id})
        (vary-meta assoc :print-column-max-width 100))))

(-> (get-neighbourhoods) 
    :geometry
    (meta))


^kind/dataset
(get-neighbourhoods)


(defmethod tech.v3.dataset.impl.column-index-structure/make-index-structure :geometry
  [data _]
  (println :geometry)
  (let [tree ^STRtree (STRtree.)]
    (doseq [[index-position geometry] (map-indexed vector data)]
      (.insert tree
               (.getEnvelopeInternal geometry)
               {:index-position index-position
                :prepared-geometry (PreparedGeometryFactory/prepare geometry)}))
    tree))

(-> (get-neighbourhoods)
    :geometry
    ds-col/index-structure
    ;; type
    )

(extend-type org.locationtech.jts.index.strtree.STRtree
  col-proto/PIndexStructure
  (select-from-index [index-structure mode selection-spec _]
    (case mode
      :intersect
      (let [{:keys [geometry]} selection-spec]
        (into []
              (comp (filter (fn [row]
                              (.intersects ^PreparedGeometry (:prepared-geometry row)
                                           geometry)))
                    (map :index-position))
              (.query ^STRtree index-structure
                      ^Envelope (.getEnvelopeInternal ^Point geometry)))))))

;; Which neighbourhoods are nearby Crown Heights ?
(let [neighbourhoods              (get-neighbourhoods)
      index-structure            (-> neighbourhoods
                                     :geometry
                                     (vary-meta assoc :categorical? false)
                                     ds-col/index-structure)
      crown-heights-geom              (-> (get-neighbourhoods)
                                     (tablecloth/select-rows #(-> % :neighborhood (= "Crown Heights")))
                                     :geometry
                                     first)
      around-crown-heights-geom (.buffer ^Geometry crown-heights-geom
                                         1000)
      row-numbers                (col-idx/select-from-index index-structure :intersect {:geometry around-crown-heights-geom})
      intersecting-neighbourhoods (-> neighbourhoods
                                      (tablecloth/select-rows row-numbers)
                                      :neighborhood)]
  intersecting-neighbourhoods)

