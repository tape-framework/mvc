(ns tape.mvc.app.named.controller
  (:require [tape.mvc.controller :as c :include-macros true]))

(defn sub
  {::c/reg ::c/sub
   ::c/id ::sub-named}
  [db _query] (::x db))

(defn sub-raw
  {::c/reg ::c/sub-raw
   ::c/id ::sub-raw-named}
  [app-db _] (::x @app-db))

(defn fx
  {::c/reg ::c/fx
   ::c/id ::fx-named}
  [_m] (do "x"))

(defn cofx
  {::c/reg ::c/cofx
   ::c/id ::cofx-named}
  [m] (assoc m ::x "x"))

(defn event-db
  {::c/reg ::c/event-db
   ::c/id ::event-db-named}
  [_db [_ev-id _params]] {::x "x"})

(defn event-fx
  {::c/reg ::c/event-fx
   ::c/id ::event-fx-named}
  [_cofx [_ev-id _params]] {:db {::x "X"}})

;; name clash test
(defn fx-named
  {::c/reg ::c/event-fx}
  [_ [_]] {})

(c/defmodule)
