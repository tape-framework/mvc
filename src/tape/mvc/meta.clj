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

(defn ->kw-var
  "Given the `extra-meta`data, and the `var-info` of a var, returns a pair
  `[reg-key var-sym]` to be used in registration."
  [extra-meta var-info]
  (let [sym (-> var-info :name)
        reg-key (keyword sym)
        var-sym (-> sym name symbol)
        extra-meta' (merge (:meta var-info) extra-meta)]
    [reg-key `(with-meta ~var-sym ~extra-meta')]))

(defn- add-nsp-meta [nsp-meta var-info]
  (update var-info :meta #(merge nsp-meta %)))

(defn- has-meta? [k v var-info]
  (-> var-info :meta k (= v)))

(defn collect
  "Given namespace metadata `ns-meta`, list of vars info `var-infos`,
  `extra-meta`data and metadata key `reg-kw` return of map of
  `{kw -> reg-data}` to be used in registration."
  [ns-meta var-infos extra-meta reg-kw reg-val]
  (let [add-nsp-meta' (partial add-nsp-meta ns-meta)
        pred          (partial has-meta? reg-kw reg-val)
        ->kw-var'     (partial ->kw-var extra-meta)]
    (->> var-infos (map add-nsp-meta') (filter pred) (map ->kw-var') (into {}))))

(defn ->derive
  "Returns a fn that derives a pair key `k` from `from`."
  [from]
  (fn [[k _]] (list `derive k from)))
