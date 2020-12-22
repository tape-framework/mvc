(ns tape.mvc
  "Modules discovery for less verbose config."
  (:require [me.raynes.fs :as fs])
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
  (.. (.replaceAll (str path) "\\.clj(s|c)" "")
      (replace \_ \-)
      (replace \/ \.)))

(defn- view-or-controller? [s]
  (re-matches #"(.*?)(view|controller)$" s))

(defn- controller? [s]
  (re-matches #"(.*?)(controller)$" s))

(defn- requires [modules]
  (map #(list 'require (list 'quote (symbol %))) modules))

(defn- views-and-controllers [app-path]
  (let [subtract-src' (partial subtract-src app-path)]
    (->> (ls-r app-path)
         (map (comp path-ns-str subtract-src' #(.getPath %)))
         (filter view-or-controller?))))

(defmacro require-modules
  "Discover modules under `app-path` and require them. Use form at top level.
  Example: `(mvc/require-modules \"src/blog/app\")`."
  [app-path]
  (let [files (views-and-controllers app-path)]
    `(do ~@(requires files))))

(defmacro modules-map
  "Returns a map with modules discovered under `app-path` to be merged in
  modules config map. Example: `(mvc/modules-map \"src/blog/app\")`."
  [app-path]
  (let [modules (views-and-controllers app-path)
        modules-kw (map #(keyword % "module") modules)]
    (apply hash-map (interleave modules-kw (repeat nil)))))
