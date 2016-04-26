(ns img-cmp-clj.core
  (:require [me.raynes.conch :refer [with-programs]]
            [hiccup.page :refer [html5 include-css include-js]])
  (:gen-class))

(defn compare-files
  [{:keys [expected actual diff]}]
  (let [comparison (with-programs [compare]
                                  (compare "-metric" "AE" expected actual diff {:throw false :verbose true}))
        exit-code (-> comparison :exit-code deref)]
    {:match   (= 0 exit-code)
     :message (-> comparison :proc :err first)}))

(defn change-prefix
  [expected prefix]
  (clojure.string/replace expected "expected/" prefix))

(defn expected-seq
  []
  (->> (clojure.java.io/file "expected")
       file-seq
       (filter #(.isFile %))
       (map str)
       (map #(hash-map :expected %
                       :diff (change-prefix % "diff/")
                       :actual (change-prefix % "actual/")))))

(defn compare-all
  []
  (map #(merge % (compare-files %)) (expected-seq)))

(defn render-item
  [item]
  [:div
   [:h1
    (:expected item)
    ": "
    (if (:match item) "Matched" "Did not match")]
   [:p
    [:code
     (:message item)]]
   [:table.table
    [:tr
     [:td.text-center [:img.img-responsive {:src (:expected item)}]]
     [:td.text-center [:img.img-responsive {:src (:actual item)}]]
     [:td.text-center [:img.img-responsive {:src (:diff item)}]]]]])

(defn render
  [items]
  (html5
    [:head
     (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css")
     (include-js "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js")
     [:style "td { width: 33% }"]]
    [:div.container (map render-item items)]))

(defn -main
  [& args]
  (->> (compare-all) render (spit "out.html")))