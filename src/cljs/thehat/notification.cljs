(ns thehat.notification
  (:require [figwheel.client :as fw :include-macros true]
            [dommy.core :as dommy])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(enable-console-print!)

;; ---

;; var filter = audioContext.createBiquadFilter();
;; filter.type = filter.LOWPASS;
;; filter.frequency = 0;
;; filter.Q = 0;

;; // sweep the frequency from 0-5k; sweep the Q from 20 to 0.
;; var now = audioContext.currentTime;
;; filter.frequency.setValueAtTime( 0, now );
;; filter.frequency.linearRampToValueAtTime( 2000.0, now + 2.0 );
;; filter.frequency.linearRampToValueAtTime( 0.0, now + 4.0 );

;; filter.Q.setValueAtTime( 20.0, now );
;; filter.Q.linearRampToValueAtTime( 10.0, now + 4 );

;;; ---

;; var wavetable = audioContext.createWaveTable( Float32Array real, Float32Array imag );
;; oscillator.setWaveTable( wavetable );

(defn create-context []
  (let [constructor (or js/window.AudioContext
                        js/window.webkitAudioContext)]
    (constructor.)))

(defonce ctx (create-context))

(defn make-sound [overall-length]
  (let [dest (.-destination ctx)
        osc1 (.createOscillator ctx)
        osc2 (.createOscillator ctx)
        gain1 (.createGain ctx)
        gain2 (.createGain ctx)

        attack 15
        plateau 150
        decay 30
        pulse-length (+ attack plateau decay)
        final-pulse-length (* pulse-length 2)
        exponent 0.5

        current-time (.-currentTime ctx)
        stop-time (+ current-time (/ overall-length 1000))]

    (.connect gain1 dest)
    (.setValueAtTime (-> gain1 .-gain) 0 0)

    (loop [pulse-ramp-up-start current-time
           pause 1.25]
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
    (.stop osc1 (+ stop-time (/ final-pulse-length 1000)
                   (/ decay 1000)))))

(defn vibrate [length]
  (cond
     (.-vibrate js/navigator) (.vibrate js/navigator 1000)
     (.-webkitVibrate js/navigator) (.webkitVibrate js/navigator 1000)
     (.-mozVibrate js/navigator) (.mozVibrate js/navigator 1000)))

(defn start-notifying []
  (let [overall-length 5000]
    (make-sound overall-length)
    (js/setTimeout #(vibrate 1000) overall-length)))
