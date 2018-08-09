(defproject sieppari "0.0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]]
  :profiles {:dev {:dependencies [[eftest "0.5.2"]
                                  [metosin/testit "0.4.0-SNAPSHOT"]
                                  [criterium "0.4.4"]
                                  [metosin/ring-http-response "0.9.0"]]}}
  :plugins [[lein-eftest "0.5.2"]]
  :eftest {:multithread? false}
  :test-selectors {:default (constantly true)
                   :all (constantly true)}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"})
