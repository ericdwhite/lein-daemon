(ns leiningen.daemon.daemonProxy
  "The entry point for services"
  (:import org.apache.commons.daemon.Daemon)
  (:refer-clojure :include [with-bindings])
  (:gen-class :name leiningen.daemon.daemonProxy
              :init ctor
              :state state
              :implements [org.apache.commons.daemon.Daemon]))

(defn -ctor []
  [[] (ref {:proxy-ns nil
            :daemon-controller nil})])

(defn -init [this context]
  (let [[ns-string & args] (seq (.getArguments context))
        ns-sym (symbol ns-string)
        null (require ns-sym)
        ns (find-ns ns-sym)]
    (println "init: ns = " ns)
    (dosync
     (alter (.state this) assoc :proxy-ns ns)
     (alter (.state this) assoc :controller (.getController context)))
    (println "proxy-ns = " (:proxy-ns @(.state this)))
    (if ns
      (if-let [init-fn (ns-resolve ns 'init)]
        (do
          (println "found init-fn, calling with " args)
          (clojure.main/with-bindings
            (apply init-fn args))))
      (.fail (.getController context (str "Clojure namespace " ns-sym " not found"))))))

(defn -start [this]
  (let [state @(.state this)
        start-fn (ns-resolve (:proxy-ns state) 'start)]
    (if start-fn
      (do
        (println "starting!")
        (clojure.main/with-bindings
          (start-fn)))
      (do
        (.fail (:controller @state) (str "fn start not found in namespace " (:proxy-ns state)))))))

(defn -stop [this]
  (let [state @(.state this)
        stop-fn (ns-resolve (:proxy-ns state) 'stop)]
    (if stop-fn
      (do
        (println "stopping!")
        (stop-fn))
      (println "stop function not found in ns " (:proxy-ns state) ", ignoring"))))

(defn -destroy [this]
  (let [state @(.state this)
        destroy-fn (resolve (:proxy-ns state) 'destroy)]
    (if destroy-fn
      (do
        (println "destroying!")
        (destroy-fn))
      (println "destroy function not found in ns " (:proxy-ns state) ", ignoring"))))

