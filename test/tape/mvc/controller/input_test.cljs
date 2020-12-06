(ns tape.mvc.controller.input-test
  (:require [cljs.test :refer [deftest is are]]
            [integrant.core :as ig]
            [re-frame.registrar :as registrar]
            [tape.module :as module :include-macros true]
            [tape.mvc.controller :as c]
            [tape.mvc.view :as v]
            [tape.mvc.app.input.controller :as input.c]))

(module/load-hierarchy)

(deftest derive-test
  (are [x y] (isa? x y)
    ::input.c/sub ::c/sub
    ::input.c/subn ::c/sub
    ::input.c/event-db ::c/event-db
    ::input.c/event-fx ::c/event-fx))

(deftest module-test
  (let [f    (ig/init-key ::input.c/module nil)
        conf (f {})]
    (is (= #{::input.c/sub
             ::input.c/subn
             ::input.c/event-db
             ::input.c/event-fx}
           (set (keys conf))))
    (are [v mv] (= v mv)
      [[input.c/signal] input.c/sub] (::input.c/sub conf)
      [[input.c/signal] input.c/subn] (::input.c/subn conf)
      [[input.c/interceptor] input.c/event-db] (::input.c/event-db conf)
      [[input.c/interceptor] input.c/event-fx] (::input.c/event-fx conf))))

(def ^:private config
  {::c/module       nil
   ::v/module       nil
   ::input.c/module nil})

(deftest reg-test
  (let [system (-> config module/prep-config ig/init)]
    (are [kind id] (some? (registrar/get-handler kind id))
      :sub ::input.c/sub
      :sub ::input.c/sub-named
      :event ::input.c/event-db
      :event ::input.c/event-fx-named)
    (ig/halt! system)))
