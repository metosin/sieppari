(defproject metosin/sieppari "0.0.0-alpha9"
  :description "Small, fast, and complete interceptor library."
  :url "https://github.com/metosin/sieppari"
  :license {:name "Eclipse Public License", :url "https://www.eclipse.org/legal/epl-2.0/"}
  :deploy-repositories [["releases" :clojars]]
  :lein-release {:deploy-via :clojars}

  :dependencies []

  :profiles {:dev-deps {:dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                                       [org.clojure/clojurescript "1.10.520"]
                                       ;; Add-ons:
                                       [org.clojure/core.async "1.1.587"]
                                       [manifold "0.1.8"]
                                       [funcool/promesa "5.1.0"]
                                       ;; Testing:
                                       [metosin/testit "0.4.0"]
                                       [lambdaisland/kaocha "1.0.629"]
                                       [lambdaisland/kaocha-cljs "0.0-71"]
                                       ;; Dev:
                                       [org.clojure/tools.namespace "1.0.0"]
                                       ;; Perf testing:
                                       [criterium "0.4.5"]
                                       [io.pedestal/pedestal.interceptor "0.5.7"]
                                       [org.slf4j/slf4j-nop "1.7.30"]]}
             :test-common [:dev-deps {:source-paths ["test/cljc"]}]
             :test-clj {:source-paths ["test/clj"]}
             :test-cljs {:source-paths ["test/cljs"]}
             :dev [:dev-deps {:source-paths ["dev" "test/cljc" "test/clj" "test/cljs"]}]
             :examples {:source-paths ["examples"]}
             :perf {:jvm-opts ^:replace ["-server" "-Xms4096m" "-Xmx4096m" "-Dclojure.compiler.direct-linking=true"]}}

  :aliases {"kaocha-clj" ["with-profile" "+test-common,+test-clj" "run" "-m" "kaocha.runner"]
            "kaocha-cljs" ["with-profile" "+test-common,+test-cljs" "run" "-m" "kaocha.runner"]
            "perf" ["with-profile" "default,dev,examples,perf"]
            "perf-test" ["perf" "run" "-m" "example.perf-testing"]})
