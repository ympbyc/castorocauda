(ns add-two-numbers.core
  (:use [castorocauda.core     :only [launch-app]]
        [castorocauda.util     :only [dom-ready q-select]]
        [castorocauda.timeline :only [tl-map tl-filter tl-merge dom-events]]))


(defn render-all
  "Map a snapshot of the app's state to an EDN representing DOM"
  [{:keys [a b result]
    :or   {a 0, b 0, result 0}}]
  [:div
   [:h2 (str a " + " b " = " result)]
   [:input#a-in {:type "number"}] " + "
   [:input#b-in {:type "number"}] " = " (str result)
   [:p "the result is "
    [:span (if (even? result) "even" "odd")]]
   (if (< result 5)
     [:div [:p {:style {:color "red"}} "result is less than 5"]]
     (list [:p {:style {:color "red"}} "result is greater than 5"]
           [:p "..."]))])


(defn val-timeline
  "Watch el for keyup and extract integer value from it"
  [el]
  (->> (dom-events el "keyup")                         ;;timeline of keyup
       (tl-map #(->> % .-target .-value js/parseInt))  ;;timeline of values
       (tl-filter (comp not js/isNaN))                 ;;reject invalid values
       ))


(defn main
  "launch-app takes:
   1. nullary function that produce a map of timelines
   2. the render-all function defined above
   3. a HTMLElement that Castorocauda renders its state in"
  []
  (launch-app
   #(let [a-tl (val-timeline (q-select "#a-in"))
          b-tl (val-timeline (q-select "#b-in"))]
       {:a      a-tl
        :b      b-tl
        :result (tl-merge + a-tl b-tl)})
    render-all
    (q-select "#add-two-numbers-widget")))


(dom-ready main)
