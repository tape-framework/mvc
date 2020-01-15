## README

STATUS: Pre-alpha, in design and prototyping phase.

#### About

`tape.mvc`

IOC/DI dressing of Re-Frame & Reagent by means of
[Integrant](https://github.com/weavejester/integrant) and `tape.module`.

#### Usage

You must be familiar with [Reagent](https://reagent-project.github.io/) &
[Re-Frame](https://github.com/day8/re-frame), Integrant, `tape.refmap` &
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
            │   └── view.cljs
            ├── users
            │   └── ...
            └── posts
                ├── controller.cljs
                ├── model.cljs
                └── view.cljs
```

Under `blog.app` you should have a package for each piece of your UI (users, 
posts, layouts). Each of these will have a model, view and controller namespace
(not necessarily all 3).

The **model** is a normal namespace, and is likely to be missing if you have
little to no business logic for that UI piece.

##### Controllers

The **controller** namespaces contain functions that can be registered in
Re-Frame. Each fn that is to be registered is annotated with one of:

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
- `::c/signal` symbol that evaluates to a signal in the current namespace

Metadata at the namespace level is added to the functions that are registered,
and can be used to add interceptors to all event handlers in a namespace, or a
signal to all subscriptions in the namespace.

```cljs
(ns blog.app.greet.controller
  (:require [tape.mvc.controller :as c :include-macros true]))

(defn ^::c/event-db hello [_db [_ev-id _params]]
  {::say "Hello Tape MVC!"})

(defn ^::c/sub say [db _query]
  (::say db))

(c/defmodule)
```

There is a `(c/defmodule)` call at the end that inspects the current namespace
and defines a `tape.module` that will have the functions be registered in
Re-Frame. It is equivalent to:

```cljs
(derive ::hello ::c/event-db)
(derive ::say ::c/sub)

(defmethod integrant.core/init-key ::module [_ _]
  (fn [config]
    (tape.module/merge-configs config {::hello hello
                                       ::say say})))
```

##### Views

The **view** namespace contains Reagent functions. Some can be registered in the
views map if they are annotated with:

- `::v/view` if the function is to be registered in the views map

The key in the map will be based off the controller namespace, as to match an
event (see naming conventions below) that can result in the view being set as
current `{::greet.c/hello greet.v/hello}`.

```cljs
(ns blog.app.greet.view
  (:require [re-frame.core :as rf]
            [tape.mvc.view :as v :include-macros true]
            [blog.app.greet.controller :as greet.c]))

(defn ^::v/view hello []
  (let [say @(rf/subscribe [::greet.c/say])]
    [:p say]))

(v/defmodule blog.app.greet.controller)
```

There is a `(v/defmodule blog.app.greet.controller)` call at the end that 
inspects the current namespace and defines a `tape.module` that will have the 
functions be registered in the views registry map. Note the argument that must
be an unquoted symbol of the corresponding controller namespace, in full. It is
equivalent to:
                                                                          
```cljs
(derive ::hello ::v/view)

(defmethod integrant.core/init-key ::module [_ _]
  (fn [config]
    (tape.module/merge-configs config {::hello hello})))
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

```cljs
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

(defn app []
  [:div @(rf/subscribe [::v/current-fn])])

(defn mount []
  (when-let [el (goog.dom/getElement "app")]
    (r/render-component [app] el)))

(defn -main []
  (set! system (-> config module/prep-config ig/init))
  (rf/dispatch-sync [::greet.c/hello])
  (mount))
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
- and (potentially) renders the `posts.v/index` view
  (if it exists, and the view was not already set from the handler)
- by setting in app-db `{::v/current ::posts.c/index}`
  (automatically via the view interceptor, or manually in the event handler)
- which results in the subscription `(rf/subscribe [::v/current-fn])`
- to yield the `posts.v/index` view fn

The current view is automatically set in app-db by an interceptor if not set
already from the event handler, if there exists a view corresponding to the
event, per the naming convention.

#### License

Copyright © 2019 clyfe

Distributed under the MIT license.