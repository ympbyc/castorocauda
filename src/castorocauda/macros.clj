;*CLJSBUILD-MACRO-FILE*;

(ns castorocauda.macros)

(defmacro do-timeout [n & body]
  `(js/setTimeout
    (fn [] ~@body)
    ~n))
