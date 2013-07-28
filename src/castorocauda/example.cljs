(ns castorocauda.example
  (:use [castorocauda.monads :only [state-m set-state fetch-state]]
        [castorocauda.core :only [castorocauda-m launch-app]])
  (:use-macros [castorocauda.monads-macro :only [domonad defn-m]]))


(defn render-all [{:keys [text]}]
  [:div {}
   [:h1 {}  text]
   [:input {:castorocauda :text-in :value text}]])



(defn dom-ready [fun]
  (set! (.-onload js/window) fun))


(defn main []
  (launch-app render-all (.getElementById js/document "castorocauda"))

  (let [xx
        ((domonad castorocauda-m
                  [x (fetch-state)]
                  x)
         {:text "最初"})]
    (.addEventListener document/body "click"
                       (fn []
                         ((domonad castorocauda-m
                                   [x (set-state {:text (str "次" (js/Date.))})
                                    x (fetch-state)]
                                   x) xx)))))

(dom-ready main)
