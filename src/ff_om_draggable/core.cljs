(ns ff-om-draggable.core
  (:require [cljs.core.async :as async :refer [put! <! mult untap tap chan sliding-buffer]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [cljs.core.async.macros :as am :refer [go-loop]]))

(def position-chan (chan (sliding-buffer 1)))
(def mouse-mult (mult position-chan))

(defn touch-move
  [e]
  (.preventDefault e)
  (let [touch (aget (.-changedTouches e) 0)]
    {:left (.-clientX touch) :top (.-clientY touch)}))

(.addEventListener js/window "mousemove" #(put! position-chan {:left (.-clientX %) :top (.-clientY %)}))
(.addEventListener js/window "touchmove" #(put! position-chan (touch-move %)))

(defn target-position
  [e]
  (let [rect (.getBoundingClientRect (.-currentTarget e))]
    {:top (.-top rect) :left (.-left rect)}))

(defn move-start
  [event-position owner current-position]
  (let [user-movement (om/get-state owner :user-movement)
        offset {:top (- (.-clientY event-position) (current-position :top))
                :left (- (.-clientX event-position) (current-position :left))}
        new-position (fn [mouse]
                       {:top (- (mouse :top) (offset :top))
                        :left (- (mouse :left) (offset :left))})]
    (om/set-state! owner :new-position new-position)
    (tap mouse-mult user-movement)))

(defn touch-start
  [e owner current-position]
  (.preventDefault e)
  (move-start (aget (.-changedTouches e) 0) owner current-position))

(defn mouse-start
  [e owner current-position]
  (.preventDefault e)
  (move-start e owner current-position))

(defn move-end
  [e owner cursor position-cursor]
  (.preventDefault e)
  (let [user-movement (om/get-state owner :user-movement)]
    (untap mouse-mult user-movement)
    (om/update! cursor position-cursor (om/get-state owner :position))
    (om/set-state! owner :new-position nil)))

(defn position
  [item position-cursor state]
  (if (state :new-position)
    (state :position)
    (get-in item position-cursor)))

(defn draggable-item
  [view position-cursor]
  (fn [item owner]
    (reify
      om/IInitState
      (init-state [_]
        {:position (get-in item position-cursor)
         :user-movement (chan)
         :new-position nil})
      om/IWillMount
      (will-mount [_]
        (let [position (om/get-state owner :user-movement)]
          (go-loop []
            (let [mouse (<! position)
                  new-position (om/get-state owner :new-position)]
              (om/set-state! owner :position (new-position mouse)))
            (recur))))
      om/IRenderState
      (render-state [_ state]
        (let [current-position (position item position-cursor state)]
          (dom/div (clj->js {:style (conj {:position "absolute"} current-position)
                             :onTouchStart #(touch-start % owner @current-position)
                             :onMouseDown #(mouse-start % owner @current-position)
                             :onTouchEnd #(move-end % owner item position-cursor)
                             :onMouseUp #(move-end % owner item position-cursor)})
                   (om/build view item)))))))
