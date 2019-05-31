# UpgradingChatBot

Welcome! This is a chatbot that I developed on my Twitch Stream at
https://www.twitch.tv/upgradingdave

# Getting started with clojure

In case you're interested in getting started with clojure, please checkout the following links: 

- https://www.braveclojure.com/
- https://jakegny.gitbooks.io/learning-clojure/content/

# About Dave

I've been a software developer and consultant since around 2001. Over
the years, I've worked mostly with Java and Javascript and Web
Technologies, but I've also dabbled with a ton of other languages and
frameworks including php, perl, ruby, groovy, and haskell.

Out of everything I've learned, there is one language and community
that has outshined them all ... Clojure! I'm completely fascinated
with the clojure programming language. And it's safe to say that I've
learned more about computer science and software development from the
clojure language and community than from anything else in my career so
far.

I'm challenging myself to live stream on twitch every Mon/Wed/Fri
7:30-8:30am EST.

If you're interested in clojure, (or any type of programming for that
matter!), please come hang out and say hi. 

# History

- This Month (May 2019) :: Starting to develop my own clojure(script)
  twitch chatbot. Added a bunch of commands. Wrote a basic Twitch
  Extension using clojure and clojurescript. Also added 2d animated
  bouncing emoticons in clojurescript (using processing.js)

- April 2019 :: Dave fumbling thru first live streams and trying to
  get over his fear of public speaking.

# About the Chat Bot

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
