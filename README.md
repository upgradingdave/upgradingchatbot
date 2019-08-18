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

3. OBS Webpage Overlays 

The Http Server also hosts several web pages that use clojurescript
and javascript libraries to run animations and other fun stuff. These
webpages can be setup as overlays in OBS so they will appear during
the stream. The irc code can communicate with these pages via
websockets.

4. Twitch Extension

I've started to implement a twitch extention, but so far it doesnt do
much and I'm not using it yet.

5. Twitch api

I'm using the v5 and new Twitch API to do things like find emoticon
images, listen for new followers, etc.

## Floating Bouncing Emoticons

Whenever someone types a emote in chat, the chatbot communicates with
a web page and http server over websockets to display a P5.js
animation of the emote icon floating around the page.

When you configured this url as an overlay inside OBS, so it looks
like emotes float around the screen during the stream.

If you're running this code locally, you can see the webpage here: 

http://localhost:8081/emoticons.html

## Follower Animations

https://clips.twitch.tv/EsteemedBoxyGarageLitty

This code subscribes to Twitch so that Twitch will send a http request
whenever someone clicks to follow the channel. If this happens during
a live stream, the http server code plays a mp3 and displays an
animation on a web page. When this webpage is setup as an overlay in
OBS, then new followers are greeted by the animation during live streams.

If you're running this code locally, you can see the webpage here: 

http://localhost:8081/followers.html

## Chat Overlay

I built a webpage that displays a custom version of the twitch irc
chat. The twitchbot monitors all the activity in the chat during a
stream, and sends the messages via websockets to the http server,
which then broadcasts the messages to the chat web page. The chat web
page can be setup as a OBS overlay so that people can view the chat
activity live during the stream.

If you're running this code locally, you can see the webpage here: 

http://localhost:8081/chat.html

The chat will automatically scroll to the bottom unless you scroll up
by a certainamount of pixels (I learned a lot when writing this and it
took me a while to get this to work correctly!)

## Commands

Commands are always changing and improving. Use !help for a list of
commands. You can also type `!help <command>` for more detailed help
on each command

Here's a (probably outdated) list of commands: 

- !help
- !play <search-for-a-sound-on-freesound.org>
- !play <name-of-local-mp3-file>
- !play <any-url-to-mp3-file>
- !stop
- !so <username>
- !welcome <username>
- !today

### !play

The !play command will play a mp3 file. There are 3 ways to load mp3
files. 

First, the `play` command will search `http://freesound.org` and if it
finds a result, it will play the sound during the stream. Checkout the
examples below.

Second, you can also pass the `!play` command a url to a mp3 file and
it will download and play.

Third, you can pass the `!play` command any url to a mp3 file and it
will download and play.

Here's a few freesound.org examples: 

- !play dude
- !play 424927 (Drum roll)
- !play 431329 (powerup) 
- !play 273539 (8bit intro)
- !play Mexican Accent Male Adult "Badges? We don't need no stinking
  badges!" classic cinema houston
- !play doh
- !play sad trombone

Here's an example of playing an mp3 from a url: 

- !play https://raw.githubusercontent.com/clarkio/ttv-chat-light/master/src/assets/sounds/inconceivable.mp3

Oh yeah, there's a fourth way as well! You can also play any mp3's
that have been downloaded and stored under `./mp3` directory. For
example, if `./mp3/applause.mp3` exists, then you can do this:

- !play applause

Here are some more freesound examples:

- Stand firm !play 397298
- Drunks fighting !play 15559
- long sword fight !play 192072
- medievalcombat !play 218522
- dragon !play 249686

# CSS Animations

CSS Animations in react / reagent / re-frame are sort of difficult.

Here's a good overview of css animations: 
https://javascript.info/css-animations

Here's what I am using: 
https://reactcommunity.org/react-transition-group/
https://github.com/reactjs/react-transition-group

Here are sites for inspiration:
http://animista.net/play/entrances
https://github.com/daneden/animate.css/blob/master/animate.css

# Development

If you want to download and try this out for your own stream, you can start a clojure repl like this: 

```
./scripts/repl.sh
```

And then start things up like this: 

```
(ns upgrade.system)
(start-system!)
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

## History

- Aug 2019 :: The web chat overlay is finally working the way I
  want. Auto scrolling was a bit more challenging than I expected, but
  it's working nicely. I still want to style the web chat to be more
  nautical, but pretty happy ... I'll continue to add styles and
  overlays for the rest of this month.

- July 2019 :: Worked more on the web version of chat, I didn't have
  much time to stream in July because of work and vacation.

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

## Streaming Notes

1. Open the twitch dashboard
2. Update the "Going Live" message
3. Update the "!today" message
4. Sound to 100% and then set sound to "Multi Output Device" (from
   iShowU Audio Capture)
5. Set backgroud music volume so OBS shows -40 db. Keep OBS volume 100%
6. Set Mic volume in OBS so recording around -20 db (just above green)
7. Start chatbot
8. Remove url from overlays and add them back
9. Start stream with "Starting Soon" OBS Scene
10. Setup chat view, and windows, Setup Desktop OBS Scene

## Royalty Free Music

- https://incompetech.com/music/royalty-free/music.html (need to give credit)
- https://mobygratis.com/catalog
- http://dig.ccmixter.org/dig
- Thanks to @codephobia https://play.google.com/music/listen?u=0#/pl/AMaBXykLsd-ZNaKyMRXUmmFkuqD_tGt2L0MK5GpTtbeBZ7lYFQoG5ksbHSYRQArXxV7-UwMGbDD2X_NcHVvfa9b_DNXE0SMwaQ%3D%3D

## Awesomeness

- Get better at Touch Typing https://zty.pe/
- Which Software License should you use? https://choosealicense.com/

# Software related concepts
- https://stackoverflow.com/questions/8307370/functional-lenses
- https://wiki.haskell.org/Rank-N_types
- https://en.wikipedia.org/wiki/Church_encoding
- https://htdp.org
