(ns tape.mvc.view-test
  (:require [cljs.test :refer [deftest is are]]
            [integrant.core :as ig]
            [re-frame.core :as rf]
            [tape.module :as module :include-macros true]
            [tape.mvc.view :as v]
            [tape.mvc.app.basic.controller :as basic.c]
            [tape.mvc.app.basic.view :as basic.v]))

(module/load-hierarchy)

(def ^:private views {::basic.c/event-db basic.v/hello})

(deftest interceptor-test
  (let [interceptor (ig/init-key ::v/interceptor views)
        afterf      (:after interceptor)
        context     {:coeffects {:event [::basic.c/event-db]}
                     :effects   {:db {}}}
        context2    (assoc-in context [:coeffects :event 0] ::basic.c/no-view)]
    (is (= ::basic.c/event-db (-> context afterf (rf/get-effect :db) ::v/current)))
    (is (nil? (-> context2 afterf (rf/get-effect :db) ::v/current)))))

(deftest current-fn-test
  (let [current-fn (ig/init-key ::v/current-fn views)
        db         {::v/current ::basic.c/event-db}
        db2        {}]
    (is (= basic.v/hello (current-fn db nil)))
    (is (= nil (current-fn db2 nil)))))

(deftest derive-test
  (is (isa? ::basic.v/hello ::v/view)))

(deftest module-test
  (let [modulef (ig/init-key ::basic.v/module nil)
        conf    (modulef {})]
    (is (= #{::basic.v/hello
             ::basic.v/goodbye}
           (set (keys conf))))
    (are [f mf] (= f (.-afn mf))
      basic.v/hello (::basic.v/hello conf)
      basic.v/goodbye (::basic.v/goodbye conf))))
