(ns index-experiments.tmd-column-extension-demo
  (:require [tech.v3.dataset :as ds]
            [tech.v3.dataset.column :as ds-col]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.casting :as casting]
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
        ds/->dataset
        (tablecloth/rename-columns {(keyword "@id") :id})
        ;; (tablecloth/add-or-replace-column
        ;;  :geometry (ds/new-column :geometry
        ;;                           (tech.v3.dataset.impl.column/make)
        ;;                           #(map wgs84->nad83-2011 (:geometry %))))
        (vary-meta assoc :print-column-max-width 100))))

(-> (ds/new-column :foo [1 2 3])
    (meta))



^kind/dataset
(get-neighbourhoods)


(defmethod tech.v3.dataset.impl.column-index-structure/make-index-structure Geometry
  [data _]
  (let [tree ^STRtree (STRtree.)]
    (doseq [[index-position geometry] (map-indexed vector data)]
      (.insert tree
               (.getEnvelopeInternal geometry)
               {:index-position index-position
                :prepared-geometry (PreparedGeometryFactory/prepare geometry)}))
    tree))

(->
    (get-neighbourhoods)
    :geometry
    ds-col/index-structure
    )


