(ns tape.mvc.controller
  (:require [cljs.analyzer.api :as api]
            [integrant.core :as ig]
            [tape.module :as module]
            [tape.mvc.meta :as meta]))

(defn- controller-ns? [[k _]] (= "tape.mvc.controller" (namespace k)))

(defmacro defmodule
  "Called at the end of controller namespaces, derives the handlers according
  to their metadata declaration, and defines a module that adds them to the
  system config map. Example:

  ```cljs
  (ns blog.app.greet.controller
    (:require [tape.mvc.controller :as c :include-macros true]))

  (defn ^::c/event-db hello [_db [_ev-id _params]]
    {::say \"Hello Tape MVC!\"})

  (defn ^::c/sub say [db _query]
    (::say db))

  (c/defmodule)
  ```

  The `defmodule` call above is equivalent to:

  ```cljs
  (derive ::hello ::c/event-db)
  (derive ::say ::c/sub)

  (defmethod integrant.core/init-key ::module [_ _]
    (fn [config]
      (tape.module/merge-configs config {::hello hello
                                         ::say say})))
  ```
  "
  []
  (let [ns-str        (str *ns*)
        ns-sym        (symbol ns-str)
        ns-meta'      (meta/ns-meta controller-ns? ns-sym)
        module        (keyword ns-str "module")
        var-infos     (vals (api/ns-publics ns-sym))

        collect'      (partial meta/collect ns-meta' var-infos {})
        ->kw-fn'      (partial meta/->kw-fn ns-str)
        ->kw-sub-reg' (partial meta/->kw-reg ::signal ns-str)
        ->kw-ev-reg'  (partial meta/->kw-reg ::interceptors ns-str)

        subs          (collect' ::sub ->kw-sub-reg')
        subs-raw      (collect' ::sub-raw ->kw-fn')
        fxs           (collect' ::fx ->kw-fn')
        cofxs         (collect' ::cofx ->kw-fn')
        events-fx     (collect' ::event-fx ->kw-ev-reg')
        events-db     (collect' ::event-db ->kw-ev-reg')

        subsd         (map (meta/->derive ::sub) subs)
        subs-rawd     (map (meta/->derive ::sub-raw) subs-raw)
        fxsd          (map (meta/->derive ::fx) fxs)
        cofxsd        (map (meta/->derive ::cofx) cofxs)
        events-fxd    (map (meta/->derive ::event-fx) events-fx)
        events-dbd    (map (meta/->derive ::event-db) events-db)

        derives       (concat subsd subs-rawd fxsd cofxsd events-fxd events-dbd)
        config        (merge subs subs-raw fxs cofxs events-fx events-db)]

    `(do ~@derives
         (defmethod ig/init-key ~module [_k# _v#]
           (fn [config#]
             (module/merge-configs config# ~config))))))