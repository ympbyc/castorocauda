(ns castorocauda.timeline)


;;;; timeline.cljs
;;;; A Deadly Naive Implementation of FRP Stream.
;;;; You may probably not want to use this feature quite yet.


(def default-timeline-size-limit 100)
(def default-timeline-resetable  true)


(defprotocol FRPStream
  (update! [this item] "item become available to the consumer")
  (current-value [this] "returns the newest state")
  (subscribe [this f] "register a fn that get called upon update!"))


(declare tl-cons! tl-now tl-map)

(defrecord Timeline [stream size-limit resetable count]
  FRPStream
  (update! [this item]  (tl-cons! item this))
  (current-value [this] (tl-now this))
  (subscribe [this f]   (tl-map f this)))


(defn ->timeline
  [xs & {:keys [size resetable]
         :or   {size default-timeline-size-limit
                resetable default-timeline-resetable}}]
  (->Timeline (atom (seq xs))  size resetable (atom (count xs))))

(defn timeline
  "Create a timeline. A timeline is an atom of seq wrapped in a record."
  ([] (->timeline '()))
  ([& xs]
     (->> xs seq ->timeline)))


(def timeline? (partial instance? Timeline))


(defn tl-deref [tl]
  "Dereference a seq from the timeline"
  (->> tl :stream deref))


(def tl-now (comp first tl-deref))


(defn delayed
  "Duplicate definition is in util.cljs.
   Poor man's TCO for side-effect-ful operations.
   Use js/setTimeout to wait for the clearance of call stack."
  [f & args]
  (js/setTimeout #(apply f args) 0))


(defn- adjust-after-cons!
  "Adjust a timeline after consing based on its config.
   This step is needed to avoid memory leak"
  [{:keys [stream size-limit resetable count] :as tl}]
  (cond (< @count size-limit)
        tl

        resetable
        (do
          (swap! stream (comp list first))
          (reset! count 1)
          tl)

        :else    ;;this is super inefficient
        (do
          (swap! stream (partial take size-limit))
          (reset! count size-limit)
          tl)))


(defn tl-cons!
  "Unshift a value into a timeline"
  [x tl]
  (swap! (:stream tl) (partial cons x))
  (swap! (:count tl) inc)
  (delayed adjust-after-cons! tl)
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
    (sync-tl (comp f first) src-tl dst-tl)
    dst-tl))


(defn tl-merge
  "Create a new timeline by merging one or more timelines
   into one stream using the given function."
  [f & sources]
  (let [dst-tl (->> (apply (partial map f) (map tl-deref sources))
                    ->timeline)]
    (doseq [src-tl sources]
      (sync-tl #(apply f (map (comp first tl-deref) sources))
               src-tl dst-tl))
    dst-tl))


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




;;;; Lifted functions
;;;; Each function defined below will produces a timeline of streams

(def tl-of-rest    (tl-lift rest))

(def tl-of-take    (tl-lift take))

(def tl-of-drop    (tl-lift take))

(def tl-of-reduce  (tl-lift reduce))

(def tl-of-count   (tl-lift count))

(def tl-of-cons    (tl-lift cons))

(def tl-of-concat  (tl-lift concat))

(def tl-of-reverse (tl-lift reverse))

(def tl-of-map     (tl-lift map))

(def tl-of-filter  (tl-lift filter))
