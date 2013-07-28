(ns castorocauda.core
  (:require [castorocauda.dom :as cdom]))

;;Current State
;; -- use of this atom is not ideal
;;    because it forces castorocauda app to be singleton
(def app-state (atom {}))


(declare render-all)
(declare toplevel-el)


(defn logg
  [x]
  (let [dom (.getElementById js/document "castorocauda-console")
        txt (.-textContent dom)]
    (set! (.-textContent dom) (str txt (prn-str x)))))



(defn patch-state
  [f]
  (swap! app-state (fn [as] (merge as (f as))))
  (cdom/gendom (render-all @app-state) (toplevel-el)))



(defn on-click
  [el f]
  (.addEventListener el "click"
                     (fn [e]
                       (patch-state (partial f e)))))


(defn launch-app
  [state renderer base-el]
  (def render-all renderer)
  (defn toplevel-el []
    base-el)
  (reset! app-state state)
  (cdom/gendom (render-all @app-state) base-el))
