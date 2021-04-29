(defproject index-experiments "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :plugins [[reifyhealth/lein-git-down "0.4.0"]]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 ;; [techascent/tech.ml.dataset "5.13-SNAPSHOT"]
                 [techascent/tech.ml.dataset "6.00-beta-8-SNAPSHOT"]
                 [scicloj/tablecloth "5.11"]
                 [factual/geo "4566f37e2681113373350632087d2dbf8f145b96"]
                 [scicloj/notespace "3-beta5"]
                 [scicloj/tablecloth.time "1414cee38d634e62d34a9f0aebeca93f64dbdea9"]]
  :repositories [["public-github" {:url "git://github.com"}]]
  )
