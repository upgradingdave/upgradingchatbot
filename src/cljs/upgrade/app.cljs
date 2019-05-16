(ns upgrade.app)

(-> js/document
    (.getElementById "main")
    (.-innerHTML)
    (set! "Today, we'll be coding a twitch extension in cljs!"))
