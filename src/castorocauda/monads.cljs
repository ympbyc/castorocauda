(ns castorocauda.monads
  "Simple monads")



;;;State
(def state-m
  {:m-result
   (fn [v]
     (fn [st] [v st]))

   :m-bind
   (fn [mv f]
     (fn [st]
       (let [[v new-st] (mv st)]
         ((f v) new-st))))})


(defn patch-state [f]
  (fn [st]
    [st (f st)]))


(defn fetch-state []
  (fn [state]
    [state state]))

(defn set-state [new-state]
  (fn [old-state]
    [old-state new-state]))





;;; Maybe
(def maybe-m
  {:m-result identity
   :m-bind
   (fn [mv f]
     (if (nil? mv) nil (f mv)))})


;;; Sequence
(def seq-m
  {:m-result list
   :m-bind
   (fn [mv f]
     (flatten (map f mv)))})
