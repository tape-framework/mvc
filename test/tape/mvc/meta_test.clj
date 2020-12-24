(ns tape.mvc.meta-test
  (:require [clojure.test :refer :all]
            [tape.mvc.meta :as meta]
            [tape.mvc.controller :as c]))

(deftest ->kw-var-test
  (let [extra-meta {::c/signals 'signals}
        var-info {:name 'foo.bar/baz :meta {::c/sub true}}
        var-info2 {:name 'foo.bar/baz :meta {::c/sub :foo.bar/qux}}]
    (is (= [:foo.bar/baz '(clojure.core/with-meta baz {::c/signals signals
                                                       ::c/sub true})]
           (meta/->kw-var extra-meta var-info)))
    (is (= [:foo.bar/baz '(clojure.core/with-meta baz {::c/signals signals
                                                       ::c/sub :foo.bar/qux})]
           (meta/->kw-var extra-meta var-info2)))))

(deftest collect-test
  (let [ns-meta {:x :a}
        var-infos [{:name 'bar.baz/qux :meta {::c/sub true}}
                   {:name 'bar.baz/quux :meta {::c/signals 'signals
                                               ::c/sub :bar.baz/zub}}]
        m {:y :b}
        ->kw-var meta/->kw-var]
    (is (= '#:bar.baz{:quux (clojure.core/with-meta
                             quux
                             {:tape.mvc.controller/signals signals
                              :tape.mvc.controller/sub :bar.baz/zub
                              :x :a
                              :y :b})
                      :qux (clojure.core/with-meta
                            qux
                            {:tape.mvc.controller/sub true
                             :x :a
                             :y :b})}
           (meta/collect ns-meta var-infos m ::c/sub ->kw-var)))))

(deftest ->derive-test
  (let [f (meta/->derive ::foo)]
    (is (= `(derive ::bar ::foo) (f [::bar 'a])))))
