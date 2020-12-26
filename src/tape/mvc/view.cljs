(ns tape.mvc.view
  (:require [integrant.core :as ig]
            [tape.refmap :as refmap]
            [tape.module :as module]))

;;; Helpers

(defn- with-controller-kw
  "In the view registry, views are indexed by their coresponding controller
  keywords (event names). This changes the key of a reg pair from the view ns
  to the controller ns."
  [[view-kw view]]
  (let [controller-ns (-> view meta ::controller-ns-str)
        _ (assert (some? controller-ns))
        controller-kw (keyword controller-ns (name view-kw))]
    [controller-kw view]))

;;; Integrant

;; Views registry; keys are in the controller namespaces and values are Reagent
;; functions.
(defmethod ig/init-key ::views [_ views]
  (into {} (map with-controller-kw views)))

;;; Module

(defmethod ig/init-key ::module [_ _]
  (fn [config]
    (module/merge-configs config
                          {::views (refmap/refmap ::view)})))
