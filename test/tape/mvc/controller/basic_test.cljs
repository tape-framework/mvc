(ns tape.mvc.controller.basic-test
  (:require [cljs.test :refer [deftest is are]]
            [integrant.core :as ig]
            [re-frame.registrar :as registrar]
            [tape.module :as module :include-macros true]
            [tape.mvc.controller :as c]
            [tape.mvc.view :as v]
            [tape.mvc :as mvc]
            [tape.mvc.app.basic.controller :as basic.c]
            [tape.mvc.app.basic.view :as basic.v]
            [clojure.set :as set]))

(module/load-hierarchy)

(deftest derive-test
  (are [x y] (isa? x y)
    ::basic.c/routes ::c/routes
    ::basic.c/sub ::c/sub
    ::basic.c/sub-raw ::c/sub-raw
    ::basic.c/fx ::c/fx
    ::basic.c/cofx ::c/cofx
    ::basic.c/event-db ::c/event-db
    ::basic.c/event-fx ::c/event-fx))

(deftest module-test
  (let [f    (ig/init-key ::basic.c/module nil)
        conf (f {})]
    (is (= #{::basic.c/routes
             ::basic.c/sub
             ::basic.c/sub-raw
             ::basic.c/fx
             ::basic.c/cofx
             ::basic.c/event-db
             ::basic.c/event-fx}
           (set (keys conf))))
    (is (= basic.c/routes (::basic.c/routes conf)))
    (are [f mf] (= f (.-afn mf))
      basic.c/sub (::basic.c/sub conf)
      basic.c/sub-raw (::basic.c/sub-raw conf)
      basic.c/fx (::basic.c/fx conf)
      basic.c/cofx (::basic.c/cofx conf)
      basic.c/event-db (::basic.c/event-db conf)
      basic.c/event-fx (::basic.c/event-fx conf))))

(derive ::context :tape/const)

(def ^:private config
  {::c/module         nil
   ::v/module         nil
   ::mvc/module       nil
   ::basic.c/module   nil
   ::basic.v/module   nil})

(deftest reg-test
  (let [system (-> config module/prep-config (ig/init [:tape.mvc/main :tape/multi]))]
    (is (set/subset? #{::c/subs
                       ::c/subs-raw
                       ::c/fxs
                       ::c/cofxs
                       ::c/events-db
                       ::c/events-fx}
                     (set (keys system))))
    (are [kind id] (some? (registrar/get-handler kind id))
      :sub ::basic.c/sub
      :sub ::basic.c/sub-raw
      :fx ::basic.c/fx
      :cofx ::basic.c/cofx
      :event ::basic.c/event-db
      :event ::basic.c/event-fx)
    (ig/halt! system)))
