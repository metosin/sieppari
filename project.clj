(defproject metosin/sieppari "0.0.0-alpha7"
  :description "Small, fast, and complete interceptor library."
  :url "https://github.com/metosin/sieppari"
  :license {:name "Eclipse Public License", :url "https://www.eclipse.org/legal/epl-2.0/"}
  :deploy-repositories [["releases" :clojars]]
  :lein-release {:deploy-via :clojars}

  :dependencies []

  :profiles {:dev {:source-paths ["dev" "test/clj" "test/cljc"]
                   :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                                  ;; Add-ons:
                                  [org.clojure/core.async "0.4.490"]
                                  [manifold "0.1.8"]
                                  [funcool/promesa "2.0.0-SNAPSHOT"]
                                  ;; Dev:
                                  [org.clojure/tools.namespace "0.2.11"]
                                  ;; Testing:
                                  [eftest "0.5.4"]
                                  [metosin/testit "0.4.0-SNAPSHOT"]
                                  ;; Perf testing:
                                  [criterium "0.4.4"]
                                  [io.pedestal/pedestal.interceptor "0.5.5"]
                                  [org.slf4j/slf4j-nop "1.7.25"]]}
             :examples {:source-paths ["examples"]}
             :perf {:jvm-opts ^:replace ["-server" "-Xms4096m" "-Xmx4096m" "-Dclojure.compiler.direct-linking=true"]}}

  :plugins [[lein-eftest "0.5.4"]]
  :eftest {:multithread? false}
  :test-selectors {:default (constantly true)
                   :all (constantly true)}

  :aliases {"perf" ["with-profile" "default,dev,examples,perf"]
            "perf-test" ["perf" "run" "-m" "example.perf-testing"]})
