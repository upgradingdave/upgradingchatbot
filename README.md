# UpgradingChatBot

Welcome!

I'm challenging myself to live stream on twitch every
Mon/Wed/Fri 7:30-8:30am EST. I'm fascinated with the clojure
programming language and excited to dive deeper. 

If you're interested in clojure, or any type of programming in
general, please come and hang out with us and help us test out this
code and let us know what projects you're working on.

# History

- This Month (May 2019) :: Testing and adding to the first version of
  our chat bot
- April 2019 :: Dave fumbling thru first live streams and trying to
  get over his fear of public speaking.

# Twitch Chat Bot

The Twitch Chat Bot is the first thing I built on the livestream. We
started working on this in April 2019.

## Commands

I love when people come by to help test our chatbot. You get double
points if you can break it. Please come by and try out these commands
during a livestream! 

Also, I'm always on the lookout for suggestions and ideas for cool
features to add next. (And if you're interested, checkout the
CHANGELOG.md for a list of ideas we might add soon)

- !help
- !play <search-for-a-sound-on-freesound.org>
- !stop 
- !so <username>
- Detect when someone joins. 
- Detect when someone leaves. 

# !play

- !play car crash
- (car crash) !play 237375
- (powerup) !play 431329 
- (8bit intro) !play 273539
- !play 2 yay
- !play yo
- !play boom
- !play Mexican Accent Male Adult "Badges? We don't need no stinking
  badges!" classic cinema houston
- !play doh
- !play sad trombone
- !play dude
- !play 1 dude
- !play timber
- !play jet
- !play 1 underwater

- Stand firm !play 397298
- Gold !play 161315
- Russian Festival !play 468218

## Development

This is an IRC Chatbot for Twitch written in clojure based on the Java
Kitteh IRC Client Library (KICL). 

To start it up, run: 

```
./scripts/run.sh
```

To start a repl: 

```
./scripts/repl.sh
```

## Streaming Notes

1. Sound to 100% and then set sound to "Multi Output Device" (from
   iShowU Audio Capture)
2. Set backgroud music volume so OBS shows -45 db. Keep OBS volume 100%
3. Set Mic volume in OBS so recording around -20 db (just above green)
4. Open the twitch dashboard
5. Start stream with "Starting Soon" OBS Scene
6. Setup chat view, and windows, Setup Desktop OBS Scene
7. Start chatbot


