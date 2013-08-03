(ns test.html
  (:use [castorocauda.html :only [pad attr-diffs mk-path html-delta merge-strings
                                  normalize]]
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


(qtest "merge-strings"
       (de (merge-strings ["aaa" "bbb" "ccc"]) ["aaabbbccc"] "concat all strings")

       (de (merge-strings ["aaa" [:p "bbb" "ccc"] "ddd" "eee" [:i "fff"]])
           ["aaa" [:p "bbb" "ccc"] "dddeee" [:i "fff"]]
           "merge only toplevel string"))

(qtest "normalize"
       (de (normalize [:div])
           [:div {} '()]
           "complement attrs and children")

       (de (normalize [:div [:i "aaa"]])
           [:div {} '([:i "aaa"])]
           "complement attrs. wrap children into one list")

       (de (normalize
            [:div {:hoge 4} [:div "a"]　'([:p "b"] [:b "c"]) [:i "d"]])
           [:div {:hoge 4} '([:div "a"] [:p "b"] [:b "c"] [:i "d"])]
           "nested list gets expanded. attrs copied")

       (de (normalize [:div "aaa" "bbb" [:c "ddd" "eee"]])
           [:div {} '("aaabbb" [:c "ddd" "eee"])]
           "merge strings")

       (de (normalize "text node sample")
           [:_TextNode {} '("text node sample")]
           "TextNodes are marked with :_TextNode")

       (de (normalize [:foo '(1 2 3)])
           [:foo {} '(1 2 3)]
           "If list is at attrs position and children are empty, let the list be children"))




(qtest "html-delta"
       (de (html-delta nil [:div "aaa"] [{:tag :div :index 0}] 0)
           `([:append [{:tag :div :index 0}] [:div {} (~(normalize "aaa"))] nil])
           "append to parent node if old children were not as many as new children")

       (de (html-delta '() [:div "aaa"] [{:tag :div :index 0}] 0)
           `([:append [{:tag :div :index 0}] [:div {} (~(normalize "aaa"))] nil])
           "empty collection is treated the same as nil")

       (de (html-delta [:div "aaa"] nil [{:tag :div :index 0}] 5)
           '([:remove [{:tag :div :index 0}] nil 5])
           "remove old child if there is no longer an element.")

       (de (html-delta [:div "aaa"] '() [{:tag :div :index 0}] 5)
           '([:remove [{:tag :div :index 0}] nil 5])
           "remove old child if there is no longer an element.")

       (de (html-delta [:div "aaa"] '() [{:tag :div :index 0}] 5)
           '([:remove [{:tag :div :index 0}] nil 5])
           "remove old child if there is no longer an element.")

       (de (html-delta "aaa" "aaa" [] 0)
           '() "empty list if identical")

       (de (html-delta [:div {:hoge 1} '("aaa")] [:div {:hoge 1} '("aaa")] [] 0)
           '() "empty list if identical -- a bit more spphisticated example")


       (de (html-delta "hello"
                       "world" [{:tag :div :index 0}] 2)
           '([:nodeValue
              [{:tag :div :index 0} {:tag :_TextNode :index 2}]
              "world" nil])
           "update the textnode if both new and old are textnode.")

       (de (html-delta [:div#top [:p "old-p"]]
                       [:div#top "new-textnode"] [] 0)
           '([:swap [{:tag :div#top :index 0} {:tag :_TextNode :index 0}]
             "new-textnode"
              nil])
           "swap the old element with new textnode")


       (de (html-delta [:div#top [:p] [:b {:x 2} "old-" "b"]]
                       [:div#top [:p] [:i {:x 5} "new-" "i"]] []  0)
           `([:swap [{:tag :div#top :index 0} {:tag :i :index 1}]
              [:i {:x 5} (~(normalize "new-i"))]
              nil])
           "swap if tag differ.")


       (de (html-delta [:div#top [:p] [:div [:input {:value 5}]]]
                       [:div#top [:p] [:div [:input {:value 6}]]] [] 0)
           '([:att [{:tag :div#top :index 0} {:tag :div :index 1} {:tag :input :index 0}]
              :value 6])
           "Attribute difference"))
