(ns castorocauda.example
  (:require [castorocauda.core :as castorocauda])
  (:use [castorocauda.dom  :only [dom-ready q-select]]
        [castorocauda.stream :only [mk-fstream map-fstream merge-fstream cons-fstream dom-events]])
  (:use-macros [castorocauda.macros :only [run-app]]))


(defn render-all [{:keys [a b result]
                   :or   {a 0, b 0, result 0}}]
  [:div#example-app
   [:h1 (str a "+" b "=" result)]
   [:input#a-in {:value a}]
   [:input#b-in {:value b}]])


(defn val-stream
  [el]
  (->>
   (dom-events el "keyup")
   (map-fstream (fn [e]
                  (js/parseInt (.-value (.-target e)))))))


(defn main []
  (run-app
   (let [a-stream (val-stream (q-select "#a-in"))
         b-stream (val-stream (q-select "#b-in"))]
     {:a      a-stream
      :b      b-stream
      :result (merge-fstream + a-stream b-stream)})
   render-all
   (q-select "#castorocauda")))


(dom-ready main)
