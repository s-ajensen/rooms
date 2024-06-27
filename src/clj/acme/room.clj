(ns acme.room
  (:require [c3kit.bucket.api :as db]
            [c3kit.wire.apic :as apic]
    ;[acme.categories :as categories]
            [acme.dispatch :as dispatch]
            [acme.occupantc :as occupantc]
            [acme.roomc :as roomc]))

(def lock (Object.))
(defmacro with-lock [& body]
  `(locking lock
     ~@body))

(def code-chars
  (->> (concat (range 48 58) (range 65 91))
       (map char)
       (remove #{\O \0 \1 \I \G \g})))

(defn new-code []
  (->> (repeatedly #(rand-nth code-chars))
       (take 6)
       (apply str)))

(defn unused-code []
  (->> (repeatedly new-code)
       (remove #(db/ffind-by :room :code %))
       first))

(defn categories []
  (shuffle []))

(defn ws-create-room [{:keys [params] :as request}]
  (with-lock
    (let [code (unused-code)]
      (roomc/create-room! code)
      (apic/ok [code]))))

(defn maybe-missing-room [{:keys [room-code] :as params}]
  (when-not room-code (apic/fail nil "Missing room!")))
(defn maybe-nonexistent-room [room]
  (when-not room (apic/fail nil "Room does not exist!")))
(defn maybe-missing-nickname [{:keys [nickname] :as params}]
  (when-not nickname (apic/fail nil "Missing nickname!")))

(defn ws-fetch-room [{:keys [params] :as request}]
  (let [room (db/ffind-by :room :code (:room-code params))
        occupants (map db/entity (:occupants room))]
    (or (maybe-missing-room params)
        (maybe-nonexistent-room room)
        (apic/ok (cons room occupants)))))

(defn push-to-room!
  ([room payload]
   (push-to-room! room payload :room/update))
  ([room payload method]
   (let [occupants (map db/entity (:occupants room))]
     (dispatch/push-to-occupants! occupants method payload))))

(defn push-room! [room]
  (push-to-room! room [room]))

(defn- create-and-join! [room nickname connection-id]
  (let [occupant (occupantc/create-occupant! nickname connection-id)
        room (roomc/add-occupant! room occupant)
        occupants (map db/entity (:occupants room))]
    (push-to-room! room [room occupant])
    (apic/ok (cons room occupants))))

(defn- assign-to-room! [{:keys [room-code nickname]} connection-id]
  (let [room (db/ffind-by :room :code room-code)]
    (or (maybe-nonexistent-room room)
        (create-and-join! room nickname connection-id))))

(defn ws-join-room [{:keys [params connection-id] :as request}]
  (with-lock
    (or (maybe-missing-room params)
        (maybe-missing-nickname params)
        (assign-to-room! params connection-id))))

(defn ws-leave-room [{:keys [connection-id] :as request}]
  (with-lock
    (when-let [occupant (occupantc/by-conn-id connection-id)]
      (let [room (roomc/by-occupant occupant)
            room (roomc/remove-occupant! room occupant)]
        (push-room! room)
        (roomc/remove-occupant! room occupant)))))