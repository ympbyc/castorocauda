(ns castorocauda.dom
  (:require [castorocauda.html :refer [html-delta wrap-tags]]
            [crate.core :as crate]
            [goog.dom :as gdom]
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
  (set! (.-backgroundColor (.-style node)) "rgba(225,101,98,0.5)")
  (set! (.-border (.-style node)) "1px solid rgb(225,101,98)")
  (js/setTimeout
   (fn [] (set! (.-backgroundColor (.-style node)) "")
     (set! (.-border (.-style node)) "")) 1000))


(defn- propagate-dom-change
  "deltas :: [:html   path hiccup nil]
           | [:att    path attr-name attr-value]
           | [rem-att path attr-name]"
  [deltas base-el]
  (let [created-els (atom [])] ;;<-- TODO: recur
    (.log js/console "v----------- Delta Application -----------v")
    (doseq [[typ path a b :as delta] deltas]
      (let [node (select-path-dom base-el path)]
        (prn-log (str "DELTA:" (prn-str delta)))
        (.log js/console node)
        (case typ
          :html
          (set! (.-innerHTML node) (hiccups/html a))

          :html-child   ;;targeted in-place update
          (do
            (cond (or (nil? a) (string? a))
                  (gdom/replaceNode (gdom/createTextNode a) (aget (.-childNodes node) b))

                  :else
                  (gdom/replaceNode (crate/html a) (aget (.-childNodes node) b)))
            (swap! created-els (partial cons path)))

          :html-children
          (do
            (if (empty? (filter #(= path %) @created-els))
              ;;if wrapper element hasn't already been created
              (set! (.-innerHTML node) (hiccups/html a))

              ;;otherwise, append child to the wrapper element
              (let [el (crate/html a)]
                (gdom/append  node el)))
            (swap! created-els (partial cons path)))

          :att
          (.setAttribute node (name a) (str b))

          :rem-att
          (.removeAttribute node (name a)))

        ;;glow affected node red for a while
        (if (gdom/isElement node) (glow-red node))))
    (.log js/console "^----------- Delta Application -----------^")))


(defn- gendom
  "new-dom :: hiccup
   base-el :: HTMLElement"
  [new-dom base-el]
  (swap! dom-edn
         (fn [old-dom]
           (propagate-dom-change
            (html-delta  old-dom new-dom [] 0)
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
