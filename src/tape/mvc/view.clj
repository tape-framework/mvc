(ns tape.mvc.view
  (:require [cljs.analyzer.api :as api]
            [integrant.core :as ig]
            [tape.module :as module]
            [tape.mvc.meta :as meta]
            [clojure.string :as string]))

;;; Ergonomics

(defmacro dispatch
  "Like `re-frame.core/dispatch` but with  an IDE navigable symbol instead a
  keyword. When compiled it replaces the symbol with the keyword. Example:
  `(v/dispatch [counter.c/increment])
  ; => (rf/subscribe [::counter.c/increment])`."
  [[fsym & args]]
  `(re-frame.core/dispatch ~(into [(meta/event-kw &env fsym)] args)))

(defmacro subscribe
  "Like `re-frame.core/subscribe` but with  an IDE navigable symbol instead a
  keyword. When compiled it replaces the symbol with the keyword. Example:
  `(v/subscribe [counter.c/count]) ; => (rf/subscribe [::counter.c/count])`."
  [[fsym & args]]
  `(re-frame.core/subscribe ~(into [(meta/sub-kw &env fsym)] args)))

;;; Module

(defn- view-ns? [[k _]] (= "tape.mvc.view" (namespace k)))

(defn- controller-ns-str [view-ns-str]
  (assert (re-find #".view$" view-ns-str) "Namespace must end in \".view\"!")
  (string/replace view-ns-str #".view$" ".controller"))

(defmacro defmodule
  "Called at the end of view namespaces. Derives the views according to their
  metadata declaration and declares a module that adds them to the system
  config map. Example:

  ```clojure
  (ns blog.app.greet.view
    (:require [re-frame.core :as rf]
              [tape.mvc.view :as v :include-macros true]
              [blog.app.greet.controller :as greet.c]))

  (defn hello
    {::v/reg ::v/view}
    []
    (let [say @(rf/subscribe [::greet.c/say])]
      [:p say]))

  (v/defmodule) ;; blog.app.greet.controller ns must exist
  ```

  The `defmodule` call above is equivalent to:

  ```clojure
  (derive ::hello ::v/view)

  (defmethod integrant.core/init-key ::module [_ _]
    (fn [config]
      (tape.module/merge-configs config {::hello #'hello})))
  ```
  "
  []
  (let [ns-sym    (api/current-ns)
        ns-str    (str ns-sym)
        co-ns-str (controller-ns-str ns-str)

        ns-meta   (-> ns-sym api/find-ns :name meta)
        module    (keyword ns-str "module")
        var-infos (vals (api/ns-publics ns-sym))

        extra-meta {::controller-ns-str co-ns-str}
        collect   (partial meta/collect ns-meta var-infos extra-meta)

        views     (collect ::reg ::view)
        viewsd    (map (meta/->derive ::view) views)]

    `(do ~@viewsd
         (defmethod ig/init-key ~module [_k# _v#]
           (fn [config#]
             (module/merge-configs config# ~views))))))
