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


(defn merge-strings
  "Group adjoining strings. This corresponds to browsers' behaviour."
  [lst]
  (if-let [[x & more] lst]
    (if-let [[y & more] more]
      (if (and (string? x) (string? y))
        (merge-strings (cons (str x y) more))
        (cons x (merge-strings (cons y more))))
      lst)
    lst))

(defn normalize
  [el]
  (let [[tg at & chs] el
        [at chs]      (if (map? at) [at chs] [{}  (cons at chs)])
        chs           (merge-strings chs)]
    (concat [tg at] chs)))


(declare delta-dive)

(defn html-delta
  "Hiccup * Hiccup * [Path] * Int -> [Delta]
   where type Delta = [typ path att-name val]"
  [old-dom new-dom path n]
  (let [[tg1 at1 & chs1] (normalize old-dom)
        [tg2 at2 & chs2] (normalize new-dom)
        next-path (conj path (mk-path tg2 n))]
    (cond
     (= new-dom old-dom)
     '()


     (nil? old-dom) ;No corresponding node is present
     (list [:html-children path new-dom nil])


     (or (not (coll? old-dom))
         (not (coll? new-dom)))
     ;;Update the node at corresponding position in-place
     (list [:html-child path new-dom n])


     (empty? new-dom) ;the content of the parent will be erased
     (list [:html path '() nil])

     (empty? old-dom) ;was empty, now not
     (list [:html path new-dom nil])


     (not= tg1 tg2)
     (list [:html path new-dom nil])

     (not= at1 at2)
     (concat (attr-diffs at1 at2 next-path)
             (delta-dive chs1 chs2 next-path))

     :else
     (delta-dive chs1 chs2 next-path))))


(defn delta-dive
  [chs1 chs2 next-path]
  (let [x  (max (count chs1) (count chs2))]
    (mapcat (fn [ch1 ch2 idx]
              (html-delta ch1 ch2 next-path idx))
            (pad chs1 x nil)
            (pad chs2 x nil)
            (range x))))


(defn wrap-tags
  "Wrap every tag with :div.castorocauda-wrap"
  [dom]
  (let [[tag attrs & children] dom
        [attrs children] (if (map? attrs) [attrs children] [{} (cons attrs children)])]
    (cond
     (not (coll? dom))
     dom ;[:div.castorocauda-wrap dom]

     :else
     [tag attrs (map wrap-tags children)])))


     (comment [:div.castorocauda-wrap
       [tag attrs (map wrap-tags children)]])
