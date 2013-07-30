(ns test.html
  (:use [castorocauda.html :only [pad attr-diffs mk-path html-delta]]
        [test.test :only [se de ok prn-log]])
  (:use-macros [test.test-macro :only [qtest]]))


(js/module "castorocauda.html")

(qtest "pad"
       (de (pad [1 2 3] 5 :x) [1 2 3 :x :x] "n greater than length")
       (de (pad [1 2 3] 2 :x) [1 2 3] "ignore if n is less tha length")
       (de (pad [] 5 nil) [nil nil nil nil nil] "empty coll and nil padding")
       (de (pad [1 2 3] -2 :x) [1 2 3] "negative n"))


(defn all-contained
  "test if all contents in coll2 is in coll1"
  [coll1 coll2]
  (every?
   identity
   (map (fn [x] (some #{x} coll1)) coll2)))



(qtest "attr-diffs"
       (.log js/console (clj->js (attr-diffs {:text "hello" :arr [1 2 3]}
                                             {:text "rrr"   :arr [2 3 4]} [:x])))
       (ok (all-contained
            (attr-diffs {:text "hello" :arr [1 2 3]}
                        {:text "rrr"   :arr [2 3 4]} [:x])
            #{[:att [:x] :arr [2 3 4]]
              [:att [:x] :text "rrr"]})
           "double :att")


       (ok (all-contained (attr-diffs {:text "hello" :arr [1 2 3]}
                                      {:text "clojure"} [])
                          #{[:att [] :text "clojure"]
                            [:rem-att [] :arr nil]})
           ":att and :rem-att")

       (ok (all-contained
            (attr-diffs {} {:text "okok"} [:x :y])
            #{[:att [:x :y] :text "okok"]})
           ":att from empty map"))


(qtest "mk-path"
       (de (mk-path :div 3)
           {:tag :div :index 3}
           "make path"))
