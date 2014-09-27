(ns thehat.splash
  (:require [figwheel.client :as fw :include-macros true]
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

(defn far [cell-size grid-width grid-height grid radius2 x y]
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


(defn sample [cell-size grid-width grid x y]
  (let [s #js [x y]
        grid-pos (+ (* grid-width (Math/floor (/ y cell-size)))
                    (Math/floor (/ x cell-size)))]
    (aset grid grid-pos s)
    s))

(defn build-sampler [width height radius]
  (let [max-retries 30
        radius2 (* radius radius)
        r-big (* 3 radius2)
        cell-size (* radius Math/SQRT1_2)
        grid-width (Math/ceil (/ width cell-size))
        grid-height (Math/ceil (/ height cell-size))
        grid (js/Array. (* grid-width grid-height))
        queue #js []]
    (letfn [
            (sample-seq [queue-size]
              (let [i (Math/floor (* (Math/random) queue-size))
                    s (aget queue i)]
                (when s
                  (loop [retry 0]
                    (if-not (< retry max-retries)
                      (do
                        (aset queue i (aget queue (dec queue-size)))
                        (cons s (lazy-seq (sample-seq (dec queue-size)))))
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
                            (aset queue queue-size new-s)
                            (cons s (lazy-seq (sample-seq (inc queue-size)))))
                          (recur (inc retry)))))))))]
      (aset queue 0 (sample (* (Math/random) width) (* (Math/random) height)))
      (sample-seq 1))))

(defn sample-seq
  ([width height radius]
     (let [cell-size (* radius Math/SQRT1_2)
           grid-width (Math/ceil (/ width cell-size))
           grid-height (Math/ceil (/ height cell-size))
           grid (js/Array. (* grid-width grid-height))
           queue #js []]
       (aset queue 0 (sample cell-size grid-width grid
                             (* (Math/random) width) (* (Math/random) height)))
       (sample-seq width height radius grid queue queue-size)))
  ([width height radius grid queue queue-size]
     (let [max-retries 30
           radius2 (* radius radius)
           r-big (* 3 radius2)
           cell-size (* radius Math/SQRT1_2)
           grid-width (Math/ceil (/ width cell-size))
           grid-height (Math/ceil (/ height cell-size))
           i (Math/floor (* (Math/random) queue-size))
           s (aget queue i)]
       (prn queue-size i)
       (when s
         (loop [retry 0]
           (if-not (< retry max-retries)
             (do
               (aset queue i (aget queue (dec queue-size)))
               (cons s (lazy-seq (sample-seq (dec queue-size)))))
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
                   (aset queue queue-size new-s)
                   (cons s (lazy-seq (sample-seq width height radius grid queue (inc queue-size)))))
                 (recur (inc retry))))))))))


(def points-seq-atom (atom nil))

(defn ticker []
  (let [points-seq @points-seq-atom]
    (when (seq? points-seq)
      (let [point (first points-seq)
            x (Math/floor (aget point 0))
            y (Math/floor (aget point 1))]
        (swap! points-seq-atom rest)
        (if-not (masked? x y)
          (draw-point canvas-ctx x y))
        (.requestAnimationFrame js/window ticker)))))

(defn ^:export init []
  (let [canvas-ctx (init-fullscreen-canvas "splashCanvas" true)
        hidden-canvas-ctx (init-fullscreen-canvas "textCanvas" false)
        width (.-innerWidth js/window)
        height (.-innerHeight js/window)]

    (set! (.-textBaseline hidden-canvas-ctx) "bottom")
    (set! (.-textAlign hidden-canvas-ctx) "center")
    (set! (.-font hidden-canvas-ctx) "bold 19em sans-serif")
    (doto hidden-canvas-ctx
      (.fillText "The" (* 0.5 width) (* 0.5 height)))
    (set! (.-textBaseline hidden-canvas-ctx) "top")
    (doto hidden-canvas-ctx
      (.fillText "Hat" (* 0.5 width) (* 0.5 height)))

    (let [img-data (.-data (.getImageData hidden-canvas-ctx 0 0 width height))
          masked? (fn [x y] (pos? (aget img-data (+ 3 (* (+ (* width y) x) 4)))))]
      (set! (.-fillStyle hidden-canvas-ctx) "rgb(0,0,255)")
      (doseq [x (range width) y (range height)]
        (when (masked? x y)
          (.fillRect hidden-canvas-ctx x y 1 1)))

      (reset! points-seq-atom (sample-seq width height 10))
      #_(ticker)

      (prn @points-seq-atom)

      (doseq [point @points-seq-atom
              :let [x (Math/floor (aget point 0))
                    y (Math/floor (aget point 1))]]
        (if-not (masked? x y)
          (draw-point canvas-ctx x y))))))

(fw/watch-and-reload
 :jsload-callback  (fn [] ))

(init)
