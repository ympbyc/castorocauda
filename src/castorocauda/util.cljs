(ns castorocauda.util
  (:use [castorocauda.timeline :only [timeline tl-cons!]]
        [goog.dom :only [getParentElement]]))

;;;; This file isn't what castorocauda uses.
;;;; It is here for your convenience.

;;Convert array-like objects into a cljs vector
(def ->vec (comp js->clj goog.array.toArray))


(defn dom-ready
  "Delay excecution of the function until the DOM has been fixed"
  [fun]
  (set! (.-onload js/window) fun))

(defn q-select
  "Wrapper for document.querySelecot and element.querySelector"
  ([q]
     (.querySelector js/document q))
  ([q el]
     (.querySelector el q)))


(defn q-select-all
  "Wrapper for document.querySelectorAll and element.querySelectorAll"
  ([q]
     (q-select-all q js/document))
  ([q el]
     (->vec (.querySelectorAll el q))))


(defn prn-log [& xs]
  (.log js/console (apply prn-str xs))
  (first xs))



(defn selector-match?
  "Test if selector query matches the element"
  [el sel]
  (some (partial = el) (q-select-all sel (getParentElement el))))


(defn delayed-fn
  "Wrapper for js/setTimeout"
  [msec f]
  (fn [& args]
    (js/setTimeout #(apply f args) msec)))


(defn delayed
  "Poor man's TCO for side-effect-ful operations.
   Use js/setTimeout to wait for the clearance of call stack."
  [f & args]
  (js/setTimeout #(apply f args) 0))


(defn dom-element-events
  "A timeline of events occured at the element"
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
