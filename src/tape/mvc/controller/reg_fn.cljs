(ns tape.mvc.controller.reg-fn
  "Bearing some similarity to Re-Frame's subscriptions, this allows to register
  functions that can later be called from views via a subscribe'ish mechanism.
  This is useful when function calls require context from the system map."
  (:require [re-frame.loggers :as l]))

(def id->handler (atom {}))

(defn reg-fn!
  "Register the function `handler` under the id keyword."
  [id handler]
  (swap! id->handler assoc id handler))

(defn subscribe
  "Subscribe to a function registered with [[reg-fn!]]."
  [[id & params]]
  (if-let [handler (get @id->handler id)]
    (apply handler params)
    (l/console :error (str ": no reg-fn handler registered for: " id "."))))
