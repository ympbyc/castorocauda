(ns castorocauda.util
  (:use [castorocauda.timeline :only [timeline tl-cons!]]
        [goog.dom :only [getParentElement]]))

;;; This file isn't what castorocauda uses.
;;; It is here for your convenience.

(def ->vec (comp js->clj goog.array.toArray))


(defn dom-ready [fun]
  (set! (.-onload js/window) fun))

(defn q-select
  ([q]
     (.querySelector js/document q))
  ([q el]
     (.querySelector el q)))


(defn q-select-all
  ([q]
     (.querySelectorAll js/document q))
  ([q el]
     (.querySelectorAll el q)))


(defn prn-log [x]
  (.log js/console (prn-str x)))



(defn selector-match? [el sel]
  (some (partial = el) (->vec (q-select-all sel (getParentElement el)))))



(defn dom-element-events
  [el event-name]
  (let [tl (timeline)]
    (.addEventListener
     el event-name
     (fn [e] (tl-cons! e tl)))
    tl))


(defn dom-delegated-events
  "delegated events can be captured by specifying a selector query or a fn"
  ([event-name sel]
     (dom-delegated-events document/body event-name sel))
  ([el event-name sel]
     (let [tl (timeline)]
       (.addEventListener
        el event-name
        (fn [e]
          (when
              (if (string? sel)
                (selector-match? (.-target e) sel)
                (sel e))
            (tl-cons! e tl))))
       tl)))
