(ns acme.room
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.wire.js :as wjs]
            [c3kit.wire.websocket :as ws]
            [clojure.string :as str]
            [reagent.core :as reagent]
            [c3kit.bucket.api :as db]
            [acme.page :as page]
            [acme.state :as state]))

(defn- join-room! []
  (when (not (str/blank? @state/nickname))
    (ws/call! :room/join
              {:nickname @state/nickname :room-code (:room-code @page/state)}
              db/tx*)))

(defn nickname-prompt [_]
  (let [local-nickname-ratom (reagent/atom nil)]
    (fn [nickname-ratom]
      [:div.center-div.margin-top-plus-5
       {:id "-nickname-prompt"}
       [:h1 "Enter nickname to join room..."]
       [:div.center
        [:input {:type "text"
                 :id "-nickname-input"
                 :placeholder "Enter your nickname"
                 :value @local-nickname-ratom
                 :on-change #(reset! local-nickname-ratom (wjs/e-text %))}]
        [:button {:id "-join-button"
                  :on-click #(do (reset! nickname-ratom @local-nickname-ratom)
                                 (join-room!))}
         "Join"]]])))


(defn room [occupants-ratom]
  [:div.main-container
   {:id "-room"}
   [:div.left-container
    [:br]
    [:br]
    [:h3 "occupants"]
    [:ul
     [:<>
      (ccc/for-all [occupant @occupants-ratom]
        [:li {:key (:id occupant)
              :id  (str "-occupant-" (:id occupant))}
         (:nickname occupant)])]]]
   [:div.center
    [:div.game-container
     [:h1 "acme"]
     ; TODO - counter here
     ]]])

(defn nickname-prompt-or-room [nickname-ratom]
  [:div {:id "-prompt-or-room"}
   (if (str/blank? @nickname-ratom)
     [nickname-prompt nickname-ratom]
     [room state/occupants])])

(defn- fetch-room []
  (ws/call! :room/fetch
            {:room-code (:room-code @page/state)}
            db/tx*))

(defmethod page/entering! :room [_]
  (fetch-room))

(defmethod page/render :room [_]
  [nickname-prompt-or-room state/nickname])