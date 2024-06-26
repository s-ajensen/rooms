(ns acme.room-spec
  (:require-macros [speclj.core :refer [redefs-around around stub should-have-invoked should-not-have-invoked with-stubs describe context it should= should-be-nil should-contain should should-not before should-not-be-nil]]
                   [c3kit.wire.spec-helperc :refer [should-not-select should-select]])
  (:require [accountant.core :as accountant]
            [acme.occupant :as occupant]
            [c3kit.wire.js :as wjs]
            [reagent.core :as reagent]
            [acme.dark-souls :as ds]
            [acme.init :as init]
            [acme.page :as page]
            [acme.room :as sut]
            [c3kit.wire.spec-helper :as wire]
            [c3kit.bucket.api :as db]
            [acme.state :as state]
            [acme.layout :as layout]
            [c3kit.wire.websocket :as ws]
            [acme.routes :as routes]))

(describe "Room"
  (init/install-reagent-db-atom!)
  (init/install-legend!)
  (init/configure-api!)
  (with-stubs)
  (wire/stub-ws)
  (wire/with-root-dom)
  (ds/with-schemas)
  (before (db/set-safety! false)
          (db/clear)
          (ds/init)
          (occupant/clear!)
          (sut/install-room! (:code @ds/firelink))
          (routes/load-page! :room)
          (wire/render [layout/default]))


  (it "fetches room on enter"
    (should-have-invoked :ws/call! {:with [:room/fetch {:room-code ds/firelink-code} db/tx*]}))

  (context "nickname prompt or room"
    (it "renders nickname prompt if no nickname"
      (should-select "#-nickname-prompt")
      (should-not-select "#-room"))

    (it "renders room if nickname"
      (occupant/install! @ds/frampt)
      (wire/flush)
      (should-not-select "#-nickname-prompt")
      (should-select "#-room")))

  (context "nickname prompt"
    (it "updates input on change"
      (wire/change! "#-nickname-input" "Lautrec")
      (should= "Lautrec" (wire/value "#-nickname-input"))
      (wire/change! "#-nickname-input" "Patches")
      (should= "Patches" (wire/value "#-nickname-input")))

    (context "button click"
      (it "joins room"
        (wire/change! "#-nickname-input" "Lautrec")
        (wire/click! "#-join-button")
        (should-have-invoked :ws/call! {:with [:room/join
                                               {:nickname "Lautrec" :room-code ds/firelink-code}
                                               occupant/receive-join!]}))

      (it "doesn't join room if blank nickname"
        (wire/change! "#-nickname-input" " ")
        (wire/click! "#-join-button")
        (should-not-have-invoked :ws/call!))))

  (context "room"
    (before (occupant/install! @ds/lautrec)
            (wire/flush))

    (context "displays occupants"

      (it "with one occupant"
        (let [frampt @ds/frampt]
          (run! db/delete (db/find :occupant))
          (db/tx frampt)
          (wire/flush)
          (should= "Kingseeker Frampt" (wire/html (str "#-occupant-" (:id frampt))))))

      (it "with multiple occupants"
        (should= "Lautrec" (wire/html (str "#-occupant-" (:id @ds/lautrec-atom))))
        (should= "Kingseeker Frampt" (wire/html (str "#-occupant-" (:id @ds/frampt-atom))))
        (should= "Patches" (wire/html (str "#-occupant-" (:id @ds/patches))))))))