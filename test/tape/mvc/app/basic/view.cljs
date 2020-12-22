(ns tape.mvc.app.basic.view
  (:require [re-frame.core :as rf]
            [tape.mvc.view :as v :include-macros true]
            [tape.mvc.app.basic.controller :as baisc.c]))

(defn ^::v/view hello []
  (let [say @(rf/subscribe [::baisc.c/say])]
    [:p say]))

(defn ^::v/view goodbye []
  (let [shout @(rf/subscribe [::baisc.c/shout])]
    [:h1 shout]))

(v/defmodule)
