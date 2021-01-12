(ns tape.mvc-test
  (:require [clojure.test :refer :all]
            [tape.mvc :as mvc]))

(deftest ->pair-test
  (let [var-info {:name 'foo.bar/baz
                  :meta #::mvc{:signals 'signals,
                               :reg ::mvc/sub}}
        var-info2 {:name 'foo.bar/baz
                   :meta #::mvc{:signals 'signals,
                                :reg ::mvc/sub,
                                :id :foo.bar/qux}}]
    (is (= [[:foo.bar/baz ::mvc/sub]
            '(clojure.core/with-meta
              baz #::mvc{:signals signals
                         :reg ::mvc/sub})]
           (#'mvc/->pair var-info)))
    (is (= [[:foo.bar/baz ::mvc/sub]
            '(clojure.core/with-meta
              baz #::mvc{:signals signals
                         :reg ::mvc/sub
                         :id :foo.bar/qux})]
           (#'mvc/->pair var-info2)))))

(def ^:private var-infos
  [{:name 'bar.baz/qux
    :meta #::mvc{:reg ::mvc/sub}}
   {:name 'bar.baz/quux
    :meta #::mvc {:reg ::mvc/sub
                  :id :bar.baz/zub
                  :signals 'signals}}])

(deftest config-test
  (let [extra-meta {::mvc/controller-ns-str "a.b.controller"}]
    (is (= '{[:bar.baz/quux ::mvc/sub]
             (clojure.core/with-meta
              quux #:tape.mvc{:reg ::mvc/sub
                              :id :bar.baz/zub
                              :signals signals
                              :controller-ns-str "a.b.controller"})
             [:bar.baz/qux ::mvc/sub]
             (clojure.core/with-meta
              qux #:tape.mvc{:reg ::mvc/sub
                             :controller-ns-str "a.b.controller"})}
           (#'mvc/config extra-meta var-infos)))))
