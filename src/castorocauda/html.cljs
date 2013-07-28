(ns castorocauda.html)

(defn pad
  [xs n padd]
  (let [diff-n (- n (count xs))]
    (concat xs (map (fn [_] padd) (range diff-n)))))


(defn attr-diffs
  [attr1 attr2 path]
  (let [diff (reduce (fn [acc [k v]]
                       (cond
                        (nil? (attr2 k))
                        (cons [:rem-att path k nil] acc)

                        (not= v (attr2 k))
                        (cons [:att path k (attr2 k)] acc)

                        :else
                        acc)) '() attr1)
        diff (reduce (fn [acc [k v]]
                       (if (nil? (attr1 k))
                         (cons [:att path k v] acc)
                         acc)) diff attr2)]
    diff))

(defn mk-path
  [tag n]
  {:tag   tag
   :index n})


(defn html-delta
  "Hiccup * Hiccup * [Path] * Int -> [Delta]
   where type Delta = [typ path att-name val]"
  [old-dom new-dom path n]
  (let [[tg1 at1 & chs1] old-dom
        [tg2 at2 & chs2] new-dom
        new-path (conj path (mk-path tg2 n))]
    (cond
     (= new-dom old-dom)
     '()

     (or (not (coll? new-dom))
         (not (coll? new-dom)))
     (list [:html path new-dom nil])

     (empty? new-dom)
     (list [:html new-path nil nil])

     (empty? old-dom)
     (list [:html new-path new-dom nil])

     (not= tg1 tg2)
     (list [:html new-path new-dom nil])

     (not= at1 at2)
     (let [x (max (count chs1) (count chs2))]
       (concat (attr-diffs at1 at2 new-path)
               (mapcat (fn [ch1 ch2 idx]
                         (html-delta ch1 ch2 new-path idx))
                       (pad chs1 x nil)
                       (pad chs2 x nil)
                       (range x))))

     :else
     (let [x (max (count chs1) (count chs2))]
       (mapcat (fn [ch1 ch2 idx]
                 (html-delta ch1 ch2 new-path idx))
               (pad chs1 x nil)
               (pad chs2 x nil)
               (range x))))))



(comment html-delta
 [:div {}
  [:h1 {} "aaa"]
  [:textarea {:value "ympbyc"}]
  [:p {} "paagraph" [:a {:href "x"} "eee"]]]

 [:div {}
  [:h1 {} "aaa"]
  [:textarea {:value "kasukabe"}]
  [:p {} "paragraph" [:a {:href "y"} "eee"]]]

 [])


(comment list [:att [:div :textarea] :value "kasukabe"]
      [:html [:div :p] "paagraph" nil]
      [:att [:div :p :a] :href "x"])
