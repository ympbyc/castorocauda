;*CLJSBUILD-MACRO-FILE*;
(ns test.test-macro)

(defmacro qtest
  [name & body]
  `(js/test ~name
            (fn []
              ~@body)))

(defmacro async-test
  [name n & body]
  `(js/asyncTest ~name ~n
                 (fn []
                   ~@body)))

(defmacro go-resume
  [& body]
  (cons 'go (concat body '((js/start)))))
