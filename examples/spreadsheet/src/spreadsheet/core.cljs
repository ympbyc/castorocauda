(ns spreadsheet.core
  (:use [castorocauda.core     :only [launch-app]]
        [castorocauda.util     :only [dom-ready q-select prn-log]]
        [castorocauda.timeline :only [timeline tl-cons! tl-map
                                      tl-filter tl-merge]]))


(defn render-cell
  "Render a table cell"
  [i x]
  [:tr [:th (str i)]
   (comment if (= mode :edit)
      [:td.edit [:input {:value (str expr)}]]
      [:td (str val)])
   [:td (str x)]])


(defn render-all
  [& cells]
  [:div#spreadsheet-example
   [:h1 {:style {:font-size "1.3em"}} "Spreadsheet Example"]
   [:p#description "Click on cells to edit its expression"]
   [:table#spreadsheet
    [:tr [:th ] [:th "A"]]
    ;(map-indexed render-cell cells)
    [:tr [:th "0"] [:td (prn-str cells)]]]])


(defn init-cells
  [n]
  (aset js/window "A" (array))
  (for [i (range 0 n)]
    (let [cell (timeline)]
      (aset window/A i cell)
      cell)))


(def spreadsheet-fns
  {:plus
   (fn [& tls]
     (->> (apply (partial tl-merge +) tls)
          prn-log))})


(doseq [[fname f] spreadsheet-fns]
  "make functions available to js"
  (aset js/window (name fname) f))


(defn main []
  (let [cells (init-cells 5)
        cells (apply (partial tl-merge (comp flatten list)) cells)]
    (tl-map (fn [x] (dorun (prn-log x))) cells)
    (launch-app
     {:cells cells}
     render-all
     (q-select "#spreadsheet-widget"))))


(dom-ready main)



[{:val 1 :expr "1" :mode :show}
 {:val 2 :expr "1 + rows[0]" :mode :show}
 {:val 3 :expr "plus(A[1], A[2])" :mode :edit}
 {:val 6 :expr "rows.sum()" :mode :show}]
