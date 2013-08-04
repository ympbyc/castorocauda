(ns spreadsheet.core
  (:require [goog.dom :as gdom])
  (:use [castorocauda.core     :only [launch-app]]
        [castorocauda.util     :only [dom-ready q-select prn-log dom-delegated-events
                                      dom-element-events q-select-all delayed-fn]]
        [castorocauda.timeline :only [timeline tl-cons! tl-now
                                      tl-map tl-filter tl-merge]]))


(defn render-cell
  "Render a table cell"
  [i {:keys [mode expr val] :as x}]
  [:tr [:th (str i)]
   (if (= mode :edit)
     [:td.cell-edit [:input.cell-expr {:value (str expr)}]]
     [:td.cell-static {:data-id i} (str expr " = " val)])])


(defn render-all
  [{:keys [cells]}]
  [:div#spreadsheet-example
   [:h1 {:style {:font-size "1.3em"}} "Spreadsheet Example"]
   [:p#description
    "Click on cells to edit its expression"
    "Supported functions: "
    [:span.code "add"]
    [:span.code "sub"]
    [:span.code "mul"]
    [:span.code "div"]]
   [:table#spreadsheet
    [:tr [:th ] [:th "A"]]
    (map-indexed render-cell cells)]])


(defn char-from
  [start-char length]
  (->> (range (.charCodeAt start-char 0) (+ (.charCodeAt start-char 0) length))
       (map String/fromCharCode)))


(defn init-cells
  [x y]
  (prn-log (char-from "A" x))
  (into {}
        (for [ch (char-from "A" x)]
          (do
            (aset js/window (str ch) (array))
            [ch (vec (for [i (range 0 y)]
                       (let [cell (timeline {:val 0 :expr "0" :mode :show})]
                         (aset (aget js/window (str ch)) i cell)
                         cell)))]))))


(def js-eval
  (fn [expr] (js/eval expr)))


(defn lift-tl-fn
  [f]
  (fn [& tls]
    (reduce
     f
     (map (fn [x]
            (let [y (js/parseFloat x)]
              (if (js/isNaN y) (->> x tl-now :expr js-eval)
                  y)))
          tls))))


(def spreadsheet-fns
  {:add
   (lift-tl-fn +)

   :sub
   (lift-tl-fn -)

   :mul
   (lift-tl-fn *)

   :div
   (lift-tl-fn /)})


(doseq [[fname f] spreadsheet-fns]
  "make functions available to js"
  (aset js/window (name fname) f))


(defn ev-target-attr
  [attr-name e]
  (.getAttribute (.-target e) attr-name))


(defn update-awkward
  "Couldn't figure out a nicer way..."
  [cells]
  (dorun
   (map-indexed
    (fn [i td]
      (let [x (tl-now (get cells i))]
        (gdom/replaceNode (gdom/createTextNode (str (:expr x) " = " (js-eval (:expr x))))
                          (aget (.-childNodes td) 0))))
    (q-select-all ".cell-static"))))


(defn watch-blur [cell cells el]
  ;;when focus is lost from a cell, set the cell's mode to :show
  (.focus el)
  (tl-map
   (fn [e] (tl-cons!
           (assoc (tl-now cell)
             :mode :show
             :expr (.-value el)
             :val  (js/eval (.-value el)))
           cell)
     (update-awkward cells))
   (dom-element-events "blur" el)))


(defn main []
  (let [cells (get (init-cells 5 5) "A")
        table (apply (partial tl-merge (fn [& xs] (vec xs))) cells)]
    (aset js/window "CELLS" cells)
    (launch-app
     {:cells table}
     render-all
     (q-select "#spreadsheet-widget"))
    (tl-cons! {:val 0 :expr "0" :mode :show}  (cells 2))
    (tl-cons! {:val 0 :expr "0" :mode :show} (cells 0))
    (tl-cons! {:val 0 :expr "0" :mode :show} (cells 1))
    (tl-cons! {:val 0 :expr "0" :mode :show} (cells 3))



    ;;when clicked on a cell, set the mode of the cell to :edit
    (tl-map
     (fn [e]
       (.log js/console e)
       (let [cell (get cells (ev-target-attr "data-id" e))]
         (tl-cons!
          (assoc (tl-now cell) :mode :edit)
          cell)
         (watch-blur cell cells (q-select ".cell-expr" (-> e .-target .-parentNode)))))
     (dom-delegated-events "click" ".cell-static"))))


(dom-ready main)
