(defproject metosin/sieppari "0.0.0-SNAPSHOT"
  :description "Small, fast, and complete interceptor library."
  :url "https://github.com/metosin/sieppari"
  :license {:name "Eclipse Public License", :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-parent "0.3.2"]]
  :parent-project {:path "../../project.clj", :inherit [:deploy-repositories :managed-dependencies]}
  :dependencies [[metosin/sieppari.core]
                 [metosin/sieppari.async.core-async]
                 [metosin/sieppari.async.deref]])
