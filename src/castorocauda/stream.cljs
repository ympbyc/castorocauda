(ns castorocauda.stream
  )

(defn mk-fstream
  [& xs]
  (atom (seq xs)))

(defn cons-fstream
  [x stream]
  (swap! stream (fn [xs] (cons x xs))))

(defn- sync-fstream-
  [f src-s dst-s pred]
  (add-watch src-s (gensym)
             (fn [k r os [x & _ :as ns]]
               (when (pred x)
                 (cons-fstream (f x) dst-s)))))

(defn sync-fstream
  ([f src-s dst-s]
     (sync-fstream- f src-s dst-s (fn [_] true)))
  ([f src-s dst-s pred]
     (sync-fstream- f src-s dst-s pred)))


(defn map-fstream
  [f src-s]
  (let [dst-s (atom (map f @src-s))]
    (sync-fstream (fn [new-head] (f new-head))
                  src-s dst-s)
    dst-s))


(defn merge-fstream
  [f s1 s2]
  (let [dst-s (atom (map f @s1 @s2))]
    (sync-fstream (fn [new-head] (f new-head (first @s2)))
                  s1 dst-s)
    (sync-fstream (fn [new-head] (f (first @s1) new-head))
                  s2 dst-s)
    dst-s))


(defn filter-fstream
  [f src-s]
  (let [dst-s (atom (filter f @src-s))]
    (sync-fstream identity
                  src-s dst-s
                  (fn [x] (f x)))
    dst-s))




;;;; DOM ;;;;;

(defn dom-events
  [el ev]
  (let [strm (mk-fstream)]
      (.addEventListener
       el ev
       (fn [e] (cons-fstream e strm)))
      strm))
