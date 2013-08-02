(ns castorocauda.timeline)


(defrecord Timeline [stream])


(defn ->timeline
  [xs]
  (->Timeline (atom (seq xs))))

(defn timeline
  "Create a timeline. A timeline is just an atom of seq."
  ([] (->Timeline (atom '())))
  ([& xs]
     (->> xs seq ->timeline)))


(defn tl-deref [tl]
  (->> tl :stream deref))


(defn tl-cons!
  "Unshift a value into a timeline"
  [x tl]
  (swap! (:stream tl) (partial cons x))
  tl)

(defn- sync-tl-
  [f src-tl dst-tl pred]
  (add-watch (:stream src-tl) (gensym)
             (fn [k r os [x & _ :as ns]]
               (when (pred x)
                 (tl-cons! (f ns) dst-tl))))
  nil)

(defn- sync-tl
  ([f src-tl dst-tl]
     (sync-tl- f src-tl dst-tl (fn [_] true)))
  ([f src-tl dst-tl pred]
     (sync-tl- f src-tl dst-tl pred)))


(defn tl-map
  "Create a new timeline by mapping a function on a timeline.
   Unlike core/map this fn only takes one timeline."
  [f src-tl]
  (let [dst-tl (->> src-tl tl-deref (map f) ->timeline)]
    (sync-tl (comp f first)src-tl dst-tl)
    dst-tl))


(defn- tl-merge-2
  "Helper for merge-timeline. Does the actual work of merging."
  [f s1 s2]
  (let [dst-tl (->> (map f (tl-deref s1) (tl-deref s2)) ->timeline)]
    (sync-tl (fn [new-s] (f (first new-s) (first (tl-deref s2))))
             s1 dst-tl)
    (sync-tl (fn [new-s] (f (first (tl-deref s1)) (first new-s)))
             s2 dst-tl)
    dst-tl))

(defn tl-merge
  "Create a new timeline by merging one or more timelines
   into one stream using the given function."
  [f source & sources]
  (reduce (fn [strm src]
            (tl-merge-2 f strm src))
          source sources))


(defn tl-filter
  "Create a new timeline by selecting certain items from a timeline"
  [f src-tl]
  (let [dst-tl (->> src-tl tl-deref (filter f) ->timeline)]
    (sync-tl first
             src-tl dst-tl
             (fn [x] (f x)))
    dst-tl))


(defn tl-apply
  "Apply a sequence function to a tl.
  The produced tl is a stream of sequences"
  [f src-tl]
  (let [dst-tl (->> src-tl tl-deref f timeline)]
    (sync-tl f src-tl dst-tl)
    dst-tl))


(defn tl-lift
  "Lift normal sequence function to timeline function.
  The produced tl is a stream of sequences"
  [f]
  (fn [& args]
    (let [src-tl (last args)
          args   (butlast args)]
      (tl-apply #(apply f (concat args (list %))) src-tl))))




;;;; Lifted functions ;;;;;

(def tl-of-take (tl-lift take))

(def tl-of-drop (tl-lift take))

(def tl-of-reduce (tl-lift reduce))

(def tl-of-count (tl-lift count))

(def tl-of-cons (tl-lift cons))

(def tl-of-concat (tl-lift concat))

(def tl-of-reverse (tl-lift reverse))

(def tl-of-map (tl-lift map))

(def tl-of-filter (tl-lift filter))
