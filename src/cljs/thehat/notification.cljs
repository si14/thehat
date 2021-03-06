(ns thehat.notification
  (:require
   [figwheel.client :as fw :include-macros true]
   [dommy.core :as dommy :refer-macros [sel sel1]]))

(enable-console-print!)

(defn create-context []
  (when-let [constructor (or js/window.AudioContext
                             js/window.webkitAudioContext)]
    (constructor.)))

(defonce ctx (create-context))

(def sound-timeout (atom nil))
(def vibration-timeout (atom nil))
(def osc-atom (atom nil))
(def gain-atom (atom nil))

(defn make-sound [overall-length]
  (let [dest (.-destination ctx)
        osc1 (.createOscillator ctx)
        gain1 (.createGain ctx)

        attack 15
        plateau 150
        decay 30
        pulse-length (+ attack plateau decay)
        final-pulse-length (* pulse-length 2)
        exponent 0.6

        current-time (.-currentTime ctx)
        stop-time (+ current-time (/ overall-length 1000))]

    (reset! osc-atom osc1)
    (reset! gain-atom gain1)

    (.connect gain1 dest)
    (.setValueAtTime (-> gain1 .-gain) 0 0)

    (loop [pulse-ramp-up-start current-time
           pause 2.5]
      (.setValueAtTime (-> gain1 .-gain) 0 pulse-ramp-up-start)
      (if (<= (+ pulse-ramp-up-start
                 (/ pulse-length 1000)
                 pause)
             stop-time)
        (let [pulse-plateau-start (+ pulse-ramp-up-start (/ attack 1000))
              pulse-ramp-down-start (+ pulse-plateau-start (/ plateau 1000))
              pulse-end (+ pulse-ramp-down-start (/ decay 1000))]
          (.linearRampToValueAtTime (-> gain1 .-gain) 0.5 pulse-plateau-start)
          (.setValueAtTime (-> gain1 .-gain) 0.5 pulse-ramp-down-start)
          (.linearRampToValueAtTime (-> gain1 .-gain) 0 pulse-end)
          (recur (+ pulse-end pause) (* pause exponent)))
        (do
          (.linearRampToValueAtTime (-> gain1 .-gain) 1 (+ pulse-ramp-up-start (/ attack 1000)))
          (.setValueAtTime (-> gain1 .-gain) 1 (+ stop-time (/ final-pulse-length 1000)))
          (.linearRampToValueAtTime (-> gain1 .-gain) 0 (+ stop-time
                                                           (/ final-pulse-length 1000)
                                                           (/ decay 1000))))))

    (set! (-> osc1 .-frequency .-value) 600)
    (set! (.-type osc1) "sine")
    (.connect osc1 gain1)
    (.start osc1 current-time)
    (.stop osc1 (+ stop-time
                   (/ final-pulse-length 1000)
                   (/ decay 1000)))))

(defn vibrate [length]
  (cond
     (.-vibrate js/navigator) (.vibrate js/navigator 1000)
     (.-webkitVibrate js/navigator) (.webkitVibrate js/navigator 1000)
     (.-mozVibrate js/navigator) (.mozVibrate js/navigator 1000)))

(defn start
  "Takes round length in seconds and percent at which
   notification should start"
  [round-length warning-percent]
  (let [round-length-msec (* 1000 round-length)
        target-time (+ (js/Date.now) round-length-msec)
        make-sound-callback (fn []
                              (let [duration (- target-time (js/Date.now))]
                                (when (> duration 100)
                                  (make-sound duration))))]
    (when ctx
      (reset! sound-timeout
              (js/setTimeout make-sound-callback
                             (* round-length-msec warning-percent))))
    (reset! vibration-timeout
            (js/setTimeout (partial vibrate 1000) round-length-msec))))

(defn stop []
  (when @sound-timeout
    (js/clearTimeout @sound-timeout)
    (reset! sound-timeout nil))
  (when @vibration-timeout
    (js/clearTimeout @vibration-timeout)
    (reset! vibration-timeout nil))
  (when @osc-atom
    (let [current-time (.-currentTime ctx)]
      (.linearRampToValueAtTime (-> @gain-atom .-gain) 0 (+ current-time 0.1))
      (.stop @osc-atom (+ current-time 0.1)))
    (reset! osc-atom nil)))

(defn unlock-notification []
  (dommy/listen-once!
   (sel1 :body)
   :touchstart
   (fn [evt]
     (let [dest (.-destination ctx)
           osc (.createOscillator ctx)
           gain (.createGain ctx)
           current-time (.-currentTime ctx)]
       (.connect gain dest)
       (.setValueAtTime (-> gain .-gain) 0 0)
       (.connect osc gain)
       (set! (-> osc .-frequency .-value) 5)
       (.start osc current-time)
       (.stop osc (+ current-time 0.01))))))

(defn is-ios? []
  (some? (re-find #"(iPad|iPhone|iPod)" js/navigator.userAgent)))
