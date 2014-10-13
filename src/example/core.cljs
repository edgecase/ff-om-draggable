(ns example.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ff-om-draggable.core :refer [draggable-item]]))

(def app-state (atom {:people [{:name "Cosby" :avatar "images/cosby.png" :position {:left 100 :top 200}}
                               {:name "Jordan" :avatar "images/jordan.png" :position {:left 300 :top 30}}
                               {:name "Uncle Neo" :avatar "images/neo.png" :position {:left 500 :top 200}}
                               {:name "Chappelle" :avatar "images/chappelle.png" :position {:left 200 :top 400}}]}))

(defn person-view
  [person owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/img #js {:src (person :avatar)})
               (dom/div nil (person :name))))))

(def draggable-person-view
  (draggable-item person-view [:position]))

(defn people-view
  [app owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js  {:id "celebrities"}
             (om/build-all draggable-person-view (app :people))))))

(om/root
  people-view
  app-state
  {:target (. js/document (getElementById "app"))})
