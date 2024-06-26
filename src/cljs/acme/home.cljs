(ns acme.home
  (:require [accountant.core :as accountant]
            [acme.occupant :as occupant]
            [c3kit.apron.corec :as ccc]
            [c3kit.bucket.api :as db]
            [c3kit.wire.js :as wjs]
            [c3kit.wire.websocket :as ws]
            [clojure.string :as str]
            [acme.state :as state]
            [acme.page :as page]))

(defn join-room! [[code]]
  (ws/call! :room/join {:nickname @state/nickname :room-code code}
            occupant/receive-join!)
  (accountant/navigate! (str "/room/" code)))

(defn- create-room! [nickname]
  (when (not (str/blank? nickname))
    (ws/call! :room/create {:nickname nickname} join-room!)))

(defn home [nickname-ratom]
  [:div.homepage-container
   [:h1 "Welcome to acme"]
   [:div.nickname-input
    [:input {:type "text"
             :id "-nickname-input"
             :placeholder "Enter your nickname"
             :value @nickname-ratom
             :on-change #(reset! nickname-ratom (wjs/e-text %))}]]
   [:div.room-actions
    [:button {:id       "-create-room-button"
              :on-click #(create-room! @nickname-ratom)}
     "Create Room"]]])

(defmethod page/render :home [_]
  [home state/nickname])