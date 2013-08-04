;*CLJSBUILD-MACRO-FILE*;

(ns castorocauda.macros)


(defmacro run-app
  [state-gen render-all base-el]
  `(castorocauda/launch-app
    (fn []
      ~state-gen)
    ~render-all
    ~base-el))
