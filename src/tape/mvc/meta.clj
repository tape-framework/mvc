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

(defn ns-meta
  "Returns a map of meta data on the namespace `ns-sym` who'se pairs obey the
  `ns-pred` predicate."
  [ns-pred ns-sym]
  (->> ns-sym api/find-ns :name meta (filter ns-pred) (into {})))

(defn ->kw-var
  "Given the namespace string `ns-str`, extra metadata `m`, and the `var-info`
  of a var, returns a pair `[event-kw var-sym]` to be used in registration."
  [ns-str m var-info]
  (let [sym-name (-> var-info :name name)
        event-kw (keyword ns-str sym-name)
        var-sym  (symbol sym-name)
        m'       (merge (:meta var-info) m)]
    [event-kw `(with-meta ~var-sym ~m')]))

(defn- add-nsp-meta [nsp-meta var-info]
  (update var-info :meta #(merge nsp-meta %)))

(defn- has-meta? [pred-key var-info]
  (-> var-info :meta pred-key some?))

(defn collect
  "Given namespace metadata `ns-meta`, list of vars info `var-infos`, extra
  medatada `m`, metadata key `k` and registration pair constructor `->pair`,
  return of map of `{kw -> reg-data}` to be used in events registration."
  [ns-meta var-infos m k ->pair]
  (let [add-nsp-meta' (partial add-nsp-meta ns-meta)
        pred          (partial has-meta? k)
        ->pair'       (partial ->pair m)]
    (->> var-infos (map add-nsp-meta') (filter pred) (map ->pair') (into {}))))

(defn ->derive
  "Returns a fn that derives a pair key `k` from `from`."
  [from]
  (fn [[k _]] (list `derive k from)))
