(defproject bsless/contextual "0.0.0-alpha"
  :description "Deferred evaluation with context"
  :url "https://github.com/bsless/contextual"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :plugins [[lein-jmh "0.3.0"]]
  :profiles {:dev {:source-paths ["./dev"]
                   :dependencies [[org.clojure/tools.trace "0.7.10"]
                                  [criterium "0.4.6"]
                                  [jmh-clojure "0.4.0"]
                                  [com.clojure-goes-fast/clj-async-profiler "0.4.1"]
                                  [borkdude/sci "0.1.1-alpha.9"]]}}
  :repl-options {:init-ns contextual.core})
