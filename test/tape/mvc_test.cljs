(ns tape.mvc-test
  (:require [cljs.test :refer [deftest is are run-tests]]
            [tape.mvc :as mvc :include-macros true]))

(deftest require-modules-test
  (is (= '(do (require 'tape.mvc.app.basic.view)
              (require 'tape.mvc.app.basic.controller)
              (require 'tape.mvc.app.named.controller)
              (require 'tape.mvc.app.input.controller))
         (macroexpand '(mvc/require-modules "test/tape/mvc/app/")))))

(mvc/require-modules "test/tape/mvc/app/")

(deftest modules-discovery-test
  (is (= {:routes [["/" :tape.mvc.app.basic.controller/home
                    "/event-db" :tape.mvc.app.basic.controller/event-db]
                   nil
                   ["/foo" :tape.mvc.app.input.controller/foo]]
          :modules {:tape.mvc.app.named.controller/module nil
                    :tape.mvc.app.input.controller/module nil
                    :tape.mvc.app.basic.controller/module nil
                    :tape.mvc.app.basic.view/module nil}}
         (mvc/modules-discovery "test/tape/mvc/app/"))))
