# UpgradingChatBot

Welcome! This is a chatbot that I developed live on my Twitch Stream
at https://www.twitch.tv/upgradingdave

# Getting started with clojure

In case you're interested in getting started with clojure, please
checkout the following links:

- https://www.braveclojure.com/
- https://jakegny.gitbooks.io/learning-clojure/content/

# What does this do?

This Twitch Chat Bot is the first thing I built live on stream. I
started working on this in April 2019 and will probably be adding to
it and updating it for a long time. If you have ideas for new
features, please stop by my livestream and let me know.

There are a few main pieces that make up this chatbot. 

1. IRC Bot

I'm using a java library called the Kitteh IRC Client Library
(KICL). KICL will raise events whenever there's any activity in an irc
chat. This bot looks at each message and can react as needed. 

2. Http Server

I'm using a clojure library called httpkit to handle http
requests. The Http server listens for webhook requests from Twitch
(for example whenever there's a new follower).

3. Clojurescript / Javascript Web Pages and Animations

The Http Server also hosts several web pages that use clojurescript
and javascript libraries to run animations and other fun stuff. The
irc code can communicate with these pages via websockets.

4. Twitch Extension

I've started to implement a twitch extention, but so far it doesnt do
much and I'm not using it yet.

5. Twitch api

I'm using the v5 and new Twitch API to do things like find emoteicon
images, listen for new followers, etc.

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

### !play

Currently, the `play` command will search `http://freesound.org` and
if it find a result, it will play the sound during the stream.

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

# History

- June 2019 :: The goals for this month is to have animation play when
  new followers follow me and/or new subscribers subscribe. I'd also
  like to implement my own web version of the chat to display as an
  overlay.
  
- May 2019 :: Starting to develop my own clojure(script)
  twitch chatbot. Added a bunch of commands. Wrote a basic Twitch
  Extension using clojure and clojurescript. Also added 2d animated
  bouncing emoticons in clojurescript (using processing.js)

- April 2019 :: Dave fumbling thru first live streams and trying to
  get over his fear of public speaking.

# Development

This is an IRC Chatbot for Twitch written in clojure based on the Java
Kitteh IRC Client Library (KICL). 

To use it, start a repl like this: 

```
./scripts/repl.sh
```

And then start things up like this: 

```
(ns upgrade.system)
(start-twitchbot!)
(start-httpkit!)
```

# My Twitch Live Stream

Out of everything I've learned over my career, nothing has had a
bigger impact on the way I program (and even the way I think about
problems) than Clojure and functional programming. I'm completely
fascinated with the clojure programming language. 

I'm challenging myself to live stream on twitch every Mon/Wed/Fri
7:30-8:30am EST for year. 

If you're interested in clojure, (or any type of programming for that
matter!), please come hang out and say hi. 

## Streaming Notes

1. Open the twitch dashboard
2. Update the "Going Live" message
3. Update the "!today" message
4. Sound to 100% and then set sound to "Multi Output Device" (from
   iShowU Audio Capture)
5. Set backgroud music volume so OBS shows -40 db. Keep OBS volume 100%
6. Set Mic volume in OBS so recording around -20 db (just above green)
7. Start stream with "Starting Soon" OBS Scene
8. Setup chat view, and windows, Setup Desktop OBS Scene
9. Start chatbot

## Royalty Free Music

- https://incompetech.com/music/royalty-free/music.html (need to give credit)
- https://mobygratis.com/catalog
- http://dig.ccmixter.org/dig
- Thanks to @codephobia https://play.google.com/music/listen?u=0#/pl/AMaBXykLsd-ZNaKyMRXUmmFkuqD_tGt2L0MK5GpTtbeBZ7lYFQoG5ksbHSYRQArXxV7-UwMGbDD2X_NcHVvfa9b_DNXE0SMwaQ%3D%3D

## Awesomeness

- Get better at Touch Typing (mentioned by @roberttables during
  @noopkat feed) https://zty.pe/
- Which Software License should you use? https://choosealicense.com/

## Functional concepts to learn
- https://stackoverflow.com/questions/8307370/functional-lenses
- https://wiki.haskell.org/Rank-N_types
