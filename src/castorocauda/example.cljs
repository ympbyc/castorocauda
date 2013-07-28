(ns castorocauda.example
  (:use [castorocauda.core :only [launch-app patch-state on-click]]
        [castorocauda.dom  :only [dom-ready q-select]]
        [castorocauda.stream :only [mk-fstream map-fstream merge-fstream cons-fstream dom-events]]))


(defn render-all [{:keys [a b result]}]
  [:div {}
   [:h1 {}  (str a "+" b "=" result)]
   [:input#a-in {:value a}]
   [:input#b-in {:value b}]])


(defn val-stream
  [el]
  (->>
   (dom-events el "keyup")
   (map-fstream (fn [e]
                  (js/parseInt (.-value (.-target e)))))))


(defn main []
  (launch-app
   {:a 2 :b 3 :result 5}
   render-all
   (q-select "#castorocauda"))

  (comment on-click js/document
            (fn [e st]
              {:a 4}))

  (let [a-stream (val-stream (q-select "#a-in"))
        b-stream (val-stream (q-select "#b-in"))
        c-stream (merge-fstream + a-stream b-stream)]
      (map-fstream
       (fn [res]
         (.log js/console (clj->js @a-stream))
         (patch-state (fn [st] {:a (first @a-stream)
                               :b (first @b-stream)
                               :result res})))
   c-stream)))

(dom-ready main)



;;;;;;;;;;;;;;;;;;; IDEAL ;;;;;;;;;;;;;;;;;

(comment
  (defn val-stream
    [el-query]
    (->>
     (mk-dom-events el-query "keyup")
     (map (fn [{{:keys [value]} :target}] (js/parseInt value)))))

  (def a-stream
    (val-stream  #(.getElementById js/document "a-in")))

  (def b-stream
    (val-stream  #(.getElementById js/document "b-in")))

  (def result-stream
    (merge a-stream b-stream))

  (launch-app
   {:a      a-stream
    :b      b-stream
    :result result-stream}
   render-all
   (.getElementById js/document "castorocauda")))
