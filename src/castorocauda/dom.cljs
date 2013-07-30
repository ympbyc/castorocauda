(ns castorocauda.dom
  (:require [castorocauda.html :refer [html-delta wrap-tags]]
            [hiccups.runtime :as hiccupsrt])
  (:require-macros [hiccups.core :as hics]))


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



(defn- propagate-dom-change
  "deltas :: [:html   path hiccup nil]
           | [:att    path attr-name attr-value]
           | [rem-att path attr-name]"
  [deltas base-el]
  (let [created-els (atom [])] ;;<-- TODO: recur
    (doseq [[typ path a b :as delta] deltas]
      ;;(prn-log (str "DELTA: " delta))
      ;;(prn-log (->> (wrap-tags a) (drop 1) first))
      ;;(prn-log path)
      (let [path (rest path)
            node (select-path-dom base-el path)]
        (.log js/console node)
        (prn-log delta)
        (case typ
          :html
          (set! (.-innerHTML node) (hics/html (->> (wrap-tags a) (drop 1) first)))

          :child-html
          (do
            (if (empty? (filter #(= path %) @created-els))
              ;;if wrapper element hasn't already been created
              (set! (.-innerHTML node) (hics/html (wrap-tags a)))

              ;;otherwise, append child to the wrapper element
              (let [w-node   (aget (.-childNodes node) 0)         ;;the wrapper
                    html-str (hics/html (wrap-tags a))
                    doc      (.parseFromString (js/DOMParser.) html-str "application/xml")]
                (.appendChild w-node
                              (.-firstChild doc))))
            (swap! created-els (partial cons path)))

          :att
          (.setAttribute node (name a) (str b))

          :rem-att
          (.removeAttribute node (name a)))))))


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
