(ns castorocauda.example
  (:use [castorocauda.monads :only [state-m set-state fetch-state patch-state]]
        [castorocauda.core :only [castorocauda-m launch-app]])
  (:use-macros [castorocauda.monads-macro :only [domonad defn-m]]))


(defn render-all [{:keys [text]}]
  [:div {}
   [:h1 {}  text]
   [:input {:castorocauda :text-in :value text}]])



(defn dom-ready [fun]
  (set! (.-onload js/window) fun))


(defn on-click
  "TODO: officialize this"
  [el f st]
  (.addEventListener
   el "click"
   (fn []
     (.log js/console @st)
     ((domonad castorocauda-m
               [s (fetch-state)]
               (reset! st s))
      (f @st)))))


(defn main []
  (launch-app render-all (.getElementById js/document "castorocauda"))

  (let [xx (atom {:text "最初"})]
    (on-click js/document
              (domonad castorocauda-m
                       [st (fetch-state)
                        x (set-state {:text (str "次次" (st :text))})]
                       x)
              xx)))

(dom-ready main)
