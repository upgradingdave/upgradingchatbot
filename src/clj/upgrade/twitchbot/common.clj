(ns upgrade.twitchbot.common
  (:import [upgrade.encrypt EncryptionManager]))

(defn encrypt [message]
  (.. (EncryptionManager.) (encrypt (java.io.File. "./mykeyfile") message)))

(defn decrypt [message]
  (.. (EncryptionManager.) (decrypt (java.io.File. "./mykeyfile") message)))


