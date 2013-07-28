(ns castorocauda.stream)


(defn mk-fstream
  "Create a stream. A stream is just an atom of seq."
  [& xs]
  (atom (seq xs)))

(defn cons-fstream
  "Unshift a value into a fstream."
  [x stream]
  (swap! stream (fn [xs] (cons x xs))))

(defn- sync-fstream-
  [f src-s dst-s pred]
  (add-watch src-s (gensym)
             (fn [k r os [x & _ :as ns]]
               (when (pred x)
                 (cons-fstream (f x) dst-s)))))

(defn- sync-fstream
  ([f src-s dst-s]
     (sync-fstream- f src-s dst-s (fn [_] true)))
  ([f src-s dst-s pred]
     (sync-fstream- f src-s dst-s pred)))


(defn map-fstream
  "Create a new fstream by mapping a function on a fstream.
   Unlike core/map this fn only takes one fstream."
  [f src-s]
  (let [dst-s (atom (map f @src-s))]
    (sync-fstream (fn [new-head] (f new-head))
                  src-s dst-s)
    dst-s))


(defn- merge-fstream-2
  "Helper for merge-fstream. Does the actual work of merging."
  [f s1 s2]
  (let [dst-s (atom (map f @s1 @s2))]
    (sync-fstream (fn [new-head] (f new-head (first @s2)))
                  s1 dst-s)
    (sync-fstream (fn [new-head] (f (first @s1) new-head))
                  s2 dst-s)
    dst-s))

(defn merge-fstream
  "Create a new fstream by merging one or more fstreams
   into one stream using the given function."
  [f source & sources]
  (reduce (fn [strm src]
            (merge-fstream-2 f strm src))
          source sources))


(defn filter-fstream
  "Create a new fstream by selecting certain items from a fstream"
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




;;; Consideration ;;;

(comment deftype FStream [stream]
  Seq
  (first [xs]   (first @xs))
  (rest  [xs]   (rest  @xs))
  (cons  [x xs] (swap! xs (fn [xs] (cons x xs)))))
