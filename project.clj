(defproject metosin/sieppari "0.0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 ; Optional dependency for generating graphs from interceptors
                 [ubergraph "0.5.1" :scope "provided"]]
  :profiles {:dev {:source-paths ["examples"]
                   :dependencies [[eftest "0.5.2"]
                                  [metosin/testit "0.4.0-SNAPSHOT"]
                                  [criterium "0.4.4"]
                                  [metosin/ring-http-response "0.9.0"]
                                  [org.slf4j/slf4j-nop "1.7.25"]
                                  [io.pedestal/pedestal.interceptor "0.5.4"]
                                  [org.clojure/core.async "0.4.474"]]}
             :perf {:jvm-opts ^:replace ["-server"
                                         "-Xmx4096m"
                                         "-Dclojure.compiler.direct-linking=true"]}}
  :plugins [[lein-eftest "0.5.2"]]
  :eftest {:multithread? false}
  :test-selectors {:default (constantly true)
                   :all (constantly true)}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :aliases {"perf" ["with-profile" "default,dev,perf"]})
