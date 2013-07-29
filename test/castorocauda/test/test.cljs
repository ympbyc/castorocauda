(ns test.test)

(def se js/strictEqual)

(defn de
  [x y mes]
  (js/deepEqual (clj->js x)
                (clj->js y)
                mes))


(def ok js/ok)


(defn prn-log
  [x]
  (.log js/console (prn-str x))
  x)
