(ns castorocauda.dom
  (:require [castorocauda.html :refer [html-delta wrap-tags]]
            [crate.core :as crate]
            [goog.dom :as gdom]
            [goog.fx.dom :as domfx]
            [goog.events :as gevents]
            [hiccups.runtime :as hiccupsrt])
  (:require-macros [hiccups.core :as hiccups]))


(defn prn-log
  [x]
  (.log js/console (prn-str x))
  x)

;;Current DOM represented in EDN
(def dom-edn (atom []))


(defn- select-path-dom
  "start-el :: HTMLElement
   path :: [{:index Int :tag Keyword}]
   -> HTMLElement"
  [start-el [{:keys [tag index]} & rest-path :as path]]
  (if (empty? path)
    start-el
    (let [children (.-childNodes start-el)]
      (if (< (.-length children) 1)
        (do (.log js/console "fmm...") start-el)
        (select-path-dom (aget children index) rest-path)))))

(defn glow-red [node]
  (let [white (array 255 255 255)
        color (array 188 237 128)
        anim (domfx/BgColorTransform. node white color 500)]
    (gevents/listen anim goog/fx.Transition.EventType.END
                    (fn []
                      (.play (domfx/BgColorTransform. node color white 500))))
    (.play anim)))


(defn ->vec
  [x]
  (js->clj (.call js/Array.prototype.slice x)))


(defn- propagate-dom-change
  "deltas :: [:html   path hiccup nil]
           | [:att    path attr-name attr-value]
           | [rem-att path attr-name]"
  [deltas base-el]
  (let [rm-paths (atom [])
        sel-path-dom (memoize (partial select-path-dom base-el))
        get-children (memoize (fn [node] (->vec (.-childNodes node))))]
    (doseq [[typ path a b :as delta] deltas]
      (let [node (sel-path-dom path)]
        ;(prn-log (str "DELTA:" (prn-str delta)))
        ;(.log js/console node)
        (case typ
          :html
          (set! (.-innerHTML node) (hiccups/html a))

          :att
          (.setAttribute node (name a) (str b))

          :rem-att
          (.removeAttribute node (name a))

          :append
          (gdom/append node (crate/html a))

          :remove
          (gdom/removeNode (get (get-children node) b))

          :swap
          (gdom/replaceNode (crate/html a) node)

          :nodeValue
          ;;(set! (.-nodeValue node) a)
          (gdom/setTextContent node a))

        ;;glow affected node red for a while
        (if (gdom/isElement node)
          (glow-red node)
          (some-> node .-parentNode glow-red))))))


(defn- gendom
  "new-dom :: hiccup
   base-el :: HTMLElement"
  [new-dom base-el]
  (swap! dom-edn
         (fn [old-dom]
           (propagate-dom-change
            (html-delta old-dom new-dom [] 0)
            base-el)
           new-dom)))

;; util

;;todo separate file


(defn dom-ready [fun]
  (set! (.-onload js/window) fun))

(defn q-select [q]
  (.querySelector js/document q))


(defn q-select-all [q]
  (.querySelectorAll js/document q))
