(ns tape.mvc.controller
  (:require [cljs.analyzer.api :as api]
            [integrant.core :as ig]
            [tape.module :as module]
            [tape.mvc.meta :as meta]))

(defmacro defmodule
  "Called at the end of controller namespaces, derives the handlers according
  to their metadata declaration, and defines a module that adds them to the
  system config map. Example:

  ```clojure
  (ns blog.app.greet.controller
    (:require [tape.mvc.controller :as c :include-macros true]))

  (defn ^::c/event-db hello [_db [_ev-id _params]]
    {::say \"Hello Tape MVC!\"})

  (defn ^::c/sub say [db _query]
    (::say db))

  (c/defmodule)
  ```

  The `defmodule` call above is equivalent to:

  ```clojure
  (derive ::hello ::c/event-db)
  (derive ::say ::c/sub)

  (defmethod integrant.core/init-key ::module [_ _]
    (fn [config]
      (tape.module/merge-configs config {::hello hello
                                         ::say say})))
  ```
  "
  []
  (let [ns-sym       (api/current-ns)
        ns-str       (str ns-sym)
        ns-meta      (-> ns-sym api/find-ns :name meta)
        module       (keyword ns-str "module")
        var-infos    (vals (api/ns-publics ns-sym))

        collect      (partial meta/collect ns-meta var-infos {})
        ->kw-var     (partial meta/->kw-var ns-str)

        routes       (collect ::routes ->kw-var)
        subs         (collect ::sub ->kw-var)
        subs-raw     (collect ::sub-raw ->kw-var)
        fxs          (collect ::fx ->kw-var)
        cofxs        (collect ::cofx ->kw-var)
        events-fx    (collect ::event-fx ->kw-var)
        events-db    (collect ::event-db ->kw-var)

        routesd      (map (meta/->derive ::routes) routes)
        subsd        (map (meta/->derive ::sub) subs)
        subs-rawd    (map (meta/->derive ::sub-raw) subs-raw)
        fxsd         (map (meta/->derive ::fx) fxs)
        cofxsd       (map (meta/->derive ::cofx) cofxs)
        events-fxd   (map (meta/->derive ::event-fx) events-fx)
        events-dbd   (map (meta/->derive ::event-db) events-db)

        derives      (concat routesd subsd subs-rawd fxsd cofxsd events-fxd events-dbd)
        config       (merge routes subs subs-raw fxs cofxs events-fx events-db)]

    `(do ~@derives
         (defmethod ig/init-key ~module [_k# _v#]
           (fn [config#]
             (module/merge-configs config# ~config))))))
