(defproject metosin/sieppari "0.0.0-SNAPSHOT"
  :dependencies []
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                                  ;; Dev:
                                  [org.clojure/tools.namespace "0.2.11"]
                                  ;; Testing:
                                  [eftest "0.5.2"]
                                  [metosin/testit "0.4.0-SNAPSHOT"]
                                  ;; Perf testing:
                                  [criterium "0.4.4"]
                                  [io.pedestal/pedestal.interceptor "0.5.4"]
                                  [org.slf4j/slf4j-nop "1.7.25"]
                                  ;; async testing:
                                  [org.clojure/core.async "0.4.474"]]
                   :source-paths ["dev"]}
             :examples {:source-paths ["examples"]}
             :perf {:jvm-opts ^:replace ["-server"
                                         "-Xms4096m"
                                         "-Xmx4096m"
                                         "-Dclojure.compiler.direct-linking=true"]}}
  :plugins [[lein-eftest "0.5.2"]]
  :eftest {:multithread? false}
  :test-selectors {:default (constantly true)
                   :all (constantly true)}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :aliases {"perf" ["with-profile" "default,dev,examples,perf"]
            "perf-test" ["perf" "run" "-m" "example.perf-testing"]})
