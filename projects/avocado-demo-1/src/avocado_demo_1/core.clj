(ns avocado-demo-1.core
  (:require [tablecloth.api :as t]
            [tech.v3.dataset :as ds]
            [tech.v3.datatype :as dtype]
            [tech.v3.tensor :as dtt]
            [tech.v3.datatype.statistics :as stats]
            [tech.v3.datatype.rolling :as rolling]
            [clojure.java.io :as io]))

(def data
  (t/dataset "resources/data/avocado.csv.gz"
             {:key-fn keyword}))

