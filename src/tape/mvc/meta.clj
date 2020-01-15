(ns tape.mvc.meta
  (:require [cljs.analyzer.api :as api]))

(defn ns-meta
  "Returns a map of meta data on the namespace `ns-sym` who'se pairs obey the
  `ns-pred` predicate."
  [ns-pred ns-sym]
  (->> ns-sym api/find-ns :name meta (filter ns-pred) (into {})))

(defn- kval-ex [id k kval]
  (ex-info (str k " val must be true or a qualified keyword, but is " kval)
           {:id id
            k kval}))

(defn ->kw-fn
  "Given the namespace string `ns-str`, extra metadata `m`, metadata key `k`
  and the `var-info` of a var, returns a pair `[event-kw fn-sym]` to be used
  in registration."
  [ns-str m k var-info]
  (let [sym-name (-> var-info :name name)
        kval     (-> var-info :meta k)
        event-kw (cond
                   (qualified-keyword? kval) kval
                   (or (true? kval) (nil? kval)) (keyword ns-str sym-name)
                   :else (throw (kval-ex sym-name k kval)))
        fn-sym   (symbol sym-name)
        m'       (merge (:meta var-info) m)]
    [event-kw `(with-meta ~fn-sym ~m')]))

(defn ->kw-reg
  "Given the namespace string `ns-str`, extra metadata `m`, metadata key `k`
  and the `var-info` of a var, returns a pair `[event-kw val]` to be used in
  registration, where `val` is either a `fn-sym` or a vector of extra input
  and `fn-sym`."
  [reg-key ns-str m k var-info]
  (let [sym-name (-> var-info :name name)
        kval     (-> var-info :meta k)
        input    (-> var-info :meta reg-key)
        event-kw (cond
                   (qualified-keyword? kval) kval
                   (or (true? kval) (nil? kval)) (keyword ns-str sym-name)
                   :else (throw (kval-ex sym-name k kval)))
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
        ->pair'       (partial ->pair m k)]
    (->> var-infos (map add-nsp-meta') (filter pred) (map ->pair') (into {}))))

(defn ->derive
  "Returns a fn that derives a pair key `k` from `from`."
  [from]
  (fn [[k _]] (list `derive k from)))
