(ns tape.mvc
  "Modules automation:
   - ergonomic utilities,
   - collect module registration data from namespaces,
   - modules discovery for less verbose config."
  (:require [cljs.env :as env]
            [cljs.analyzer :as ana]
            [cljs.analyzer.api :as api]
            [clojure.string :as string]
            [me.raynes.fs :as fs]
            [integrant.core :as ig]
            [tape.module :as module])
  (:import [java.io File]))

;;; Ergonomic API

(defn event-kw
  "Given a namespace qualified symbol of a tape event handler, returns the
  corresponding event keyword. Call from macro. Example:
  `(event-kw &env 'counter.c/increment) ; => ::counter.c/increment`."
  [env fsym]
  (let [{:keys [ns name] ::keys [event-db event-fx]} (api/resolve env fsym)
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
  `(sub-kw &env 'counter.c/count) ; => ::counter.c/count`."
  [env fsym]
  (let [{:keys [ns name] ::keys [sub]} (api/resolve env fsym)]
    (cond
      (qualified-keyword? sub) sub
      (qualified-symbol? name) (keyword name)
      :else (keyword (str ns) (str name)))))

(defmacro dispatch
  "Like `re-frame.core/dispatch` but with  an IDE navigable symbol instead a
  keyword. When compiled it replaces the symbol with the keyword. Example:
  `(mvc/dispatch [counter.c/inc]) ; => (rf/subscribe [::counter.c/inc])`."
  [[fsym & args]]
  `(re-frame.core/dispatch ~(into [(event-kw &env fsym)] args)))

(defmacro subscribe
  "Like `re-frame.core/subscribe` but with  an IDE navigable symbol instead a
  keyword. When compiled it replaces the symbol with the keyword. Example:
  `(mvc/subscribe [counter.c/count]) ; => (rf/subscribe [::counter.c/count])`."
  [[fsym & args]]
  `(re-frame.core/subscribe ~(into [(sub-kw &env fsym)] args)))

;;; Meta

(def ^:private meta-keys
  [::reg ::id ::interceptors ::signals ::controller-ns-str])

(defn- ->pair
  "Return a registration pair for the given var-info."
  [var-info]
  (let [name-sym (-> var-info :name)
        name-kw (keyword name-sym)
        reg-val (-> var-info :meta ::reg)
        local-sym (-> name-sym name symbol)
        final-meta (select-keys (:meta var-info) meta-keys)]
    [[name-kw reg-val] `(with-meta ~local-sym ~final-meta)]))

(defn- config
  "Returns the module map for the given `var-infos`, adding `extra-meta`data."
  [extra-meta var-infos]
  (->> var-infos
       (filter #(-> % :meta ::reg some?))
       (map #(update % :meta (partial merge extra-meta)))
       (map ->pair)
       (into {})))

;;; Module data

(defmulti collect
  (fn [] (-> (api/current-ns) str (string/split #"\.") last keyword)))

(defmethod collect :default []
  (throw (ex-info "Namespace name must end with '.controller' or '.view'"
                  {:ex ::collect})))

(defmethod collect :controller []
  (let [ns-sym (api/current-ns)
        ns-meta (-> ns-sym api/find-ns :name meta)
        var-infos (vals (api/ns-publics ns-sym))]
    (config ns-meta var-infos)))

(defmethod collect :view []
  (let [ns-sym (api/current-ns)
        ns-meta (-> ns-sym api/find-ns :name meta)
        var-infos (vals (api/ns-publics ns-sym))
        extra-meta {::controller-ns-str (string/replace
                                         (str ns-sym) #".view$" ".controller")}]
    (config (merge ns-meta extra-meta) var-infos)))

;;; Defm

(defmacro defm
  "Called at the end of controller or view namespaces.
  Collects `::reg` fns and defines a module that adds them to the system config
  map.

  Controller example:

  ```clojure
  (ns blog.app.greet.controller
    (:require [tape.mvc.reg :as reg :include-macros true]))

  (defn hello
    {::c/reg ::c/event-db}
    [_db [_ev-id _params]] {::say \"Hello Tape MVC!\"})

  (defn say
    {::c/reg ::c/sub}
    [db _query] (::say db))

  (reg/defm ::module)
  ```

  The `defm` call above is equivalent to:

  ```clojure
  (defmethod ig/init-key ::module [_ _]
    (fn [config]
      (module/merge-configs config {[::hello ::c/event-db] hello
                                    [::say ::c/sub] say})))
  ```

  View example:

  ```clojure
  (ns blog.app.greet.view
    (:require [re-frame.core :as rf]
              [tape.mvc.reg :as reg :include-macros true]
              [blog.app.greet.controller :as greet.c]))

  (defn hello
    {::mvc/reg ::mvc/view}
    []
    (let [say @(rf/subscribe [::greet.c/say])]
      [:p say]))

  (reg/defm ::module) ;; blog.app.greet.controller ns must exist
  ```

  The `defm` call above is equivalent to:

  ```clojure
  (defmethod ig/init-key ::module [_ _]
    (fn [config]
      (module/merge-configs config {[::hello ::mvc/view] hello})))
  ```
  "
  ([kw]
   `(defm ~kw {}))
  ([kw conf]
   `(defmethod ig/init-key ~kw [_k# _v#]
      (fn [config#]
        (module/merge-configs config# ~(collect) ~conf)))))

;;; Modules discovery

(defn- cljs? [f]
  (and (not (fs/directory? f))
       (re-matches #"(.*?)(\.cljs)$" (.getPath f))))

(defn- ls-r
  "Recursively list files under path."
  [path]
  (let [files (fs/list-dir path)]
    (concat (filter cljs? files)
            (->> files
                 (filter fs/directory?)
                 (mapcat ls-r)))))

(def ^:private src-count (count (.getPath fs/*cwd*)))

(defn- subtract-src [app-path path]
  (let [subtract-count (+ 2 src-count (.indexOf app-path File/separator))]
    (subs path subtract-count (count path))))

(defn- path-ns-str [path]
  (.. (.replaceAll (str path) "\\.clj(s|c)" "")
      (replace \_ \-)
      (replace \/ \.)))

(defn- view-or-controller? [s]
  (re-matches #"(.*?)(view|controller)$" s))

(defn- controller? [s]
  (re-matches #"(.*?)(controller)$" s))

(defn- requires [modules]
  (map #(list 'require (list 'quote (symbol %))) modules))

(defn- views-and-controllers [app-path]
  (let [subtract-src' (partial subtract-src app-path)]
    (->> (ls-r app-path)
         (map (comp path-ns-str subtract-src' #(.getPath %)))
         (filter view-or-controller?))))

(defmacro require-modules
  "Discover modules under `app-path` and require them. Use form at top level.
  Example: `(mvc/require-modules \"src/blog/app\")`."
  [app-path]
  (let [files (views-and-controllers app-path)]
    `(do ~@(requires files))))

(defmacro modules-map
  "Returns a map with modules discovered under `app-path` to be merged in
  modules config map. Example: `(mvc/modules-map \"src/blog/app\")`."
  [app-path]
  (let [modules (views-and-controllers app-path)
        modules-kw (map #(keyword % "module") modules)]
    (apply hash-map (interleave modules-kw (repeat nil)))))
