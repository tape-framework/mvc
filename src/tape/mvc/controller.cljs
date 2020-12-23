(ns tape.mvc.controller
  "Re-frame handlers registry."
  (:require [integrant.core :as ig]
            [re-frame.core :as rf]
            [tape.refmap :as refmap]
            [tape.module :as module]))

;;; Helpers

(defn- kval-ex [id k kval]
  (ex-info (str k " val must be true or a qualified keyword, but is " kval)
           {:id id
            k kval}))

(defn- get-id [kind id handler]
  (let [kval (some-> handler meta kind)]
    (cond
      (qualified-keyword? kval) kval
      (or (true? kval) (nil? kval)) id
      :else (throw (kval-ex id kind kval)))))

(defn- get-signals [handler]
  (-> handler meta ::signals (or [])))

(defn- get-interceptors [handler]
  (-> handler meta ::interceptors (or [])))

;;; Subscriptions

(defn- reg-sub [frame [id handler]]
  (let [id'     (get-id ::sub id handler)
        signals (get-signals handler)
        args    (conj (into [id'] signals) handler)]
    (apply rf/reg-sub args)
    [id' handler]))

(defn- clear-sub [frame [id _]]
  (rf/clear-sub id))

(defmethod ig/init-key ::subs [_ {:keys [frame subs] :as config}]
  (let [reg-sub' (partial reg-sub frame)]
    (assoc config
      :subs (->> subs
                 (map reg-sub')
                 (into {})))))

(defmethod ig/halt-key! ::subs [_ {:keys [frame subs] :as config}]
  (let [clear-sub' (partial clear-sub frame)]
    (run! clear-sub' subs))
  (rf/clear-subscription-cache!)
  config)

;;; Raw Subscriptions

(defn- reg-sub-raw [frame [id handler]]
  (let [id' (get-id ::sub-raw id handler)]
    (rf/reg-sub-raw id' handler)
    [id' handler]))

(defmethod ig/init-key ::subs-raw [_ {:keys [frame subs-raw] :as config}]
  (let [reg-sub-raw' (partial reg-sub-raw frame)]
    (assoc config
      :subs-raw (->> subs-raw
                     (map reg-sub-raw')
                     (into {})))))

(defmethod ig/halt-key! ::subs-raw [_ {:keys [frame subs-raw] :as config}]
  (let [clear-sub' (partial clear-sub frame)]
    (run! clear-sub' subs-raw))
  (rf/clear-subscription-cache!)
  config)

;;; Effects

(defn- reg-fx [frame [id handler]]
  (let [id' (get-id ::fx id handler)]
    (rf/reg-fx id' handler)
    [id' handler]))

(defn- clear-fx [frame [id _]]
  (rf/clear-fx id))

(defmethod ig/init-key ::fxs [_ {:keys [frame fxs] :as config}]
  (let [reg-fx' (partial reg-fx frame)]
    (assoc config
      :fxs (->> fxs
                (map reg-fx')
                (into {})))))

(defmethod ig/halt-key! ::fxs [_ {:keys [frame fxs] :as config}]
  (let [clear-fx' (partial clear-fx frame)]
    (run! clear-fx' fxs))
  config)

;;; Co-Effects

(defn- reg-cofx [frame [id handler]]
  (let [id' (get-id ::cofx id handler)]
    (rf/reg-cofx id' handler)
    [id' handler]))

(defn- clear-cofx [frame [id _]]
  (rf/clear-cofx id))

(defmethod ig/init-key ::cofxs [_ {:keys [frame cofxs] :as config}]
  (let [reg-cofx' (partial reg-cofx frame)]
    (assoc config
      :cofxs (->> cofxs
                  (map reg-cofx')
                  (into {})))))

(defmethod ig/halt-key! ::cofxs [_ {:keys [frame cofxs] :as config}]
  (let [clear-cofx' (partial clear-cofx frame)]
    (run! clear-cofx' cofxs))
  config)

;;; Events Fx

(defn- reg-event-fx [config [id handler]]
  (let [{:keys [frame views interceptor]} config
        id'          (get-id ::event-fx id handler)
        has-view?    (some? (get views id'))
        interceptors (cond-> (get-interceptors handler)
                             has-view? (conj interceptor))]
    (rf/reg-event-fx id' interceptors handler)
    [id' handler]))

(defn- clear-event [frame [id _]]
  (rf/clear-event id))

(defmethod ig/init-key ::events-fx
  [_ {:keys [events-fx] :as config}]
  (let [reg-event-fx' (partial reg-event-fx config)]
    (assoc config
      :events-fx (->> events-fx
                      (map reg-event-fx')
                      (into {})))))

(defmethod ig/halt-key! ::events-fx
  [_ {:keys [frame events-fx] :as config}]
  (let [clear-event' (partial clear-event frame)]
    (run! clear-event' events-fx))
  config)

;;; Events Db

(defn- reg-event-db [config [id handler]]
  (let [{:keys [frame views interceptor]} config
        id'          (get-id ::event-db id handler)
        has-view?    (some? (get views id'))
        interceptors (cond-> (get-interceptors handler)
                             has-view? (conj interceptor))]
    (rf/reg-event-db id' interceptors handler)
    [id' handler]))

(defmethod ig/init-key ::events-db
  [_ {:keys [events-db] :as config}]
  (let [reg-event-db' (partial reg-event-db config)]
    (assoc config
      :events-db (->> events-db
                      (map reg-event-db')
                      (into {})))))

(defmethod ig/halt-key! ::events-db [_ {:keys [frame events-db] :as config}]
  (let [clear-event' (partial clear-event frame)]
    (run! clear-event' events-db))
  config)

;;; Frame

(defmethod ig/init-key ::frame
  [_ {:keys [default] :or {default false}}]
  nil)

;;; Module

(def ^:private default-config
  {::frame     nil
   ::subs      {:frame (ig/ref ::frame)
                :subs  (refmap/refmap ::sub)}
   ::subs-raw  {:frame    (ig/ref ::frame)
                :subs-raw (refmap/refmap ::sub-raw)}
   ::fxs       {:frame (ig/ref ::frame)
                :fxs   (refmap/refmap ::fx)}
   ::cofxs     {:frame (ig/ref ::frame)
                :cofxs (refmap/refmap ::cofx)}
   ::events-fx {:frame       (ig/ref ::frame)
                :views       (ig/ref :tape.mvc.view/views)
                :interceptor (ig/ref :tape.mvc.view/interceptor)
                :events-fx   (refmap/refmap ::event-fx)}
   ::events-db {:frame       (ig/ref ::frame)
                :views       (ig/ref :tape.mvc.view/views)
                :interceptor (ig/ref :tape.mvc.view/interceptor)
                :events-db   (refmap/refmap ::event-db)}})

(defmethod ig/init-key ::module [_ conf]
  (fn [config]
    (module/merge-configs default-config conf config)))
