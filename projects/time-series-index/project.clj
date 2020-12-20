(defproject time-series-index "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [scicloj/tablecloth "5.00-beta-21"]
                 [scicloj/notespace "3-alpha3-SNAPSHOT"]
                 [aerial.hanami "0.12.4"]]
  :repl-options {:init-ns time-series-index.core})
