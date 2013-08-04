(ns spreadsheet.core
  (:require [goog.dom :as gdom])
  (:use [castorocauda.core     :only [launch-app]]
        [castorocauda.util     :only [dom-ready q-select prn-log dom-delegated-events
                                      dom-element-events q-select-all delayed-fn]]
        [castorocauda.timeline :only [timeline tl-cons! tl-now
                                      tl-map tl-filter tl-merge timeline?]]))


(def table-x 3)
(def table-y 3)


(defn char-from
  "List `n` charactors successive to `start-char`"
  [start-char n]
  (->> (range (.charCodeAt start-char 0) (+ (.charCodeAt start-char 0) n))
       (map String/fromCharCode)))


(defn render-cell
  "Render a table cell"
  [y x {:keys [val expr mode] :as cell}]
  (if (= mode :edit)
    [:td.cell-edit [:input.cell-expr {:value (str expr)}]]
    [:td.cell-static {:data-id (+ (* y table-x) x)} (str expr " = " val)]))


(defn render-all
  "Take a flat list of cells and render a `table-x` * `table-y` table."
  [{:keys [cells]}]                   ;;cells :: [{} {} {} {}]
  (let [cellss (partition table-x cells)]   ;;cells :: [[{} {}] [{} {}]]
    [:div#spreadsheet-example
     [:table#spreadsheet
      [:tr [:th ] (map (fn [c] [:th c]) (char-from "A" table-x))]
      (map
       (fn [cells y] [:tr [:th (str y)] (map-indexed (partial render-cell y) cells)])
       cellss (range 0 table-y))]]))


(defn init-cells
  [x y]
  (prn-log (char-from "A" x))
  (vec (apply concat
          (for [ch (char-from "A" x)]
            (do
              (aset js/window (str ch) (array))
              (vec (for [i (range 0 y)]
                     (let [cell (timeline {:val 0 :expr "0" :mode :show})]
                       (aset (aget js/window (str ch)) i cell)
                       cell))))))))

(defn tl-unit-idempotent
  [x]
  (if (timeline? x) x
      (timeline {:expr x})))


(defn js-eval
  [expr]
  (->>
   expr
   js/eval))


(defn lift-tl-fn
  "Lift an `a * a -> a` function upto `Timeline a * Timeline a * ... -> Timeline a` function"
  [f]
  (fn [& tls]
    (->> tls
         (map #(->> % tl-unit-idempotent tl-now :expr js-eval))
         (reduce f))))


;;Builtin operators
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
  ;;make functions available to js
  (aset js/window (name fname) f))


(defn ev-target-attr
  "Retrieve the target element from an event"
  [attr-name e]
  (.getAttribute (.-target e) attr-name))


(defn update-awkward
  "Couldn't figure out a nicer way..."
  [cells]
  (dorun
   (map-indexed
    (fn [i td]
      (let [x (tl-now (get cells i))]
        (gdom/replaceNode (gdom/createTextNode (str (:expr x)
                                                    " = "
                                                    (->> x :expr js-eval)))
                          (aget (.-childNodes td) 0))))
    (q-select-all ".cell-static"))))


(defn watch-blur
  "When focus is lost from a cell, set the cell's mode to :show.
   And evaluate the given expression."
  [cell cells el]
  (.focus el)
  (tl-map
   (fn [e] (tl-cons!
           (assoc (tl-now cell)
             :mode :show
             :expr (.-value el)
             :val  (->> el .-value js-eval))
           cell)
     (update-awkward cells))
   (dom-element-events "blur" el)))


(defn main
  "The entry point. Init timelines and bind them to the renderer"
  []
  (let [cells (init-cells table-x table-y)
        table (apply (partial tl-merge (fn [& xs] (vec xs))) cells)]
    (aset js/window "CELLS" cells)
    (launch-app
     {:cells table}
     render-all
     (q-select "#spreadsheet-widget"))
    (tl-cons! {:val 17 :expr "add(A[0], C[2])" :mode :show} (cells 4))
    (tl-cons! {:val 9 :expr "9" :mode :show} (cells 0))
    (tl-cons! {:val 8 :expr "8" :mode :show} (cells 8))



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


;;kickoff
(dom-ready main)
