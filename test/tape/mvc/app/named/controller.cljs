(ns tape.mvc.app.named.controller
  (:require [tape.mvc :as mvc :include-macros true]))

(defn sub
  {::mvc/reg ::mvc/sub
   ::mvc/id ::sub-named}
  [db _query] (::x db))

(defn sub-raw
  {::mvc/reg ::mvc/sub-raw
   ::mvc/id ::sub-raw-named}
  [app-db _] (::x @app-db))

(defn fx
  {::mvc/reg ::mvc/fx
   ::mvc/id ::fx-named}
  [_m] (do "x"))

(defn cofx
  {::mvc/reg ::mvc/cofx
   ::mvc/id ::cofx-named}
  [m] (assoc m ::x "x"))

(defn event-db
  {::mvc/reg ::mvc/event-db
   ::mvc/id ::event-db-named}
  [_db [_ev-id _params]] {::x "x"})

(defn event-fx
  {::mvc/reg ::mvc/event-fx
   ::mvc/id ::event-fx-named}
  [_cofx [_ev-id _params]] {:db {::x "X"}})

;; name clash test
(defn fx-named
  {::mvc/reg ::mvc/event-fx}
  [_ [_]] {})

(mvc/defm ::module)
