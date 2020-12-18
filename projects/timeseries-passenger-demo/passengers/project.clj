;; necessary bypass to allow an http repo
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject passengers "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [dev.nm/stat "1.2.0-SNAPSHOT"]
                 [scicloj/tablecloth "5.00-beta-5a"]
                 [scicloj/notespace "3-alpha3-SNAPSHOT"]
                 [generateme/fastmath "2.0.5"]
                 [tide "0.2.0-20170806.131424-3" :exclusions [org.apache.commons/commons-math3]]
                 [techascent/tech.viz "0.4.3"]
                 [aerial.hanami "0.12.4"]]
  :repositories [["nm-repo" "http://repo.nm.dev/maven/"]])