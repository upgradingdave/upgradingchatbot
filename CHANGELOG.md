# TODO

- refactor to pass config around (maybe use component?)
- write a helper for reading config from file. add helpers for automatically decrypting

## Extensions Backend Service

- build http server to handle extension actions

## Chatbot

- Play command - make the download from freesound asynchronous. 
- By default, limit search results to 30 seconds or less
- Able to play any mp3 link? (how to filter though?)
- Tweak the scheduled messages. I'd like several different messages to 
  be broadcasted every few minutes
- catch exceptions when chatbot client loses connection
- Setup unit tests?
- If needed, refactor code to allow for connecting to multiple channels

### play!

- Dynamically generate help for sounds (maybe !sfx command)
- make sure input streams are closed

## Documentation Site

- I started setting up a github pages / jekyll site, but it's a
  mess. I'd much rather use a static code generator using clojure. i'd
  like to write my own!

## Write a todo list

- Write a simple clojure app to replace this list. Maybe use github issues?

# 2019-05-08 

- started working on extension backend
- after the stream, started refactoring to use `component` library

# 2019-05-06

- send periodic broadcast messages to let people know how to use chatbot
- cache sound files from freesound

# 2019-05-04

- chatbot will reply with a link to freesound.org

# 2019-05-03

- Able to specify freesound sound id (in addition to searches)
- If no results found, then run (play-not-found)
- Reorganized api so that it's possible to play complicated searches that have double quotes
- Use instaparse to parse chat commands (so cool!)
- Detect users joining
- Detect users leaving

# 2019-05-01 

- implemented !so command
- started implementing detection of user's joining the channel

