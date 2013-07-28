(ns castorocauda.dom
  (:require [castorocauda.html :refer [html-delta]]
            [hiccups.runtime :as hiccupsrt])
  (:use-macros [hiccups.core :only [html]]))


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



(defn- propagate-dom-change
  "deltas :: [:html   path hiccup nil]
           | [:att    path attr-name attr-value]
           | [rem-att path attr-name]"
  [deltas base-el]
  (doseq [[typ path a b] deltas]
    (let [node (select-path-dom base-el path)]
      (case typ
        :html
        (set! (.-innerHTML node) (html a))

        :att
        (.setAttribute node (name a) (str b))

        :rem-att
        (.removeAttribute node (name a))))))


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
