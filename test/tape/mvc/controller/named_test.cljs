(ns tape.mvc.controller.named-test
  (:require [cljs.test :refer [deftest is are]]
            [integrant.core :as ig]
            [re-frame.registrar :as registrar]
            [tape.module :as module]
            [tape.mvc.controller :as c]
            [tape.mvc.view :as v]
            [tape.mvc.app.named.controller :as named.c]))

(module/load-hierarchy)

(deftest derive-test
  (are [x y] (isa? x y)
    ::named.c/sub-named ::c/sub
    ::named.c/sub-raw-named ::c/sub-raw
    ::named.c/fx-named ::c/fx
    ::named.c/cofx-named ::c/cofx
    ::named.c/event-db-named ::c/event-db
    ::named.c/event-fx-named ::c/event-fx))

(deftest module-test
  (let [f    (ig/init-key ::named.c/module nil)
        conf (f {})]
    (is (= #{::named.c/sub-named
             ::named.c/sub-raw-named
             ::named.c/fx-named
             ::named.c/cofx-named
             ::named.c/event-db-named
             ::named.c/event-fx-named}
           (set (keys conf))))
    (are [f mf] (= f (.-afn mf))
      named.c/sub (::named.c/sub-named conf)
      named.c/sub-raw (::named.c/sub-raw-named conf)
      named.c/fx (::named.c/fx-named conf)
      named.c/cofx (::named.c/cofx-named conf)
      named.c/event-db (::named.c/event-db-named conf)
      named.c/event-fx (::named.c/event-fx-named conf))))

(def ^:private config
  {::c/module       nil
   ::v/module       nil
   ::named.c/module nil})

(deftest reg-test
  (let [system (-> config module/prep-config ig/init)]
    (are [kind id] (some? (registrar/get-handler kind id))
      :sub ::named.c/sub-named
      :sub ::named.c/sub-raw-named
      :fx ::named.c/fx-named
      :cofx ::named.c/cofx-named
      :event ::named.c/event-db-named
      :event ::named.c/event-fx-named)
    (ig/halt! system)))
