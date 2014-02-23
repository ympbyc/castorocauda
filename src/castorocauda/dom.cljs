(ns castorocauda.dom
  (:require [castorocauda.html :refer [html-delta wrap-tags]]
            [dommy.core :as dommy_]
            [goog.dom :as gdom]
            [goog.fx.dom :as domfx]
            [goog.events :as gevents]
            [hiccups.runtime :as hiccupsrt])
  (:require-macros [hiccups.core :as hiccups]
                   [dommy.macros :as dommy]))


(def ->vec (comp js->clj goog.array.toArray))


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


(defn glow
  "For manual testing. Glow the selected node green for a while"
  [node]
  (let [white (array 255 255 255)
        color (array 188 237 128)
        anim (domfx/BgColorTransform. node white color 500)]
    (gevents/listen anim goog/fx.Transition.EventType.END
                    (fn []
                      (.play (domfx/BgColorTransform. node color white 500))))
    (.play anim)))


(defn style->str
  "Generate a style string from map
   e.g.)  {:background \"red\" :width \"20px\"}
       -> \"background: red; width: 20px; \""
  [style]
  (if (string? style) style
      (apply str
             (for [[k v] style]
               (str (name k) ": " v "; ")))))


(defn- propagate-dom-change
  "deltas :: `[:html    path hiccup nil]`
           | `[:att     path attr-name attr-value]`
           | `[:rem-att path attr-name]`
           | `[:append  path hiccup nil]`
           | `[:remove  path nil index]`
           | `[:swap    path hiccup nil]`
           | `[:nodeValue path String nil]`"
  [deltas base-el]
  (let [sel-path-dom (memoize (partial select-path-dom base-el))
        get-children (memoize (fn [node] (->vec (.-childNodes node))))]
    (doseq [[typ path a b :as delta] deltas]
      (let [node (sel-path-dom path)]
        ;;(.log js/console node)
        (case typ
          :html
          (set! (.-innerHTML node) (hiccups/html a))

          :att
          (if (= a :style)
            (.setAttribute node (name a) (style->str b))
            (.setAttribute node (name a) (str b)))

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

        ;;glow affected node green for a while
        (comment if (gdom/isElement node)
          (glow node)
          (some-> node .-parentNode glow))))))


(defn gendom
  "base-el :: `HTMLElement`
   old-edn :: hiccup-style EDN representing DOM
   new-edn :: hiccup-style EDN representing DOM"
  [old-edn new-edn base-el]
  (propagate-dom-change
   (html-delta old-edn new-edn [] 0)
   base-el)
  new-edn)
