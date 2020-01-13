(ns tape.mvc.controller
  "Re-frame handlers registry."
  (:require [integrant.core :as ig]
            [re-frame.core :as rf]
            [tape.refmap :as refmap]
            [tape.module :as module]
            [tape.mvc.controller.reg-fn :as reg-fn]))

;;; Helpers

(defn- kval-ex [k kval]
  (ex-info (str k " val must be true or a qualified keyword, but is " kval)
           {k kval}))

(defn- get-id [kind id handler]
  (let [kval (some-> handler meta kind)]
    (cond
      (qualified-keyword? kval) kval
      (true? kval) id
      :else (kval-ex kind kval))))

;;; Reg-Fn

;; Naming `reg-afn` because: "Namespace c.reg-fn clashes with var c/reg-fn".
(defn- reg-afn [[id handler]]
  (let [id' (get-id ::reg-fn id handler)]
    (reg-fn/reg-fn! id' handler)))

(defmethod ig/init-key ::reg-fns [_ reg-fns]
  (run! reg-afn reg-fns)
  reg-fns)

;;; Subscriptions

(defn- reg-sub [[id handler-or-args]]
  (let [id' (get-id ::sub id handler-or-args)]
    (cond
      (fn? handler-or-args) (rf/reg-sub id' handler-or-args)
      (coll? handler-or-args) (apply rf/reg-sub id' handler-or-args)
      :else (throw (ex-info "Bad argument" {})))))

(defn- clear-sub [[id _]] (rf/clear-sub id))

(defmethod ig/init-key ::subs [_ subs]
  (run! reg-sub subs)
  subs)

(defmethod ig/halt-key! ::subs [_ subs]
  (run! clear-sub subs)
  (rf/clear-subscription-cache!)
  subs)

;;; Raw Subscriptions

(defn- reg-sub-raw [[id handler]]
  (let [id' (get-id ::sub-raw id handler)]
    (rf/reg-sub-raw id' handler)))

(defmethod ig/init-key ::subs-raw [_ subs-raw]
  (run! reg-sub-raw subs-raw)
  subs-raw)

(defmethod ig/halt-key! ::subs-raw [_ subs-raw]
  (run! clear-sub subs-raw)
  (rf/clear-subscription-cache!)
  subs-raw)

;;; Effects

(defn- reg-fx [[id handler]]
  (let [id' (get-id ::fx id handler)]
    (rf/reg-fx id' handler)))

(defn- clear-fx [[id _]] (rf/clear-fx id))

(defmethod ig/init-key ::fxs [_ fxs]
  (run! reg-fx fxs)
  fxs)

(defmethod ig/halt-key! ::fxs [_ fxs]
  (run! clear-fx fxs)
  fxs)

;;; Co-Effects

(defn- reg-cofx [[id handler]]
  (let [id' (get-id ::cofx id handler)]
    (rf/reg-cofx id' handler)))

(defn- clear-cofx [[id _]] (rf/clear-cofx id))

(defmethod ig/init-key ::cofxs [_ cofxs]
  (run! reg-cofx cofxs)
  cofxs)

(defmethod ig/halt-key! ::cofxs [_ cofxs]
  (run! clear-cofx cofxs)
  cofxs)

;;; Events Helpers

(defn- get-handler [handler-or-args]
  (cond
    (fn? handler-or-args) handler-or-args
    (coll? handler-or-args) (second handler-or-args)
    :else (throw (ex-info "Bad argument" {}))))

(defn- get-interceptors [handler-or-args]
  (cond-> []
          (coll? handler-or-args) (conj (first handler-or-args))))

;;; Events Fx

(defn- reg-event-fx [config [id handler-or-args]]
  (let [{:keys [views interceptor]} config
        id'          (get-id ::event-fx id handler-or-args)
        has-view?    (some? (get views id'))
        handler      (get-handler handler-or-args)
        interceptors (cond-> (get-interceptors handler-or-args)
                             has-view? (conj interceptor))]
    (rf/reg-event-fx id' interceptors handler)))

(defn- clear-event [[id _]] (rf/clear-event id))

(defmethod ig/init-key ::events-fx
  [_ {:keys [events-fx] :as config}]
  (let [reg-event-fx' (partial reg-event-fx config)]
    (run! reg-event-fx' events-fx))
  events-fx)

(defmethod ig/halt-key! ::events-fx [_ {:keys [events-fx]}]
  (run! clear-event events-fx))

;;; Events Db

(defn- reg-event-db [config [id handler-or-args]]
  (let [{:keys [views interceptor]} config
        id'          (get-id ::event-db id handler-or-args)
        has-view?    (some? (get views id'))
        handler      (get-handler handler-or-args)
        interceptors (cond-> (get-interceptors handler-or-args)
                             has-view? (conj interceptor))]
    (rf/reg-event-db id' interceptors handler)))

(defmethod ig/init-key ::events-db
  [_ {:keys [events-db] :as config}]
  (let [reg-event-db' (partial reg-event-db config)]
    (run! reg-event-db' events-db))
  events-db)

(defmethod ig/halt-key! ::events-db [_ {:keys [events-db]}]
  (run! clear-event events-db))

;;; Module

(def ^:private default-config
  {::reg-fns   (refmap/refmap ::reg-fn)
   ::subs      (refmap/refmap ::sub)
   ::subs-raw  (refmap/refmap ::sub-raw)
   ::fxs       (refmap/refmap ::fx)
   ::cofxs     (refmap/refmap ::cofx)
   ::events-fx {:views       (ig/ref :tape.mvc.view/views)
                :interceptor (ig/ref :tape.mvc.view/interceptor)
                :events-fx   (refmap/refmap ::event-fx)}
   ::events-db {:views       (ig/ref :tape.mvc.view/views)
                :interceptor (ig/ref :tape.mvc.view/interceptor)
                :events-db   (refmap/refmap ::event-db)}})

(defmethod ig/init-key ::module [_ conf]
  (fn [config]
    (module/merge-configs default-config conf config)))
