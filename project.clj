(defproject bsless/contextual "0.0.0-alpha"
  :description "Deferred evaluation with context"
  :url "https://github.com/bsless/contextual"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.trace "0.7.10"]]}}
  :repl-options {:init-ns contextual.core})
