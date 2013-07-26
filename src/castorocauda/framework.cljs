(ns castorocauda.framework
  "Public api of castorocauda. This is the file you should :require.
   type State = (State -> [a State])"
  (:use [castorocauda.monads :only [state-m patch-state set-state]])
  (:use-macros [castorocauda.monads-macro :only [domonad defn-m]]))

(defn  launch-app
  "State * (State -> Hiccup) * HTMLElement -> State"
  [state render-all context]
  ((domonad state-m
            [dom render-all]
            dom) state))






(comment
  (defn render-all
    [state]
    (list
     [:div
      [:h1 "hello"]]
     state)))
