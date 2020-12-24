(ns tape.mvc.app.input.controller
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [tape.mvc.controller :as c :include-macros true]))

(def ^{::c/reg ::c/routes} routes
  ["/foo" ::foo])

;;; Input

(def interceptor
  (rf/enrich (fn add-test-data [db event]
               (assoc db ::test "test"))))

(defonce db (r/atom {}))

(defn signal
  ([_] db)
  ([_ _] db))

;;; Reg-ables

(defn sub
  {::c/reg ::c/sub
   ::c/signals [signal]}
  [db _query] (::x db))

(defn subn
  {::c/reg ::c/sub
   ::c/id ::sub-named
   ::c/signals [signal]}
  [app-db _] (::x @app-db))

(defn event-db
  {::c/reg ::c/event-db
   ::c/interceptors [interceptor]}
  [_db [_ev-id _params]] {::x "x"})

(defn event-fx
  {::c/reg ::c/event-fx
   ::c/id ::event-fx-named
   ::c/interceptors [interceptor]}
  [_cofx [_ev-id _params]] {:db {::x "X"}})

;;; Module

(c/defmodule)
