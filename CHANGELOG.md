# TODO

- write a helper for reading config from file. add helpers for
  automatically decrypting
- implement command line parsing for main 

## Extensions Backend Service

- parse jwt token in backend
- enable https? (when needed, I'll handle this with apache or nginx)

## Chatbot

- Add help for all other commands (we already did !play)
- And also allow for this syntax: !help !play as well as !help play
- Tweak the scheduled messages. I'd like several different messages to 
  be broadcasted every few minutes. 
- Play command - make the download from freesound asynchronous. 
- By default, limit search results to 30 seconds or less
- Able to play any mp3 link? (how to filter though?)
- catch exceptions when chatbot client loses connection
- Setup unit tests?
- If needed, refactor code to allow for connecting to multiple channels

### play!

- make sure input streams are closed

## Documentation Site

- I started setting up a github pages / jekyll site, but it's a
  mess. I'd much rather use a static code generator using clojure. i'd
  like to write my own!

## Write a todo list

- Write a simple clojure app to replace this list. Maybe use github issues?

# Complete

## 2019-05-19 (off stream)

- cljs version of panel.html is working against nodejs backend I can
  run the simple "change color circle" example by using the twitch
  developer rig, and starting the front end using my version of
  panel.html (which uses cljs). My front end calls their nodejs
  backend and it works!
- removed component lib. I decided it's not really helping. I can write
  start and stop myself
- added clojure.java-time and buddy dependencies 

## 2019-05-18 (off stream)

- basic color changer cljs/clj webapp is working (but no twitch integration yet)
- added re-frame, http-fx and coded basic re-frame app
- added ring-json dependency. now the http server will automatically
  return json if clojure data structures are returned in respsonse
  body
- add ring-mock

## 2019-05-17 (on stream)

- Added !today and !welcome commands
- Started implementing "change color" twitch example in clojurescript

## 2019-05-15 (on stream)

- Dynamically generate help for sounds. You can now type 
  `!help play` for extra help

## 2019-05-11 (off stream)

- Setup clojurescript compilation using figwheel

## 2019-05-10

- Created very basic http server using httpkit, and bidi
- Before the stream, refactored code to use the component library

## 2019-05-08 

- started working on extension backend
- after the stream, started refactoring to use `component` library

## 2019-05-06

- send periodic broadcast messages to let people know how to use chatbot
- cache sound files from freesound

## 2019-05-04

- chatbot will reply with a link to freesound.org

## 2019-05-03

- Able to specify freesound sound id (in addition to searches)
- If no results found, then run (play-not-found)
- Reorganized api so that it's possible to play complicated searches that have double quotes
- Use instaparse to parse chat commands (so cool!)
- Detect users joining
- Detect users leaving

## 2019-05-01 

- implemented !so command
- started implementing detection of user's joining the channel

