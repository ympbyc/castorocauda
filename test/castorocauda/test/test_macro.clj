;*CLJSBUILD-MACRO-FILE*;
(ns test.test-macro)

(defmacro qtest
  [name & body]
  `(js/test ~name
            (fn []
              ~@body)))
