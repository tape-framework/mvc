(ns tape.mvc.app.basic.controller
  (:require [tape.mvc.controller :as c :include-macros true]))

(def ^{::c/reg ::c/routes} routes
  ["/" ::home
   "/event-db" ::event-db])

(defn sub
  {::c/reg ::c/sub}
  [db _query]
  (::x db))

(defn sub-raw
  {::c/reg ::c/sub-raw}
  [app-db _]
  (::x @app-db))

(defn fx
  {::c/reg ::c/fx}
  [_m]
  (do "x"))

(defn cofx
  {::c/reg ::c/cofx}
  [m]
  (assoc m ::x "x"))

(defn event-db
  {::c/reg ::c/event-db}
  [_db [_ev-id _params]]
  {::x "x"})

(defn event-fx
  {::c/reg ::c/event-fx}
  [_cofx [_ev-id _params]]
  {:db {::x "X"}})

(c/defmodule)
