(ns acme.room-spec
  (:require-macros [speclj.core :refer [redefs-around around stub should-have-invoked should-not-have-invoked with-stubs describe context it should= should-be-nil should-contain should should-not before should-not-be-nil]]
                   [c3kit.wire.spec-helperc :refer [should-not-select should-select]])
  (:require [accountant.core :as accountant]
            [c3kit.apron.time :as time]
            [c3kit.wire.js :as wjs]
            [reagent.core :as reagent]
            [acme.dark-souls :as ds]
            [acme.init :as init]
            [acme.page :as page]
            [acme.room :as sut]
            [c3kit.wire.spec-helper :as wire]
            [c3kit.bucket.api :as db]
            [acme.state :as state]
            [c3kit.wire.websocket :as ws]))

(def occupants-ratom (reagent/atom []))
(def room-ratom (reagent/atom {}))

(describe "Room"
  (init/install-reagent-db-atom!)
  (init/install-legend!)
  (init/configure-api!)
  (with-stubs)
  (wire/stub-ws)
  (wire/with-root-dom)
  (ds/init-with-schemas)
  (before (db/set-safety! false)
          (db/clear)
          (reset! state/nickname nil)
          (reset! occupants-ratom [])
          (reset! room-ratom {}))


  (it "fetches room on enter"
    (page/install-room! "A8SBLK")
    (should-be-nil @state/room)
    (page/entering! :room)
    (should-have-invoked :ws/call! {:with [:room/fetch {:room-code "A8SBLK"} db/tx*]}))

  (context "nickname prompt or room"
    (before (wire/render [sut/nickname-prompt-or-room state/nickname]))

    (it "renders nickname prompt if no nickname"
      (should-select "#-nickname-prompt")
      (should-not-select "#-room"))

    (it "renders room if nickname"
      (reset! state/nickname "Lautrec")
      (wire/flush)
      (should-not-select "#-nickname-prompt")
      (should-select "#-room")))

  (context "nickname prompt"
    (before (wire/render [sut/nickname-prompt state/nickname]))

    (it "updates input on change"
      (wire/change! "#-nickname-input" "Lautrec")
      (should= "Lautrec" (wire/value "#-nickname-input"))
      (wire/change! "#-nickname-input" "Patches")
      (should= "Patches" (wire/value "#-nickname-input")))

    (context "button click"

      (context "updates nickname ratom"
        (it "when nickname is Lautrec"
          (wire/change! "#-nickname-input" "Lautrec")
          (should= nil @state/nickname)
          (wire/click! "#-join-button")
          (should= "Lautrec" @state/nickname))

        (it "when nickname is Patches"
          (wire/change! "#-nickname-input" "Patches")
          (should= nil @state/nickname)
          (wire/click! "#-join-button")
          (should= "Patches" @state/nickname)))

      (it "joins room"
        (wire/change! "#-nickname-input" "Lautrec")
        (wire/click! "#-join-button")
        (should-have-invoked :ws/call! {:with [:room/join
                                               {:nickname "Lautrec" :room-code "A8SBLK"}
                                               db/tx*]}))

      (it "doesn't join room if blank nickname"
        (wire/change! "#-nickname-input" " ")
        (should= nil @state/nickname)
        (wire/click! "#-join-button")
        (should-not-have-invoked :ws/call!))))

  (context "room"
    (before (wire/render [sut/room occupants-ratom]))

    (context "displays occupants"

      (it "with one occupant"
        (reset! occupants-ratom [@ds/frampt-atom])
        (wire/flush)
        (should= "Kingseeker Frampt" (wire/html (str "#-occupant-" (:id @ds/frampt-atom)))))

      (it "with multiple occupants"
        (reset! occupants-ratom [@ds/frampt-atom @ds/lautrec-atom])
        (wire/flush)
        (should= "Kingseeker Frampt" (wire/html (str "#-occupant-" (:id @ds/frampt-atom))))
        (should= "Lautrec" (wire/html (str "#-occupant-" (:id @ds/lautrec-atom))))))))