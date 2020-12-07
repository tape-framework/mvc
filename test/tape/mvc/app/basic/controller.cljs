(ns tape.mvc.app.basic.controller
  (:require [tape.mvc.controller :as c :include-macros true]))

(def routes ["/" ::home])

(defn ^::c/sub sub [db _query] (::x db))
(defn ^::c/sub-raw sub-raw [app-db _] (::x @app-db))
(defn ^::c/fx fx [_m] (do "x"))
(defn ^::c/cofx cofx [m] (assoc m ::x "x"))
(defn ^::c/event-db event-db [_db [_ev-id _params]] {::x "x"})
(defn ^::c/event-fx event-fx [_cofx [_ev-id _params]] {:db {::x "X"}})

(c/defmodule)
