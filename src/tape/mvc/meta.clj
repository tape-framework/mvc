(ns tape.mvc.meta
  "Various helpers for operations based on Tape MVC specific metadata
  annotations."
  (:require [cljs.env :as env]
            [cljs.analyzer :as ana]
            [cljs.analyzer.api :as api]))

;;; Ergonomics

(defn event-kw
  "Given a namespace qualified symbol of a tape event handler, returns the
  corresponding event keyword. Call from macro. Example:
  `(meta/event-kw &env 'counter.c/increment) ; => ::counter.c/increment`."
  [env fsym]
  (let [{:keys [ns name] :tape.mvc.controller/keys [event-db event-fx]} (api/resolve env fsym)
        event (->> [event-db event-fx]
                   (filter qualified-keyword?)
                   first)]
    (cond
      (qualified-keyword? event) event
      (qualified-symbol? name) (keyword name)
      :else (keyword (str ns) (str name)))))

(defn sub-kw
  "Given a namespace qualified symbol of a tape subscription, returns the
  corresponding sumscription keyword. Call from macro. Example:
  `(meta/sub-kw &env 'counter.c/count) ; => ::counter.c/count`."
  [env fsym]
  (let [{:keys [ns name] ::keys [sub]} (api/resolve env fsym)]
    (cond
      (qualified-keyword? sub) sub
      (qualified-symbol? name) (keyword name)
      :else (keyword (str ns) (str name)))))

;;; Module

(def ^:private meta-keys
  [:tape.mvc.controller/reg
   :tape.mvc.controller/id
   :tape.mvc.controller/interceptors
   :tape.mvc.controller/signals
   :tape.mvc.view/reg
   :tape.mvc.view/controller-ns-str])

(defn- ->kw-var
  "Given the `extra-meta`data, and the `var-info` of a var, returns a pair
  `[reg-key var-sym]` to be used in registration."
  [var-info]
  (let [sym (-> var-info :name)
        reg-key (keyword sym)
        var-sym (-> sym name symbol)
        final-meta (select-keys (:meta var-info) meta-keys)]
    [reg-key `(with-meta ~var-sym ~final-meta)]))

(defn- add-meta [extra-meta var-info]
  (update var-info :meta #(merge extra-meta %)))

(defn- has-meta? [reg-kw var-info]
  (-> var-info :meta reg-kw some?))

(defn config
  "Given metadata key `reg-kw`, `extra-meta`data, list of vars info
  `var-infos`, return a map of `{id-kw -> reg-sym}` to be used in
  registration."
  [reg-kw extra-meta var-infos]
  (->> var-infos
       (filter (partial has-meta? reg-kw))
       (map (partial add-meta extra-meta))
       (map ->kw-var)
       (into {})))

(defn- ->derive [reg-kw var-info]
  (let [sym (-> var-info :name keyword)
        reg-val (-> var-info :meta reg-kw)]
    (when (qualified-keyword? reg-val)
      (list `derive sym reg-val))))

(defn derives
  "Returns a form that derives the handlers of var-infos according to their
  metadata."
  [reg-kw var-infos]
  (->> var-infos
       (filter (partial has-meta? reg-kw))
       (keep (partial ->derive reg-kw))))
