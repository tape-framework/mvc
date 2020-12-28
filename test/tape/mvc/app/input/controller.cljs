(ns tape.mvc.app.input.controller
  (:require [integrant.core :as ig]
            [reagent.core :as r]
            [reagent.ratom :as ratom :include-macros true]
            [re-frame.core :as rf]
            [tape.mvc :as mvc :include-macros true]))

(def ^{::mvc/reg ::mvc/routes} routes
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
  {::mvc/reg ::mvc/sub
   ::mvc/signals [signal]}
  [db _query] (::x db))

(defmethod ig/init-key ::sub-raw [_ _]
  ;; Workaround for: https://ask.clojure.org/index.php/8975
  (let [sub-raw
        ^{::mvc/reg ::mvc/sub-raw}
        (fn [_ _] (ratom/reaction (::x @db)))]
    sub-raw))

(defn subn
  {::mvc/reg ::mvc/sub
   ::mvc/id ::sub-named
   ::mvc/signals [signal]}
  [app-db _] (::x @app-db))

(defn event-db
  {::mvc/reg ::mvc/event-db
   ::mvc/interceptors [interceptor]}
  [_db [_ev-id _params]] {::x "x"})

(defn event-fx
  {::mvc/reg ::mvc/event-fx
   ::mvc/id ::event-fx-named
   ::mvc/interceptors [interceptor]}
  [_cofx [_ev-id _params]] {:db {::x "X"}})

;;; Module

(derive ::sub-raw ::mvc/sub-raw)

(mvc/defm ::module {::sub-raw nil})
