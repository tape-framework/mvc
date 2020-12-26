(ns tape.mvc.view-test
  (:require [cljs.test :refer [deftest is are]]
            [integrant.core :as ig]
            [re-frame.core :as rf]
            [tape.module :as module :include-macros true]
            [tape.mvc.view :as v :include-macros true]
            [tape.mvc.app.basic.controller :as basic.c]
            [tape.mvc.app.basic.view :as basic.v]))

(module/load-hierarchy)

;;; Ergonomics

(deftest dispatch-test
  (with-redefs [rf/dispatch identity]
    (is (= [::basic.c/event-db]
           (v/dispatch [basic.c/event-db])))))

(deftest subscribe-test
  (with-redefs [rf/subscribe identity]
    (is (= [::basic.c/sub]
           (v/subscribe [basic.c/sub])))))

;;; Module

(def ^:private views {::basic.c/event-db basic.v/hello})

(deftest derive-test
  (is (isa? ::basic.v/hello ::v/view)))

(deftest module-test
  (let [modulef (ig/init-key ::basic.v/module nil)
        conf    (modulef {})]
    (is (= #{::basic.v/hello
             ::basic.v/goodbye
             ::basic.v/extra}
           (set (keys conf))))
    (are [f mf] (= f (.-afn mf))
      basic.v/hello (::basic.v/hello conf)
      basic.v/goodbye (::basic.v/goodbye conf))))
