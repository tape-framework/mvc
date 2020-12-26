(ns tape.mvc.app.basic.view
  (:require [integrant.core :as ig]
            [re-frame.core :as rf]
            [tape.mvc.view :as v :include-macros true]
            [tape.mvc.app.basic.controller :as baisc.c]))

;;; Views

(defn hello
  {::v/reg ::v/view}
  []
  (let [say @(rf/subscribe [::baisc.c/say])]
    [:p say]))

(defn goodbye
  {::v/reg ::v/view}
  []
  (let [shout @(rf/subscribe [::baisc.c/shout])]
    [:h1 shout]))

(defmethod ig/init-key ::extra [_ _]
  (let [extra
        ^{::v/controller-ns-str "tape.mvc.app.basic.controller"}
        (fn [] [:p "p"])]
    extra))

;;; Module

(derive ::extra ::v/view)

(v/defmodule {::extra nil})
