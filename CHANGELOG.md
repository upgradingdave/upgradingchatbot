# TODO

## Chatbot

- catch exceptions when chatbot client loses connection
- refactor to pass config around (maybe use components?)
- Setup unit tests?
- Dynamically generate help for sounds (maybe !sfx command)
- Cache sound files from freesound

## Documentation Site

- I started setting up a github pages / jekyll site, but it's a
  mess. I'd much rather use a static code generator using clojure. i'd
  like to write my own!

## Write a todo list

- Write a simple clojure app to replace this list. Maybe use github issues?

# 2019-05-06

- send periodic broadcast messages to let people know how to use chatbot


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

