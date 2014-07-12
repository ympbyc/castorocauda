# Castorocauda - DOM Abstraction

![logo](http://d3j5vwomefv46c.cloudfront.net/photos/large/795746565.jpg)

## Introduction

Castorocauda provides a convenient way for client-side DOM manipulation.

Castorocauda is at this stage experimental. No API is fixed.

## Artifact

Add the following dependency to your project.clj file:

```
[castorocauda "0.1.2"]
```


## API

```clojure
(defn gendom
  "base-el :: `HTMLElement`
   old-edn :: hiccup-style EDN representing DOM
   new-edn :: hiccup-style EDN representing DOM"
  [old-edn new-edn base-el]
  new-edn)
```

## Usage

```clojure
(ns yourapp.core
    (:use [castorocauda.util :only [dom-ready q-select]]
          [castorocauda.dom  :only [gendom]]))

(def app (atom {:note "blah"}))
(def dom (atom []))

(defn render [{:keys [note]}]
  [:div
    [:h2 "Your note:"]
    [:p note]])

(defn update-note [text]
  (swap! dom
         gendom
         (render (swap! app assoc :note text))
         (q-select "#note-view")))

(dom-ready
  #(.addEventListener
     (q-select "#text")
     "keyup"
     (fn [e] (update-note (.-value (.-target e)))))
```

## How it works

Renderers create a virtual DOM using application state.

```clojure
(defn render-all [state]
  [:p (:message state)])
```

The virtual DOM gets passed to `gendom` along with a previous virtual DOM and a HTMLElement where we want the virtual DOM to be rendered.

```clojure
(gendom (render-all {:message "iwana"})
        (render-all {:message "ugui"})
        (q-select "#main"))
```

`gendom` compares the new virtual DOM and the old one to calcurate deltas, applies deltas to the DOM.

```clojure
(let [old-v-dom [:div [:p "ayu"]
                      [:div [:a {:href "/maguro"} "hamachi"]]]
      new-v-dom [:div [:p "ayu"]
                      [:div [:a {:href "/kurodai"} "saba"]
                      [:p "kujira"]]]
      delta     (html-delta old-v-dom new-v-dom [] 0)]
     delta)

;;=>
([:att       [0 1 0]   :href "/kurodai"]
 [:nodeValue [0 1 0 0] "saba" nil]
 [:append    [0 1]     [:p {} [:_TextNode {} "kujira"]] nil])
```


Here's a visualization of DOM elements that gets modified when you type some numbers in.
![delta static](https://rawgithub.com/ympbyc/castorocauda/master/resources/public/images/castorocauda1.png)
![delta gif](https://rawgithub.com/ympbyc/castorocauda/master/resources/public/images/Castorocauda3.gif)
With a brand new html delta calculator and committer, you can trust that only the smallest possible set of nodes are re-rendered.


## Running the Test Suit

Castorocauda.html is fully tested. You need to run `lein cljsbuild once` to build /resources/public/js/tests.js before running the tests in the browser. Once that's done open test.html to see how it goes. Note that Castorocauda uses core.async, which is at this stage SNAPSHOT, for testing so you need to open project.clj and uncomment the specified line to build the tests.


## License

Distributed under the Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php), the same as Clojure.
