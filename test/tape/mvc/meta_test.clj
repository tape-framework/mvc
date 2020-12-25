(ns tape.mvc.meta-test
  (:require [clojure.test :refer :all]
            [tape.mvc.meta :as meta]
            [tape.mvc.controller :as c]))

(deftest ->kw-var-test
  (let [var-info {:name 'foo.bar/baz
                  :meta {::c/signals 'signals,
                         ::c/reg ::c/sub}}
        var-info2 {:name 'foo.bar/baz
                   :meta {::c/signals 'signals,
                          ::c/reg ::c/sub,
                          ::c/id :foo.bar/qux}}]
    (is (= [:foo.bar/baz '(clojure.core/with-meta
                           baz
                           {::c/signals signals
                            ::c/reg ::c/sub})]
           (meta/->kw-var var-info)))
    (is (= [:foo.bar/baz '(clojure.core/with-meta
                           baz
                           {::c/signals signals
                            ::c/reg ::c/sub
                            ::c/id :foo.bar/qux})]
           (meta/->kw-var var-info2)))))

(def ^:private var-infos
  [{:name 'bar.baz/qux
    :meta {::c/reg ::c/sub}}
   {:name 'bar.baz/quux
    :meta {::c/reg ::c/sub
           ::c/id :bar.baz/zub
           ::c/signals 'signals}}])

(deftest config-test
  (let [extra-meta {:tape.mvc.view/controller-ns-str "a.b.controller"}]
    (is (= '#:bar.baz{:quux (clojure.core/with-meta
                             quux
                             {:tape.mvc.controller/reg :tape.mvc.controller/sub
                              :tape.mvc.controller/id :bar.baz/zub
                              :tape.mvc.controller/signals signals
                              :tape.mvc.view/controller-ns-str "a.b.controller"})
                      :qux (clojure.core/with-meta
                            qux
                            {:tape.mvc.controller/reg :tape.mvc.controller/sub
                             :tape.mvc.view/controller-ns-str "a.b.controller"})}
           (meta/config ::c/reg extra-meta var-infos)))))

(deftest derives-test
  (is (= `((derive bar.baz/qux :tape.mvc.controller/sub)
           (derive bar.baz/quux :tape.mvc.controller/sub))
         (meta/derives ::c/reg var-infos))))
