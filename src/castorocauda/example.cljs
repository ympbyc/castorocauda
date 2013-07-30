(ns castorocauda.example
  (:require [castorocauda.core :as castorocauda])
  (:use [castorocauda.dom  :only [dom-ready q-select]]
        [castorocauda.timeline :only [tl-map tl-filter tl-merge dom-events]])
  (:use-macros [castorocauda.macros :only [run-app]]))


(defn render-all [{:keys [a b result]
                   :or   {a 0, b 0, result 0}}]
  [:div#example-app
   [:h1 (str a "+" b "=" result)]
   [:input#a-in {:value a}]
   [:input#b-in {:value b}]
   (if (> result 5) [:div
                [:a {:href "..."} "clojure"]
                [:p "5以上"]
                "A wild TextNode appears"
                [:p "OK"]]
       [:div "5以下"])])


(defn val-stream
  [el]
  (->>
   (dom-events el "keyup")
   (tl-map (fn [e]
             (js/parseInt (.-value (.-target e)))))
   (tl-filter (comp not js/isNaN))))


(defn main []
  (run-app
   (let [a-stream (val-stream (q-select "#a-in"))
         b-stream (val-stream (q-select "#b-in"))]
     {:a      a-stream
      :b      b-stream
      :result (tl-filter identity (tl-merge + a-stream b-stream))})
   render-all
   (q-select "#castorocauda")))


(dom-ready main)
