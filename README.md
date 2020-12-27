## README

STATUS: Pre-alpha, in design and prototyping phase.

#### About

`tape.mvc`

An alternative interface to [Re-Frame](https://github.com/day8/re-frame/) 
(and [Reagent](https://reagent-project.github.io/)) - via an
[Integrant](https://github.com/weavejester/integrant) system built out of a
`tape.module` system ([ported](https://github.com/duct-framework/core/) from Duct Framework).

#### Usage

You must be familiar with Reagent, Re-Frame, Integrant, `tape.refmap` &
`tape.module` before proceeding.

##### Install

Add `tape.mvc` to your deps:

```clojure
tape/mvc {:local/root "../mvc"}
```

##### Structure

Your project directory structure should be similar to:

```
blog
├── deps.edn
├── resources
│   └── blog
│       └── config.edn
└── src
    └── blog
        ├── core.cljs
        └── app
            ├── layouts
            │   └── app.cljs
            ├── users
            │   └── ...
            └── posts
                ├── controller.cljs
                ├── model.cljs
                └── view.cljs
```

Under `blog.app` you should have a package for each piece of your UI (users, 
posts). Each of these will have a model, view and controller namespace (not 
necessarily all 3).

The **model** is a normal namespace, and is likely to be missing if you have
little to no business logic for that UI piece.

##### Controllers

The **controller** namespaces contain functions that can be registered in
Re-Frame. Each fn that is to be registered is annotated in metadata with
`^{::c/reg <kind>}` where `<kind>` is one of:
- `::c/sub` **subscription**
- `::c/sub-raw` raw subscription
- `::c/fx` effect handler
- `::c/cofx` coeffect handler
- `::c/event-fx` **event handler** with co/effect i/o
- `::c/event-db` **event handler** with app-db i/o

To provide interceptors to event handlers, or signals to subscriptions, use the
following metadata:
- `::c/interceptors` vector of symbols that evaluate to interceptors in the
  current namespace
- `::c/signals` vector of symbols that evaluate to signals in the current namespace

Metadata at the namespace level is added to the functions that are registered,
and can be used to:
- add interceptors to all event handlers in a namespace,
- or a signal to all subscriptions in the namespace.

Function metadata directives override namespace metadata ones.

```clojure
(ns blog.app.greet.controller
  (:require [tape.mvc.controller :as c :include-macros true]))

(defn hello
  {::c/reg ::c/event-db}
  [_db [_ev-id _params]]
  {::say "Hello Tape MVC!"})

(defn say
  {::c/reg ::c/sub}
  [db _query]
  (::say db))

(c/defmodule)
```

There is a `(c/defmodule)` call at the end that inspects the current namespace
and defines a `tape.module` that will have the functions be registered in
Re-Frame. It is equivalent to:

```clojure
(derive ::hello ::c/event-db)
(derive ::say ::c/sub)

(defmethod ig/init-key ::module [_ _]
  (fn [config]
    (module/merge-configs config {::hello hello
                                  ::say say})))
```

Derived keys are collected by `tape.refmap` and registered in Re-Frame.

##### Views

The **view** namespace contains Reagent functions. Some can be registered in
the views map if they are annotated with:

- `^{::v/reg ::v/view}` if the function is to be registered in the views
  registry map

The key in the map will be based off the controller namespace, as to match an
event (see naming conventions below) that can select a view; example:
`{::greet.c/hello greet.v/hello}`.

```clojure
(ns blog.app.greet.view
  (:require [re-frame.core :as rf]
            [tape.mvc.view :as v :include-macros true]
            [blog.app.greet.controller :as greet.c]))

(defn hello
  {::v/reg ::v/view}
  []
  (let [say @(rf/subscribe [::greet.c/say])]
    [:p say]))

(v/defmodule) ;; blog.app.greet.controller must exist
```

There is a `(v/defmodule)` call at the end that inspects the current namespace
and defines a `tape.module` that will have the functions be registered in the
views registry map. It is equivalent to:
                                                                          
```clojure
(derive ::hello ::v/view)

(defmethod ig/init-key ::module [_ _]
  (fn [config]
    (module/merge-configs
     config
     {::hello ^{::v/controller-ns-str "blog.app.greet.controller"} hello})))
```

Derived keys are collected by `tape.refmap` and registered in the view registry.

`defmodule` macros can also be called with a map argument that's merged in the
module configuration output. This allows plain integrant components to be
defined. It's more verbose than the plain function approach, but we can inject
dependencies from the system map via `ig/ref` & such.

```clojure
(...)

(defn hello
  {::c/reg ::c/event-db}
  [_db [_ev-id _params]]
  {::say "Hello Tape MVC!"})

(defmethod ig/init-key ::say [_ some-db]
  (fn [_ _] (ratom/reaction (::something @some-db))))

(derive ::say ::c/sub-raw)
(c/defmodule {::say (ig/ref ::some-ns/some-db)})
```

##### Modules system

In `resources/blog/config.edn` declare your modules that will
`tape.module/fold-modules` into your Integrant system's config-map:

```edn
{:tape.profile/base {}
 :tape.mvc.controller/module nil
 :tape.mvc.view/module nil
 :blog.app.greet.controller/module nil
 :blog.app.greet.view/module nil}
```

In `src/blog/core.cljs` you `tape.module/read-config` and
`tape.module/exec-config` your Integrant system:

```clojure
(ns tape.blog
  (:require
    [goog.dom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [tape.module :as module :include-macros true]
    [tape.mvc.controller :as c]
    [tape.mvc.view :as v]
    [blog.app.greet.controller :as greet.c]
    [blog.app.greet.view :as greet.v]))

(module/load-hierarchy)
(def config (module/read-config "tape/blog/config.edn"))
(defonce system nil)
(defn app [] [:div @(rf/subscribe [::v/current-fn])])

(defn -main []
  (set! system (-> config module/prep-config ig/init))
  (rf/dispatch-sync [::greet.c/hello])
  (r/render-component [app] (goog.dom/getElement "app")))
```

##### Conventions

Note naming convention on requires:

- controllers: `[blog.app.posts.controller :as posts.c]`
- views: `[blog.app.posts.view :as posts.v]`

Note the exclusive use of namespaced keywords and the naming conventions:

- (if `tape.router` is used) the route named `::posts.c/index`
  dispatches
- the event with the id `::posts.c/index` which
- is handled by the `posts.c/index` handler
- and (if `tape.current` is used) renders the `posts.v/index` view
  (if it exists, and the view was not already set from the handler)
- by setting in app-db `{::current/view ::posts.c/index}`
  (automatically via the view interceptor, or manually in the event handler)
- which results in the subscription `(rf/subscribe [::current/view-fn])`
- to yield the `posts.v/index` view fn

#### License

Copyright © 2019 clyfe

Distributed under the MIT license.
