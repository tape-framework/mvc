(ns tape.mvc.app.basic.view
  (:require [integrant.core :as ig]
            [re-frame.core :as rf]
            [tape.mvc :as mvc :include-macros true]
            [tape.mvc.app.basic.controller :as basic.c]))

;;; Views

(defn hello
  {::mvc/reg ::mvc/view}
  []
  (let [say @(rf/subscribe [::basic.c/say])]
    [:p say]))

(defn goodbye
  {::mvc/reg ::mvc/view}
  []
  (let [shout @(rf/subscribe [::basic.c/shout])]
    [:h1 shout]))

(defmethod ig/init-key ::extra [_ _]
  (let [extra
        ^{::mvc/controller-ns-str "tape.mvc.app.basic.controller"}
        (fn [] [:p "p"])]
    extra))

;;; Module

(derive ::extra ::mvc/view)

(mvc/defm ::module {::extra nil})
