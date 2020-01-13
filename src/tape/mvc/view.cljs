(ns tape.mvc.view
  (:require [integrant.core :as ig]
            [re-frame.core :as rf]
            [tape.refmap :as refmap]
            [tape.module :as module]
            [tape.mvc.controller :as c]))

;;; Subs

(defn ^::c/sub current [db _] (::current db))

;;; Helpers

(defn- qualify [[k v]]
  (let [nsp (-> v meta ::controller-ns-str)
        kw  (keyword nsp (name k))]
    [kw v]))

;;; Integrant

;; Views registry; keys are in the controller namespace and values are Reagent
;; functions.
(defmethod ig/init-key ::views [_ views]
  (into {} (map qualify views)))

;; Re-Frame subscription yielding the Reagent function set as `::current` in
;; app-db.
(defmethod ig/init-key ::current-fn [_ views]
  (fn current-fn-sub [db _] (get views (current db nil))))

;; An interceptor that is added in the middleware stack of Tape Re-Frame
;; event handlers that have a view with the same name in the corresponding
;; views namespace; this interceptor sets the `::current` view automatically
;; if it's not already present in app-db.
(defmethod ig/init-key ::interceptor [_ views]
  (rf/enrich (fn add-current [db event]
               (let [event-id (first event)
                     current  (::current db)
                     add?     (and (nil? current) (contains? views event-id))]
                 (cond-> db
                         add? (assoc ::current event-id))))))

;;; Module

(defmethod ig/prep-key ::module [_ config]
  (assoc config ::requires {:controller (ig/ref ::c/module)}))

(defmethod ig/init-key ::module [_ _]
  (fn [config]
    (module/merge-configs config
                          {::views       (refmap/refmap ::view)
                           ::interceptor (ig/ref ::views)
                           ::current     current
                           ::current-fn  (ig/ref ::views)})))
