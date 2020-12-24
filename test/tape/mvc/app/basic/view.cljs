(ns tape.mvc.app.basic.view
  (:require [re-frame.core :as rf]
            [tape.mvc.view :as v :include-macros true]
            [tape.mvc.app.basic.controller :as baisc.c]))

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

(v/defmodule)
