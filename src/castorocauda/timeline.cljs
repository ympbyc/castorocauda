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
  [f src-s dst-s pred]
  (add-watch (:stream src-s) (gensym)
             (fn [k r os [x & _ :as ns]]
               (when (pred x)
                 (tl-cons! (f x) dst-s))))
  nil)

(defn- sync-tl
  ([f src-s dst-s]
     (sync-tl- f src-s dst-s (fn [_] true)))
  ([f src-s dst-s pred]
     (sync-tl- f src-s dst-s pred)))


(defn tl-map
  "Create a new timeline by mapping a function on a timeline.
   Unlike core/map this fn only takes one timeline."
  [f src-s]
  (let [dst-s (->> src-s tl-deref (map f) ->timeline)]
    (sync-tl (fn [new-head] (f new-head))
             src-s dst-s)
    dst-s))


(defn- tl-merge-2
  "Helper for merge-timeline. Does the actual work of merging."
  [f s1 s2]
  (let [dst-s (->> (map f (tl-deref s1) (tl-deref s2)) ->timeline)]
    (sync-tl (fn [new-head] (f new-head (first (tl-deref s2))))
             s1 dst-s)
    (sync-tl (fn [new-head] (f (first (tl-deref s1)) new-head))
             s2 dst-s)
    dst-s))

(defn tl-merge
  "Create a new timeline by merging one or more timelines
   into one stream using the given function."
  [f source & sources]
  (reduce (fn [strm src]
            (tl-merge-2 f strm src))
          source sources))


(defn tl-filter
  "Create a new timeline by selecting certain items from a timeline"
  [f src-s]
  (let [dst-s (->> src-s tl-deref (filter f) ->timeline)]
    (sync-tl identity
             src-s dst-s
             (fn [x] (f x)))
    dst-s))




;;;; DOM ;;;;;

(defn dom-events
  [el ev]
  (let [strm (timeline)]
    (.addEventListener
     el ev
     (fn [e] (tl-cons! e strm)))
    strm))
