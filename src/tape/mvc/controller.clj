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

  (defn hello
    {::c/reg ::c/event-db}
    [_db [_ev-id _params]]
    {::say \"Hello Tape MVC!\"})

  (defn say
    {::c/reg ::c/sub}
    [db _query]
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
  (let [ns-sym (api/current-ns)
        ns-str (str ns-sym)
        ns-meta (-> ns-sym api/find-ns :name meta)
        module (keyword ns-str "module")
        var-infos (vals (api/ns-publics ns-sym))

        config (meta/config ::reg ns-meta var-infos)
        derives (meta/derives ::reg var-infos)]

    `(do ~@derives
         (defmethod ig/init-key ~module [_k# _v#]
           (fn [config#]
             (module/merge-configs config# ~config))))))
