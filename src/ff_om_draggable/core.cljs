(ns ff-draggable.core
  (:require [cljs.core.async :as async :refer [put! <! mult untap tap chan sliding-buffer]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [dommy.core :refer [set-style!]])
  (:require-macros [cljs.core.async.macros :as am :refer [go]]))

(enable-console-print!)

(defn update-mouse-position
  [e channel]
  (.preventDefault e)
  (let [position {:left (.-clientX e) :top (.-clientY e)}]
    (put! channel position)))

(def position-chan (chan (sliding-buffer 1)))
(def mouse-position (mult position-chan))
(js/window.addEventListener "mousemove" #(update-mouse-position % position-chan))

(defn target-position
  [e]
  (let [rect (.getBoundingClientRect (.-currentTarget e))]
    {:top (.-top rect) :left (.-left rect)}))

(defn move-start
  [e owner]
  (.preventDefault e)
  (let [mouse-tap (om/get-state owner :mouse-tap)
        current-position (target-position e)
        position-offset {:top (- (.-clientY e) (current-position :top))
                         :left (- (.-clientX e) (current-position :left))}]
    (tap mouse-position mouse-tap)
    (om/set-state! owner :position-offset position-offset)))

(defn move-end
  [e owner cursor position-cursor]
  (.preventDefault e)
  (let [mouse-tap (om/get-state owner :mouse-tap)]
    (untap mouse-position mouse-tap)
    (om/update! cursor position-cursor (target-position e))))

(defn draggable-item
  [view position-cursor]
  (fn [cursor owner]
    (reify
      om/IInitState
      (init-state [_]
        {:mouse-tap (chan)
         :position-offset nil})
      om/IWillMount
      (will-mount [_]
        (let [position (om/get-state owner :mouse-tap)]
          (go (while true
                (let [mouse (<! position)
                      offset (om/get-state owner :position-offset)
                      node (om/get-node owner)
                      top (- (mouse :top) (offset :top))
                      left (- (mouse :left) (offset :left))]
                  (set-style! node :top top :left left))))))
      om/IRenderState
      (render-state [_ state]
        (dom/div (clj->js {:style (conj {:position "absolute"} (get-in cursor position-cursor))
                           :onMouseDown #(move-start % owner)
                           :onMouseUp #(move-end % owner cursor position-cursor)})
                 (om/build view cursor))))))

