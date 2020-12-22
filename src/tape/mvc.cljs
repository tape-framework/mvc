(ns tape.mvc
  (:require [integrant.core :as ig]
            [tape.mvc.controller :as c]
            [tape.mvc.view :as v]
            [tape.module :as module]))

;;; Module

(defmethod ig/prep-key ::module [_ config]
  (assoc config ::requires {:controller (ig/ref ::c/module)
                            :view       (ig/ref ::v/module)}))

(defmethod ig/init-key ::module [_ _]
  (fn [config]
    (module/merge-configs
     config {::main {:subs        (ig/ref ::c/subs)
                     :subs-raw    (ig/ref ::c/subs-raw)
                     :fxs         (ig/ref ::c/fxs)
                     :cofxs       (ig/ref ::c/cofxs)
                     :events-fx   (ig/ref ::c/events-fx)
                     :events-db   (ig/ref ::c/events-db)

                     :views       (ig/ref ::v/views)
                     :interceptor (ig/ref ::v/interceptor)
                     :current     (ig/ref ::v/current)
                     :current-fn  (ig/ref ::v/current-fn)}})))
