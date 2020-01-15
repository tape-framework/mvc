(ns tape.mvc.view
  (:require [cljs.analyzer.api :as api]
            [integrant.core :as ig]
            [tape.module :as module]
            [tape.mvc.meta :as meta]))

(defn- view-ns? [[k _]] (= "tape.mvc.view" (namespace k)))

(defmacro defmodule
  "Called at the end of view namespaces with a controller namespace argument as
  a symbol, derives the views according to their metadata declaration and
  declares a module that adds them to the system config map. Example:

  ```cljs
  (ns blog.app.greet.view
    (:require [re-frame.core :as rf]
              [tape.mvc.view :as v :include-macros true]
              [blog.app.greet.controller :as greet.c]))

  (defn ^::v/view hello []
    (let [say @(rf/subscribe [::greet.c/say])]
      [:p say]))

  (v/defmodule blog.app.greet.controller)
  ```

  The `defmodule` call above is equivalent to:

  ```cljs
  (derive ::hello ::v/view)

  (defmethod integrant.core/init-key ::module [_ _]
    (fn [config]
      (tape.module/merge-configs config {::hello hello})))
  ```
  "
  [controller-ns]
  {:pre [(symbol? controller-ns)]}
  (let [ns-str    (str *ns*)
        ns-sym    (symbol ns-str)
        ns-meta'  (meta/ns-meta view-ns? ns-sym)
        module    (keyword ns-str "module")
        var-infos (vals (api/ns-publics ns-sym))

        m         {::controller-ns-str (str controller-ns)}
        collect'  (partial meta/collect ns-meta' var-infos m)
        ->kw-fn'  (partial meta/->kw-fn ns-str)

        views     (collect' ::view ->kw-fn')
        viewsd    (map (meta/->derive ::view) views)]

    `(do ~@viewsd
         (defmethod ig/init-key ~module [_k# _v#]
           (fn [config#]
             (module/merge-configs config# ~views))))))