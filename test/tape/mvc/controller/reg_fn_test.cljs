(ns tape.mvc.controller.reg-fn-test
  (:require [cljs.test :refer [deftest are]]
            [tape.mvc.controller.reg-fn :as reg-fn]))

(deftest reg-fn-&-subscribe-test
  (reg-fn/reg-fn! ::my (constantly 42))
  (reg-fn/reg-fn! ::inc #(inc %))
  (reg-fn/reg-fn! ::sum #(+ %1 %2))
  (are [x y] (= x y)
    42 (reg-fn/subscribe [::my])
    3 (reg-fn/subscribe [::inc 2])
    7 (reg-fn/subscribe [::sum 3 4]))
  (reset! reg-fn/id->handler {}))
