(ns test.test)

(defn prn=
  [x y mes]
  (js/strictEqual (prn-str x) (prn-str y) mes))

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
