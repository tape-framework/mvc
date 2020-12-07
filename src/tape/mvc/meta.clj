(ns tape.mvc.meta
  (:require [cljs.env :as env]
            [cljs.analyzer :as ana]
            [cljs.analyzer.api :as api]))

(defn- add-meta-flag [aname]
  (with-meta aname (assoc (meta aname) :tape.mvc/module! true)))

(defn flag-ns-meta! [ns-sym]
  (swap! env/*compiler* update-in [::ana/namespaces ns-sym :name] add-meta-flag))

(defn ns-meta
  "Returns a map of meta data on the namespace `ns-sym` who'se pairs obey the
  `ns-pred` predicate."
  [ns-pred ns-sym]
  (->> ns-sym api/find-ns :name meta (filter ns-pred) (into {})))

(defn ->kw-fn
  "Given the namespace string `ns-str`, extra metadata `m`, metadata key `k`
  and the `var-info` of a var, returns a pair `[event-kw fn-sym]` to be used
  in registration."
  [ns-str m var-info]
  (let [sym-name (-> var-info :name name)
        event-kw (keyword ns-str sym-name)
        fn-sym   (symbol sym-name)
        m'       (merge (:meta var-info) m)]
    [event-kw `(with-meta ~fn-sym ~m')]))

(defn ->kw-reg
  "Given the namespace string `ns-str`, extra metadata `m`, metadata key `k`
  and the `var-info` of a var, returns a pair `[event-kw val]` to be used in
  registration, where `val` is either a `fn-sym` or a vector of extra input
  and `fn-sym`."
  [reg-key ns-str m var-info]
  (let [sym-name (-> var-info :name name)
        input    (-> var-info :meta reg-key)
        event-kw (keyword ns-str sym-name)
        fn-sym   (symbol sym-name)
        val      (if input [input fn-sym] fn-sym)
        m'       (merge (:meta var-info) m)]
    [event-kw `(with-meta ~val ~m')]))

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
