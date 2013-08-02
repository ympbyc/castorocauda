(ns castorocauda.dom
  (:require [castorocauda.html :refer [html-delta wrap-tags]]
            [dommy.core :as dommy_]
            [goog.dom :as gdom]
            [goog.fx.dom :as domfx]
            [goog.events :as gevents]
            [hiccups.runtime :as hiccupsrt])
  (:require-macros [hiccups.core :as hiccups]
                   [dommy.macros :as dommy]))


(defn prn-log
  [x]
  (.log js/console (prn-str x))
  x)

(def ->vec (comp js->clj goog.array.toArray))


;;Current DOM represented in EDN
(def dom-edn (atom nil))


(defn- select-path-dom
  "`HTMLElement`
   -> `[{:index Int :tag Keyword}]`
   -> `HTMLElement`"
  [start-el [{:keys [tag index]} & rest-path :as path]]
  (if (empty? path)
    start-el
    (let [children (->> start-el .-childNodes ->vec)]
      (assert (> (count children) 0))
      (select-path-dom (get children index) rest-path))))


(defn glow [node]
  (let [white (array 255 255 255)
        color (array 188 237 128)
        anim (domfx/BgColorTransform. node white color 500)]
    (gevents/listen anim goog/fx.Transition.EventType.END
                    (fn []
                      (.play (domfx/BgColorTransform. node color white 500))))
    (.play anim)))


(defn- propagate-dom-change
  "deltas :: `[:html    path hiccup nil]`
           | `[:att     path attr-name attr-value]`
           | `[:rem-att path attr-name]`
           | `[:append  path hiccup nil]`
           | `[:remove  path nil index]`
           | `[:swap    path hiccup nil]`
           | `[:nodeValue path String nil]`"
  [deltas base-el]
  (prn-log (str "DELTAS:" (prn-str deltas)))
  (let [sel-path-dom (memoize (partial select-path-dom base-el))
        get-children (memoize (fn [node] (->vec (.-childNodes node))))]
    (doseq [[typ path a b :as delta] deltas]
      (let [node (sel-path-dom path)]
        ;;(.log js/console node)
        (case typ
          :html
          (set! (.-innerHTML node) (hiccups/html a))

          :att
          (.setAttribute node (name a) (str b))

          :rem-att
          (.removeAttribute node (name a))

          :append
          (gdom/append node (dommy/node a))

          :remove
          (gdom/removeNode (get (get-children node) b))

          :swap
          (gdom/replaceNode (dommy/node a) node)

          :nodeValue
          (set! (.-nodeValue node) a))

        ;;glow affected node red for a while
        (if (gdom/isElement node)
          (glow node)
          (some-> node .-parentNode glow))))))


(defn- gendom
  "new-dom :: `hiccup`,
   base-el :: `HTMLElement`"
  [new-dom base-el]
  (swap! dom-edn
         (fn [old-dom]
           (prn-log {:OLD old-dom
                     :NEW new-dom})
           (propagate-dom-change
            (html-delta old-dom new-dom [] 0)
            base-el)
           new-dom)))
