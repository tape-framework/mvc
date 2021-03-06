(ns tape.mvc
  "Registry that proxies to Re-Frame."
  (:require [integrant.core :as ig]
            [re-frame.core :as rf]
            [tape.refmap :as refmap]
            [tape.module :as module]))

;;; Helpers

(defn- key-ex [id kid]
  (ex-info (str id " ::c/key must be a qualified keyword, but is " kid)
           {:id id
            :kid kid}))

(defn- get-id [idk handler]
  (let [reg-id (some-> handler meta ::id)]
    (cond
      (qualified-keyword? reg-id) reg-id
      (nil? reg-id) (cond-> idk
                            (vector? idk) first)
      :else (throw (key-ex idk reg-id)))))

(defn- get-signals [handler]
  (-> handler meta ::signals (or [])))

(defn- get-interceptors [handler]
  (-> handler meta ::interceptors (or [])))

(defn- with-controller-kw
  "In the view registry, views are indexed by their coresponding controller
  keywords (event names). This changes the key of a reg pair from the view ns
  to the controller ns."
  [[viewk view]]
  (let [controller-ns (-> view meta ::controller-ns-str)
        _ (assert (some? controller-ns))
        view-kw (cond-> viewk
                        (vector? viewk) first)
        controller-kw (keyword controller-ns (name view-kw))]
    [controller-kw view]))

;;; Subscriptions

(defn- reg-sub [frame [id handler]]
  (let [id' (get-id id handler)
        signals (get-signals handler)
        args (conj (into [id'] signals) handler)]
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
  (let [id' (get-id id handler)]
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
  (let [id' (get-id id handler)]
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
  (let [id' (get-id id handler)]
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
  (let [{:keys [frame views interceptors]} config
        id' (get-id id handler)
        has-view? (some? (get views id'))
        interceptors' (cond-> (get-interceptors handler)
                              has-view? (into interceptors))]
    (rf/reg-event-fx id' interceptors' handler)
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
  (let [{:keys [frame views interceptors]} config
        id' (get-id id handler)
        has-view? (some? (get views id'))
        interceptors' (cond-> (get-interceptors handler)
                              has-view? (into interceptors))]
    (rf/reg-event-db id' interceptors' handler)
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

;; Views registry; keys are in the controller namespaces and values are Reagent
;; functions.
(defmethod ig/init-key ::views [_ views]
  (into {} (map with-controller-kw views)))

;;; Frame

(defmethod ig/init-key ::frame
  [_ {:keys [default] :or {default false}}]
  nil)

;;; Module

(def ^:private default-config
  {::frame nil
   ::subs {:frame (ig/ref ::frame)
           :subs (refmap/refmap ::sub)}
   ::subs-raw {:frame (ig/ref ::frame)
               :subs-raw (refmap/refmap ::sub-raw)}
   ::fxs {:frame (ig/ref ::frame)
          :fxs (refmap/refmap ::fx)}
   ::cofxs {:frame (ig/ref ::frame)
            :cofxs (refmap/refmap ::cofx)}
   ::events-fx {:frame (ig/ref ::frame)
                :views (ig/ref ::views)
                :interceptors (ig/refset
                               :tape.tools.current.controller/view-interceptor)
                :events-fx (refmap/refmap ::event-fx)}
   ::events-db {:frame (ig/ref ::frame)
                :views (ig/ref ::views)
                :interceptors (ig/refset
                               :tape.tools.current.controller/view-interceptor)
                :events-db (refmap/refmap ::event-db)}
   ::views (refmap/refmap ::view)
   ::main {:subs (ig/ref ::subs)
           :subs-raw (ig/ref ::subs-raw)
           :fxs (ig/ref ::fxs)
           :cofxs (ig/ref ::cofxs)
           :events-fx (ig/ref ::events-fx)
           :events-db (ig/ref ::events-db)
           :views (ig/ref ::views)}})

(defmethod ig/init-key ::module [_ conf]
  (fn [config]
    (module/merge-configs default-config conf config)))
