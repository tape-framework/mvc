(ns tape.mvc
  "Modules discovery for less verbose config."
  (:require [cljs.analyzer.api :as api]
            [me.raynes.fs :as fs])
  (:import [java.io File]))

(defn- cljs? [f]
  (and (not (fs/directory? f))
       (re-matches #"(.*?)(\.cljs)$" (.getPath f))))

(defn- ls-r
  "Recursively list files under path."
  [path]
  (let [files (fs/list-dir path)]
    (concat (filter cljs? files)
            (->> files
                 (filter fs/directory?)
                 (mapcat ls-r)))))

(def ^:private src-count (count (.getPath fs/*cwd*)))

(defn- subtract-src [app-path path]
  (let [subtract-count (+ 2 src-count (.indexOf app-path File/separator))]
    (subs path subtract-count (count path))))

(defn- path-ns-str [path]
  (.. (.replaceAll (str path) "\\.cljs" "")
      (replace \_ \-)
      (replace \/ \.)))

(defn- view-or-controller? [s]
  (re-matches #"(.*?)(\.view|\.controller)$" s))

(defn- module? [s]
  ;; Marked by tape.mvc.meta/flag-ns-meta! called from defmodule
  (some-> s symbol api/find-ns :name meta :tape.mvc/module!))

(defn- controller? [s]
  (re-matches #"(.*?)(\.controller)$" s))

(defn- requires [modules]
  (map #(list 'require (list 'quote (symbol %))) modules))

(defn- views-and-controllers [app-path]
  (let [subtract-src' (partial subtract-src app-path)]
    (->> (ls-r app-path)
         (map (comp path-ns-str subtract-src' #(.getPath %)))
         (filter view-or-controller?))))

(defn- modules-map [modules]
  (->> (interleave (map #(keyword % "module") modules)
                   (repeat nil))
       (apply hash-map)))

(defn- routes? [controller]
  ;; asumes modules already required/loaded
  (get (api/ns-publics (symbol controller)) 'routes))

(defn- routes [modules]
  (->> modules
       (filter controller?)
       (filter routes?)
       (mapv #(symbol % "routes"))))

(defmacro require-modules [app-path]
  (let [files (views-and-controllers app-path)]
    `(do ~@(requires files))))

(defmacro modules-discovery [app-path]
  (let [modules (filter module? (views-and-controllers app-path))]
    `{:modules ~(modules-map modules)
      :routes  ~(routes modules)}))
