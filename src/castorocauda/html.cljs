(ns castorocauda.html)


(defn prn-log
  [x]
  (.log js/console (prn-str x))
  x)

(defn pad
  "Widen the collection `xs` upto length `n` and fill the blank with `padd`."
  [xs n padd]
  (let [diff-n (- n (count xs))]
    (concat xs (map (fn [_] padd) (range diff-n)))))


(defn attr-diffs
  "Calcurate the difference between two attribute map."
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
  "Path is {:tag Keyword :index Int}"
  [tag n]
  {:tag   tag
   :index n})

(declare delta-dive)

(defn html-delta
  "Hiccup * Hiccup * [Path] * Int -> [Delta]
   where type Delta = [typ path att-name val]"
  [old-dom new-dom path n]
  (let [[tg1 at1 & chs1] old-dom
        [tg2 at2 & chs2] new-dom
        cur-path  (conj path (mk-path :div.castorocauda-wrap n))
        next-path (conj cur-path (mk-path tg2 0))

        ;; attributes can be ommited
        [at1 chs1] (if (map? at1) [at1 chs1] [{}  (cons at1 chs1)])
        [at2 chs2] (if (map? at2) [at2 chs2] [{}  (cons at2 chs2)])]
    (cond
     (nil? new-dom)
     '()

     (= new-dom old-dom)
     '()

     (or (not (coll? new-dom))
         (not (coll? old-dom)))
     ;;new-dom is TextNode and -> update-parent
     ;;new-dom is element and old one was TextNode -> update parent
     (list [:child-html path new-dom nil])


     (empty? new-dom)
     (list [:html cur-path nil nil])

     (empty? old-dom)
     (list [:html cur-path new-dom nil])

     (not= tg1 tg2)
     (list [:html cur-path new-dom nil])

     (not= at1 at2)
     (concat (attr-diffs at1 at2 next-path)
             (delta-dive chs1 chs2 next-path))

     :else
     (delta-dive chs1 chs2 next-path))))



(defn wrap-tags
  "Wrap every tag with :div.castorocauda-wrap"
  [dom]
  (let [[tag attrs & children] dom
        [attrs children] (if (map? attrs) [attrs children] [{} (cons attrs children)])]
    (cond
     (not (coll? dom))
     [:div.castorocauda-wrap dom]

     :else
     [:div.castorocauda-wrap
      [tag attrs (map wrap-tags children)]])))
