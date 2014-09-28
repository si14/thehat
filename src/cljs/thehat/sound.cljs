(ns thehat.sound
  (:require [figwheel.client :as fw :include-macros true]
            [dommy.core :as dommy])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(enable-console-print!)

;; var envelope = audioContext.createGainNode();
;; mySoundNode.connect( envelope );
;; envelope.connect( audioContext.destination );

;; var now = audioContext.currentTime;
;; envelope.gain.setValueAtTime( 0, now );
;; envelope.gain.linearRampToValueAtTime( 1.0, now + 2.0 );
;; envelope.gain.linearRampToValueAtTime( 0.0, now + 4.0 );
;; mySoundNode.noteOn(0);

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

(defn make-sound []
  (let [dest (.-destination ctx)
        osc1 (.createOscillator ctx)
        osc2 (.createOscillator ctx)
        gain1 (.createGain ctx)
        gain2 (.createGain ctx)
        filter1 (.createBiquadFilter ctx)
        attack 15
        plateau 200
        decay 50
        pulse-length (+ attack plateau decay)
        overall-length 5000
        exponent 0.75
        pattern (->> overall-length
                     (iterate (partial * exponent))
                     (take-while (fn [x] (> (- (/ x exponent) x)
                                            (* pulse-length exponent))))
                     (map #(- overall-length %))
                     (map #(/ % 1000)))
        current-time (.-currentTime ctx)]

    (prn pattern)

    ;; (.connect gain1 dest)
    ;; (.setValueAtTime (-> gain1 .-gain) 0 (.-currentTime ctx))
    ;; (.linearRampToValueAtTime (-> gain1 .-gain) 1 (+ (.-currentTime ctx) (/ attack 1000)))
    ;; (.linearRampToValueAtTime (-> gain1 .-gain) 0 (+ (.-currentTime ctx) (/ decay 1000)))


    (.connect gain1 dest)
    (.setValueAtTime (-> gain1 .-gain) 0 current-time)
    (doseq [start (butlast pattern)
            :let [pulse-ramp-up-start (+ current-time start)
                  pulse-plateau-start (+ pulse-ramp-up-start (/ attack 1000))
                  pulse-ramp-down-start (+ pulse-plateau-start (/ plateau 1000))
                  pulse-end (+ pulse-ramp-down-start (/ decay 1000))]]
      (prn pulse-ramp-up-start pulse-plateau-start
           pulse-ramp-down-start pulse-end)
      (.setValueAtTime (-> gain1 .-gain) 0 pulse-ramp-up-start)
      (.linearRampToValueAtTime (-> gain1 .-gain) 1 pulse-plateau-start)
      (.setValueAtTime (-> gain1 .-gain) 1 pulse-ramp-down-start)
      (.linearRampToValueAtTime (-> gain1 .-gain) 0 pulse-end))

    (let [last-pulse-start (+ current-time (last pattern))
          pulse-plateau-start (+ last-pulse-start (/ attack 1000))]
      (prn last-pulse-start pulse-plateau-start (+ current-time (/ overall-length 1000)))
      (.setValueAtTime (-> gain1 .-gain) 0 last-pulse-start)
      (.linearRampToValueAtTime (-> gain1 .-gain) 1 pulse-plateau-start))

    ;; (.linearRampToValueAtTime (-> gain1 .-gain) 1 (+ (.-currentTime ctx) 1))
    ;; (.linearRampToValueAtTime (-> gain1 .-gain) 0 (+ (.-currentTime ctx) 2))

    (set! (-> osc1 .-frequency .-value) 500)
    (set! (.-type osc1) "sine")
    (.connect osc1 gain1)
    (.start osc1 current-time)
    (.stop osc1 (+ current-time (/ overall-length 1000)))

    (set! (-> osc2 .-frequency .-value) 600)
    (set! (.-type osc2) "sine")
    (.connect osc2 dest)
    (.start osc2 (+ (.-currentTime ctx) (/ overall-length 1000)))
    (.stop osc2 (+ (.-currentTime ctx) (/ overall-length 1000)  (/ decay 1000)))


    ;; (set! (-> osc1 .-frequency .-value) 400)
    ;; (set! (.-type osc1) "sine")
    ;; (.connect osc1 gain1)
    ;; (.start osc1 0)

    ;; (set! (-> gain .-gain .-value) 100)
    ;; (.connect gain (.-frequency osc1))

    ;; (set! (-> osc2 .-frequency .-value) 1)
    ;; (.connect osc2 gain)
    ;; (.start osc2 0)

    (js/setTimeout (fn [] (.stop osc1 0) (.disconnect osc1 dest) (prn "stop")) 6000))
  )

(fw/watch-and-reload)

(make-sound)
