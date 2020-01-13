(ns tape.mvc.app.named.controller
  (:require [tape.mvc.controller :as c :include-macros true]))

(defn ^{::c/sub ::sub-named} sub [db _query] (::x db))
(defn ^{::c/sub-raw ::sub-raw-named} sub-raw [app-db _] (::x @app-db))
(defn ^{::c/fx ::fx-named} fx [_m] (do "x"))
(defn ^{::c/cofx ::cofx-named} cofx [m] (assoc m ::x "x"))
(defn ^{::c/event-db ::event-db-named} event-db [_db [_ev-id _params]] {::x "x"})
(defn ^{::c/event-fx ::event-fx-named} event-fx [_cofx [_ev-id _params]] {:db {::x "X"}})

(c/defmodule)
