(ns tape.mvc.app.basic.controller
  (:require [tape.mvc :as mvc :include-macros true]))

(def ^{::mvc/reg ::mvc/routes} routes
  ["/" ::home
   "/event-db" ::event-db])

(defn sub
  {::mvc/reg ::mvc/sub}
  [db _query]
  (::x db))

(defn sub-raw
  {::mvc/reg ::mvc/sub-raw}
  [app-db _]
  (::x @app-db))

(defn fx
  {::mvc/reg ::mvc/fx}
  [_m]
  (do "x"))

(defn cofx
  {::mvc/reg ::mvc/cofx}
  [m]
  (assoc m ::x "x"))

(defn event-db
  {::mvc/reg ::mvc/event-db}
  [_db [_ev-id _params]]
  {::x "x"})

(defn event-fx
  {::mvc/reg ::mvc/event-fx}
  [_cofx [_ev-id _params]]
  {:db {::x "X"}})

(mvc/defm ::module)
