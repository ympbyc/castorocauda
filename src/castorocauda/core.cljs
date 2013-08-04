(ns castorocauda.core
  (:require [castorocauda.dom :as cdom]
            [castorocauda.timeline :refer [tl-merge tl-map]]))


(defn launch-app
  "Map renderer to the state.
   `state` is a map of keywords to Timelines.
   `render-all` is a function that takes a snapshot of the state
     and returns a hiccup style representation of the DOM.
     It should render everything the streams in the state refers to.
   `base-el` is a HTMLElement that castorocauda renders the html
    representationof the app."
  [state render-all base-el]
  (cdom/gendom (render-all {}) base-el)
  (let [st-keys (keys state)]

    (->> (vals state)

         (apply (partial tl-merge (fn [& xs] xs)))

         (tl-map
          (fn [st-val-tl] (into {} (map (fn [x y] [x y]) st-keys st-val-tl))))

         (tl-map
          (fn [state]
            (cdom/gendom (render-all state) base-el))))))
