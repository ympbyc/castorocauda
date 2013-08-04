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
     (q-select-all q js/document))
  ([q el]
     (->vec (.querySelectorAll el q))))


(defn prn-log [& xs]
  (.log js/console (apply prn-str xs)))



(defn selector-match? [el sel]
  (some (partial = el) (q-select-all sel (getParentElement el))))


(defn delayed-fn [msec f]
  (fn [& args]
    (js/setTimeout #(apply f args) msec)))



(defn dom-element-events
  [event-name el]
  (let [tl (timeline)]
    (.addEventListener
     el event-name
     (fn [e] (tl-cons! e tl)))
    tl))


(defn dom-delegated-events
  "delegated events can be captured by specifying a selector query or a fn"
  ([event-name sel]
     (dom-delegated-events event-name sel document/body))
  ([event-name sel el]
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
