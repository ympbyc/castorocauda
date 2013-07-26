;;*CLJSBUILD-MACRO-FILE*;

(ns castorocauda.monads-macro
  "Minimum definition of monadic framework. Should be made separate later.")

(defmacro domonad
  [monad
   [sym exp & bindings :as bs]
   expr]
  (if (empty? bs)
    `((~monad :m-result) ~expr)
    `((~monad :m-bind) ~exp
      (fn [~sym]
        (domonad ~monad
                 ~bindings
                 ~expr)))))


(defmacro defn-m
  [name monad & body]
  (let [x (gensym)]
    `(defn ~name [~x]
       ((domonad ~monad
                 ~@body) ~x))))
