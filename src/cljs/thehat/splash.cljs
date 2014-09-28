(ns thehat.splash
  (:require #_[figwheel.client :as fw :include-macros true]
            [dommy.core :as dommy])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(enable-console-print!)

(defn init-fullscreen-canvas [id display?]
  (doseq [old-canvas (sel (str "#" id))]
    (dommy/remove! old-canvas))

  (let [body (sel1 :body)
        width (.-innerWidth js/window)
        height (.-innerHeight js/window)
        canvas (node [:canvas
                               {:id id
                                :height height
                                :width width
                                :style {:display (if display? "block" "none")}}])]
    (dommy/append! body canvas)
    (.getContext canvas "2d")))

(defn draw-point [ctx center-x center-y]
  (doto ctx
    .beginPath
    (.arc center-x center-y 3 0 (* 2 Math/PI) false)
    .fill))

(defn build-sampler [width height radius]
  (let [max-retries 30
        radius2 (* radius radius)
        r-big (* 3 radius2)
        cell-size (* radius Math/SQRT1_2)
        grid-width (Math/ceil (/ width cell-size))
        grid-height (Math/ceil (/ height cell-size))
        grid (js/Array. (* grid-width grid-height))
        queue #js []
        queue-size (atom 0)]
    (letfn [(sample [x y]
              (let [s #js [x y]
                    grid-pos (+ (* grid-width (Math/floor (/ y cell-size)))
                                (Math/floor (/ x cell-size)))]
                (aset grid grid-pos s)
                s))
            (far [x y]
              (let [i (Math/floor (/ x cell-size))
                    i0 (max (- i 2) 0)
                    i1 (min (+ i 3) grid-width)
                    j (Math/floor (/ y cell-size))
                    j0 (max (- j 2) 0)
                    j1 (min (+ j 3) grid-height)]
                (loop [i i0
                       j j0]
                  (if-not (< j j1)
                    true
                    (if-not (< i i1)
                      (recur 0 (inc j))
                      (let [s (aget grid (+ i (* grid-width j)))]
                        (if-not s
                          (recur (inc i) j)
                          (let [dx (- (aget s 0) x)
                                dy (- (aget s 1) y)]
                            (if (< (+ (* dx dx) (* dy dy)) radius2)
                              false
                              (recur (inc i) j))))))))))
            (sampler []
              (let [i (Math/floor (* (Math/random) @queue-size))
                    s (aget queue i)]
                (when s
                  (loop [retry 0]
                    (if-not (< retry max-retries)
                      (do
                        (aset queue i (aget queue (dec @queue-size)))
                        (swap! queue-size dec)
                        s)
                      (let [a (* 2 Math/PI (Math/random))
                            r (Math/sqrt (+ (* (Math/random) r-big) radius))
                            x (+ (aget s 0) (* r (Math/cos a)))
                            y (+ (aget s 1) (* r (Math/sin a)))]
                        (if (and (>= x 0)
                                 (<= x width)
                                 (>= y 0)
                                 (<= y height)
                                 (far x y))
                          (let [new-s (sample x y)]
                            (aset queue @queue-size new-s)
                            (swap! queue-size inc)
                            s)
                          (recur (inc retry)))))))))]
      (aset queue 0 #_(sample (* (Math/random) width) (* (Math/random) height))
            (sample (* 0.5 width) (* 0.5 height)))
      (reset! queue-size 1)
      sampler)))

(def sampler-atom (atom nil))
(def canvas-ctx-atom (atom nil))
(def masked?-atom (atom nil))

(defn ticker []
  (when-let [point (@sampler-atom)]
    (let [x (Math/floor (aget point 0))
          y (Math/floor (aget point 1))]
      (if-not (@masked?-atom x y)
        (draw-point @canvas-ctx-atom x y))
      (.requestAnimationFrame js/window ticker))))

(defn ^:export init []
  (let [canvas-ctx (init-fullscreen-canvas "splashCanvas" true)
        hidden-canvas-ctx (init-fullscreen-canvas "textCanvas" false)
        width (.-innerWidth js/window)
        height (.-innerHeight js/window)]
    (set! (.-globalAlpha canvas-ctx) 0.4)

    (set! (.-textBaseline hidden-canvas-ctx) "bottom")
    (set! (.-textAlign hidden-canvas-ctx) "center")
    (set! (.-font hidden-canvas-ctx) "bold 19em PT Sans Caption")
    (doto hidden-canvas-ctx
      (.fillText "The" (* 0.5 width) (* 0.5 height)))
    (set! (.-textBaseline hidden-canvas-ctx) "top")
    (doto hidden-canvas-ctx
      (.fillText "Hat" (* 0.5 width) (* 0.5 height)))

    (let [img-data (.-data (.getImageData hidden-canvas-ctx 0 0 width height))
          masked? (fn [x y] (pos? (aget img-data (+ 3 (* (+ (* width y) x) 4)))))]

      (reset! sampler-atom (build-sampler width height 10))
      (reset! canvas-ctx-atom canvas-ctx)
      (reset! masked?-atom masked?)

      (ticker))))

#_(fw/watch-and-reload
 :jsload-callback  (fn [] ))

#_(init)
