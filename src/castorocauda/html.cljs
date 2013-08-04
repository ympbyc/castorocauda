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


(def parse-tagname
  ;;extract tag name, id and classes from keyword
  (memoize
   (fn [tagname]
     (let [[_ name _ id classes] (re-matches #"^([^.^#]+)(#([^.]+))?(\..+)?" tagname)
           attrs {}
           attrs (if classes
                   (assoc attrs :class (clojure.string/replace classes #"\." " "))
                   attrs)
           attrs (if id (assoc attrs :id (str id)) attrs)]
       {:tag   (keyword name)
        :attrs attrs}))))



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


(defn- flatten-children
  "([:div 1] ([:p 2] [:b 3]) [:i 4]) -> ([:div 1] [:p 2] [:b 3] [:i 4])"
  [acc x]
  (if (seq? x)
    (concat acc x)
    (concat acc (list x))))


(defn normalize
  "[:div xxx yyy]          -> [:div {} (xxx yyy)]
   [:div {} xxx yyy]       -> [:div {} (xxx yyy)]
   [:div {} xxx (yyy) zzz] -> [:div {} (xxx yyy zzz)]
   hello                   -> [:_textNode {} hello]"
  [el]
  (if (string? el) [:_TextNode {} (list el)]
      (let [[tg at & chs] el
            tg-parsed (parse-tagname tg)
            [at chs]      (cond (map? at) [at chs]
                                (or (empty? at))   [{} chs]
                                (and (seq? at) (empty? chs)) [{} at] ;;list at att position
                                :else     [{}  (cons at chs)])
            [tg at]       [(:tag tg-parsed) (merge at (:attrs tg-parsed))]
            chs           (merge-strings chs)
            chs           (reduce flatten-children '() chs)]
        [tg at chs])))


(defn invalid? [x]
  (or (nil? x) (and (coll? x) (empty? x))))


(declare delta-dive)

(defn- html-delta-
  "Hiccup * Hiccup * [Path] * Int -> [Delta]
   where type Delta = [typ path att-name val]"
  [old-dom new-dom path n]
  (cond
   (invalid? old-dom)
   (let [[tg at chs] (normalize new-dom)]
     (list [:append path [tg at (map normalize chs)] nil]))

   (invalid? new-dom)
   (list [:remove path nil n])

   :else
   (let [[tg1 at1 chs1 :as old-dom] (normalize old-dom)
         [tg2 at2 chs2 :as new-dom] (normalize new-dom)
         next-path (conj path (mk-path tg2 n))]
     (cond
      (= new-dom old-dom)
      '()

      (= tg1 tg2 :_TextNode)
      (list [:nodeValue next-path (first chs2) nil])

      (= tg2 :_TextNode) ;only the new-dom is textnode
      (list [:swap next-path (first chs2) nil])

      (not= tg1 tg2)
      (list [:swap next-path [tg2 at2 (map normalize chs2)] nil])

      (not= at1 at2)
      #(concat (attr-diffs at1 at2 next-path)
               (delta-dive chs1 chs2 next-path))

      :else
      #(delta-dive chs1 chs2 next-path)))))


(defn html-delta
  [old-dom new-dom path n]
  (trampoline html-delta- old-dom new-dom path n))


(defn- delta-dive
  [chs1 chs2 next-path]
  (let [x  (max (count chs1) (count chs2))]
    (mapcat (fn [ch1 ch2 idx]
              (trampoline html-delta- ch1 ch2 next-path idx))
            (pad chs1 x nil)
            (pad chs2 x nil)
            (range x))))
