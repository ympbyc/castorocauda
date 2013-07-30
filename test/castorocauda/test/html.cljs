(ns test.html
  (:use [castorocauda.html :only [pad attr-diffs mk-path html-delta]]
        [test.test :only [se de ok prn-log]])
  (:use-macros [test.test-macro :only [qtest]]))


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

(def example [:div
              [:a {:href "..."} "clojure"]
              [:p "hello"]
              [:i "good"]])


(comment qtest "html-delta"
      (de
       (html-delta
        [] [] [] 0)
       '()
       "blank")

      (de
       (html-delta
        example
        example
        [(mk-path :p 0)] 0)
       '()
       "identical")

      (de
       (html-delta
        example
        [:div "clojure"]
        [] 1)
       (list [:html [(mk-path :div 1)] "clojure" nil])
       "new-dom become TextNode. Index properly affects the result.")

      (de
       (html-delta
        [:div "clojure"]
        example
        [] 0)
       (list [:html [(mk-path :div 0)] (list [:a {:href "..."} "clojure"]
                                             [:p "hello"]
                                             [:i "good"]) nil])
       "new-dom become element node")

      (de
       (html-delta
        example
        []
        [(mk-path :div 1)] 2)
       (list [:html [(mk-path :div 1)] nil nil])
       "new-dom empty")


      (de
       (html-delta
        []
        example
        [] 0)
       (list [:html [] [:div
                        [:a {:href "..."}  "clojure"]
                        [:p "hello"]
                        [:i "good"]] nil])
       "old-dom empty")


      (de
       (html-delta
        example
        [:div
         [:a {:href "..."} "clojure"]
         [:span "hello"]]
        [] 0)
       (list [:html [(mk-path :div 0)] [:span "hello"] nil])           ;;;;; SUSPICIOUS ;;;;;
       "different tag new-dom")

      (de
       (html-delta
        [:div
         [:a {:href "..."} "clojure"]
         [:span "hello"]]
        example
        [] 0)
       (list [:html [(mk-path :div 0)]
              (list [:p "hello"] [:i "good"]) nil])
       "different tag old-dom")


      (prn-log
        (html-delta
           [:div#example-app
            [:h1 (str a "+" b "=" result)]
            [:input#a-in {:value a}]
            [:input#b-in {:value b}]
            [:div
             [:a {:href "..."} "clojure"]
             [:p "激しく動かすとやばそう"]]]
           [:div#example-app
            [:h1 (str a "+" b "=" result)]
            [:input#a-in {:value a}]
            [:input#b-in {:value b}]
            [:p "5以上"]]
           [] 0))


      (ok
       (all-contained
        (html-delta
         example
         [:div
          [:a {:href "@@@"} "clojure"]
          [:p {:id "hoge"} "hello"]
          [:i {}  "good"]]
         [] 0)
        #{[:att [(mk-path :div 0) (mk-path :a 0)] :href "@@@"]
          [:att [(mk-path :div 0) (mk-path :p 1)] :id "hoge"]})
       "attributes")


      (ok
       (all-contained
        (html-delta
         example
         [:div
          [:a {:href "@@@"} "clojureeee"]
          [:p {:id "hoge"} "hello"]]
         [] 0)
        #{[:att [(mk-path :div 0) (mk-path :a 0)] :href "@@@"]
          [:html [(mk-path :div 0) (mk-path :a 0)] "clojureeee" nil]
          [:att [(mk-path :div 0) (mk-path :p 1)] :id "hoge"]})
       "attributes and content"))
