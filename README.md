# UpgradingChatBot

Welcome!

I'm challenging myself to live stream on twitch every Mon/Wed/Fri
7:30-8:30am EST. I'm fascinated with the clojure programming language
and I've learned so much from the clojure community.

If you're interested in clojure, (or any type of programming for that
matter!), please come hang out and say hey

# History

- This Month (May 2019) :: Starting to develop my own clojure(script)
  twitch chatbot.

- April 2019 :: Dave fumbling thru first live streams and trying to
  get over his fear of public speaking.

# Twitch Chat Bot

The Twitch Chat Bot is the first thing I built live on stream. I
started working on this in April 2019.

I love when people come by and play around with the chatbot. You get
double points if you can break it. Please come by and try out some of
the commands below during a livestream!

I'm always on the lookout for suggestions and ideas for fun new
features to add next. Checkout the CHANGELOG.md for a running list
of ideas I might add soon.

## Emote Floating Bouncing Heads 

Whenever someone types a emote in chat, the chatbot communicates with
a web page and http server over websockets to display a P5.js
animation of the emote icon floating around the page.

Dave has this page configured as an overlay inside OBS, so it looks
like emotes float around the screen during the stream.

## Commands

Commands are always changing and improving. Use !help for a list of
commands. You can also type `!help <command>` for more detailed help
on each command

Here's a (probably outdated) list of commands: 

- !help
- !play <search-for-a-sound-on-freesound.org>
- !stop 
- !so <username>
- !welcome <username>
- !today

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

1. Open the twitch dashboard
2. Update the "Going Live" message
3. Sound to 100% and then set sound to "Multi Output Device" (from
   iShowU Audio Capture)
4. Set backgroud music volume so OBS shows -40 db. Keep OBS volume 100%
5. Set Mic volume in OBS so recording around -20 db (just above green)
6. Start stream with "Starting Soon" OBS Scene
7. Setup chat view, and windows, Setup Desktop OBS Scene
8. Start chatbot

## Royalty Free Music

- https://incompetech.com/music/royalty-free/music.html (need to give credit)
- https://mobygratis.com/catalog
- http://dig.ccmixter.org/dig




