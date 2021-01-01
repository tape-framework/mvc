(ns tape.mvc-test
  (:require [clojure.set :as set]
            [cljs.test :refer [deftest testing is are]]
            [integrant.core :as ig]
            [re-frame.core :as rf]
            [re-frame.registrar :as registrar]
            [tape.module :as module :include-macros true]
            [tape.mvc :as mvc :include-macros true]
            [tape.mvc.app.basic.controller :as basic.c]
            [tape.mvc.app.basic.view :as basic.v]
            [tape.mvc.app.input.controller :as input.c]
            [tape.mvc.app.named.controller :as named.c]))

(module/load-hierarchy)

;;; Ergonomic API

(deftest dispatch-test
  (with-redefs [rf/dispatch identity]
    (is (= [::basic.c/event-db]
           (mvc/dispatch [basic.c/event-db])))))

(deftest subscribe-test
  (with-redefs [rf/subscribe identity]
    (is (= [::basic.c/sub]
           (mvc/subscribe [basic.c/sub])))))

;;; Config

(def ^:private config
  {::mvc/module nil
   ::basic.c/module nil
   ::basic.v/module nil
   ::input.c/module nil
   ::named.c/module nil})

;;; Controller

(deftest derive-test
  (testing "basic"
    (are [x y] (isa? x y)
      ::basic.c/routes ::mvc/routes
      ::basic.c/sub ::mvc/sub
      ::basic.c/sub-raw ::mvc/sub-raw
      ::basic.c/fx ::mvc/fx
      ::basic.c/cofx ::mvc/cofx
      ::basic.c/event-db ::mvc/event-db
      ::basic.c/event-fx ::mvc/event-fx))
  (testing "input"
    (are [x y] (isa? x y)
      ::input.c/routes ::mvc/routes
      ::input.c/sub ::mvc/sub
      ::input.c/sub-raw ::mvc/sub-raw
      ::input.c/subn ::mvc/sub
      ::input.c/event-db ::mvc/event-db
      ::input.c/event-fx ::mvc/event-fx))
  (testing "named"
    (are [x y] (isa? x y)
      ::named.c/sub ::mvc/sub
      ::named.c/sub-raw ::mvc/sub-raw
      ::named.c/fx ::mvc/fx
      ::named.c/cofx ::mvc/cofx
      ::named.c/event-db ::mvc/event-db
      ::named.c/event-fx ::mvc/event-fx
      ::named.c/fx-named ::mvc/event-fx)))

(deftest module-test
  (let [system (-> config
                   module/prep-config
                   (ig/init [:tape.mvc/main :tape/multi]))
        keys-set (set (keys system))]

    (testing "keys"
      (testing "mvc"
        (is (set/subset?
             #{::mvc/subs
               ::mvc/subs-raw
               ::mvc/fxs
               ::mvc/cofxs
               ::mvc/events-db
               ::mvc/events-fx}
             keys-set)))
      (testing "basic"
        (is (set/subset?
             #{::basic.c/routes
               ::basic.c/sub
               ::basic.c/sub-raw
               ::basic.c/fx
               ::basic.c/cofx
               ::basic.c/event-db
               ::basic.c/event-fx}
             keys-set)))
      (testing "input"
        (is (set/subset?
             #{::input.c/routes
               ::input.c/sub
               ::input.c/sub-raw
               ::input.c/subn
               ::input.c/event-db
               ::input.c/event-fx}
             keys-set)))
      (testing "named"
        (is (set/subset?
             #{::named.c/sub
               ::named.c/sub-raw
               ::named.c/fx
               ::named.c/cofx
               ::named.c/event-db
               ::named.c/event-fx
               ::named.c/fx-named}
             keys-set))))

    (testing "values"
      (testing "basic"
        (is (= basic.c/routes (::basic.c/routes system)))
        (are [f k] (= f (.-afn (k system)))
          basic.c/sub ::basic.c/sub
          basic.c/sub-raw ::basic.c/sub-raw
          basic.c/fx ::basic.c/fx
          basic.c/cofx ::basic.c/cofx
          basic.c/event-db ::basic.c/event-db
          basic.c/event-fx ::basic.c/event-fx))
      (testing "input"
        (are [v k] (= v [(-> system k meta ::mvc/signals) (.-afn (k system))])
          [[input.c/signal] input.c/sub] ::input.c/sub
          [[input.c/signal] input.c/subn] ::input.c/subn)
        (are [v k] (= v [(-> system k meta ::mvc/interceptors) (.-afn (k system))])
          [[input.c/interceptor] input.c/event-db] ::input.c/event-db
          [[input.c/interceptor] input.c/event-fx] ::input.c/event-fx))
      (testing "named"
        (are [f k] (= f (.-afn (k system)))
          named.c/sub ::named.c/sub
          named.c/sub-raw ::named.c/sub-raw
          named.c/fx ::named.c/fx
          named.c/cofx ::named.c/cofx
          named.c/event-db ::named.c/event-db
          named.c/event-fx ::named.c/event-fx
          named.c/fx-named ::named.c/fx-named)))

    (testing "reg"
      (testing "basic"
        (are [kind id] (some? (registrar/get-handler kind id))
          :sub ::basic.c/sub
          :sub ::basic.c/sub-raw
          :fx ::basic.c/fx
          :cofx ::basic.c/cofx
          :event ::basic.c/event-db
          :event ::basic.c/event-fx))
      (testing "input"
        (are [kind id] (some? (registrar/get-handler kind id))
          :sub ::input.c/sub
          :sub ::input.c/sub-raw
          :sub ::input.c/sub-named
          :event ::input.c/event-db
          :event ::input.c/event-fx-named))
      (testing "named"
        (are [kind id] (some? (registrar/get-handler kind id))
          :sub ::named.c/sub-named
          :sub ::named.c/sub-raw-named
          :fx ::named.c/fx-named
          :cofx ::named.c/cofx-named
          :event ::named.c/event-db-named
          :event ::named.c/event-fx-named
          :event ::named.c/fx-named)))

    (ig/halt! system)))

;;; View

(def ^:private views {::basic.c/event-db basic.v/hello})

(deftest view-derive-test
  (is (isa? ::basic.v/hello ::mvc/view)))

(deftest view-module-test
  (let [modulef (ig/init-key ::basic.v/module nil)
        conf (modulef {})]
    (is (= #{::basic.v/hello
             ::basic.v/goodbye
             ::basic.v/extra}
           (set (keys conf))))
    (are [f mf] (= f (.-afn mf))
      basic.v/hello (::basic.v/hello conf)
      basic.v/goodbye (::basic.v/goodbye conf))))

;;; Modules discovery

(deftest require-modules-test
  (is (= '(do (require 'tape.mvc.app.basic.view)
              (require 'tape.mvc.app.basic.controller)
              (require 'tape.mvc.app.named.controller)
              (require 'tape.mvc.app.input.controller))
         (macroexpand '(mvc/require-modules "test/tape/mvc/app/")))))

(mvc/require-modules "test/tape/mvc/app/")

(deftest modules-discovery-test
  (is (= {:tape.mvc.app.named.controller/module nil
          :tape.mvc.app.input.controller/module nil
          :tape.mvc.app.basic.controller/module nil
          :tape.mvc.app.basic.view/module nil}
         (mvc/modules-map "test/tape/mvc/app/"))))
