(ns add-two-numbers.core
  (:use [castorocauda.util :only [dom-ready q-select]]
        [castorocauda.dom  :only [gendom]]))


(defn render-all
  "Map a snapshot of the app's state to an EDN representing DOM"
  [{:keys [a b]
    :or   {a 0, b 0}}]
  (let [result (+ a b)]
    [:div
     [:h2 (str a " + " b " = " result)]
     [:input#a-in {:type "number"}] " + "
     [:input#b-in {:type "number"}] (str " = " result)
     [:p "the result is "
      [:span (if (even? result) "even" "odd")]]
     (if (< result 5)
       [:div [:p {:style {:color "red"}} "result is less than 5"]]
       (list [:p {:style {:color "red"}} "result is greater than 5"]
             [:p "..."]))]))


(def main-dom (atom []))

(def app (atom {:a 0 :b 0}))



(defn watcher [key]
  (fn [e]
    (swap! main-dom
           gendom
           (render-all (swap! app assoc key (js/parseInt (.-value (.-target e)))))
           (q-select "#add-two-numbers-widget"))))

(defn main []
  (swap!  main-dom
          gendom
          (render-all @app)
          (q-select "#add-two-numbers-widget"))

  (.addEventListener
   (q-select "#a-in")
   "keyup"
   (watcher :a))
  (.addEventListener
   (q-select "#b-in")
   "keyup"
   (watcher :b)))

(dom-ready main)
