(ns tape.mvc.meta-test
  (:require [clojure.test :refer :all]
            [cljs.analyzer.api :as api]
            [tape.mvc.meta :as meta]
            [tape.mvc.controller :as c]))

(deftest ns-meta-test
  (let [ns-name (with-meta 'my-ns {::c/signals      'signals
                                   ::c/interceptors 'interceptors
                                   :x               'a})]
    (with-redefs [api/find-ns (constantly {:name ns-name})]
      (is (= {::c/signals      'signals
              ::c/interceptors 'interceptors}
             (meta/ns-meta #'c/controller-ns? 'my-ns))))))

(deftest ->kw-fn-test
  (let [m         {::c/signals 'signals}
        var-info  {:name 'baz :meta {::c/sub true}}
        var-info2 {:name 'baz :meta {::c/sub :foo.bar/qux}}]
    (is (= [:foo.bar/baz '(clojure.core/with-meta baz {::c/signals signals
                                                       ::c/sub     true})]
           (meta/->kw-fn "foo.bar" m var-info)))
    (is (= [:foo.bar/baz '(clojure.core/with-meta baz {::c/signals signals
                                                       ::c/sub     :foo.bar/qux})]
           (meta/->kw-fn "foo.bar" m var-info2)))))

(deftest ->kw-reg-test
  (let [m         {:x :a}
        var-info  {:name 'baz :meta {::c/sub true, ::c/signals 'signals}}
        var-info2 {:name 'baz :meta {::c/sub :foo.bar/qux, ::c/signals 'signals}}]
    (is (= [:foo.bar/baz '(clojure.core/with-meta
                           baz {:x :a, ::c/signals signals, ::c/sub true})]
           (meta/->kw-reg ::missing "foo.bar" m var-info)))
    (is (= [:foo.bar/baz '(clojure.core/with-meta
                           [signals baz]
                           {:x :a, ::c/signals signals, ::c/sub true})]
           (meta/->kw-reg ::c/signals "foo.bar" m var-info)))
    (is (= [:foo.bar/baz '(clojure.core/with-meta
                           [signals baz]
                           {:x :a, ::c/signals signals, ::c/sub :foo.bar/qux})]
           (meta/->kw-reg ::c/signals "foo.bar" m var-info2)))))

(deftest collect-test
  (let [ns-meta   {:x :a}
        var-infos [{:name 'qux :meta {::c/sub true}}
                   {:name 'quux :meta {::c/signals 'signals
                                       ::c/sub     :bar.baz/zub}}]
        m         {:y :b}
        ->kw-reg' (partial meta/->kw-reg ::c/signals "bar.baz")]
    (is (= '#:bar.baz{:qux  (clojure.core/with-meta
                             qux {:x :a, :y :b, ::c/sub true})
                      :quux (clojure.core/with-meta
                             [signals quux]
                             {:x :a, :y :b, ::c/sub :bar.baz/zub, ::c/signals signals})}
           (meta/collect ns-meta var-infos m ::c/sub ->kw-reg')))))

(deftest ->derive-test
  (let [f (meta/->derive ::foo)]
    (is (= `(derive ::bar ::foo) (f [::bar 'a])))))
