(ns castorocauda.core
  (:require [castorocauda.dom :as cdom]
            [castorocauda.timeline :refer [tl-merge tl-map]]))


(defn launch-app
  "Map renderer to the state.
   `state-gen` is a nullary function that produces a map of keyword to FStream(s).
   `render-all` is a function that takes a snapshot of the state
     and returns a hiccup style representation of the DOM.
     It should render everything the streams in the state refers to.
   `base-el` is a HTMLElement that castorocauda renders the html
    representationof the app."
  [state-gen render-all base-el]
  (cdom/gendom (render-all {}) base-el)
  (let [state (state-gen)
        st-keys (keys state)]
    (->> (vals state)
         (apply (partial tl-merge (comp flatten list)))

         (tl-map
          (fn [x] (into {} (map (fn [x y] [x y]) st-keys x))))

         (tl-map
          (fn [state]
            (cdom/gendom (render-all state) base-el))))))
