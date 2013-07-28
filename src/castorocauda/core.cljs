(ns castorocauda.core
  (:require [castorocauda.html :refer [html-delta]]
            [hiccups.runtime :as hiccupsrt])
  (:use-macros [hiccups.core :only [html]])
  (:use [castorocauda.monads :only [fetch-state set-state]]))


;;Current DOM represented in EDN
(def dom-edn (atom []))


(declare render-all)
(declare toplevel-el)


(defn logg
  [x]
  (let [dom (.getElementById js/document "castorocauda-console")
        txt (.-textContent dom)]
    (set! (.-textContent dom) (str txt (prn-str x)))))



(defn select-path-dom-
  [start-el [{:keys [tag index]} & rest-path :as path]]
  (if (empty? path)
    start-el
    (let [children (.-childNodes start-el)]
      (if (< (.-length children) 1)
        (do (.log js/console "fmm...") start-el)
        (select-path-dom- (aget children index) rest-path)))))



(defn select-path-dom [path]
  (select-path-dom- (toplevel-el) path))



(defn propagate-dom-change
  [deltas]
  (doseq [[typ path a b] deltas]
    (let [node (select-path-dom path)]
      (case typ
        :html
        (set! (.-innerHTML node) (html a))

        :att
        (.setAttribute node (name a) (str b))

        :rem-att
        (.removeAttribute node (name a))))))




(defn gendom
  [dom st]
  (swap! dom-edn
         (fn [old-dom]
           (let [new-dom (render-all st)]
             (propagate-dom-change
              (html-delta  old-dom new-dom [] 0))
             new-dom))))




(def castorocauda-m
  {:m-result
   (fn [v]
     (fn [st]
       (gendom dom-edn st)
       [v st]))

   :m-bind
   (fn [mv f]
     (fn [st]
       (let [[v new-st] (mv st)]
         (gendom dom-edn st)
         ((f v) new-st))))})




(defn launch-app
  [renderer toplevel]
  (def render-all renderer)
  (defn toplevel-el []
    toplevel))
