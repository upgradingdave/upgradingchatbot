#! /bin/bash

# How to run multiple builds? Right now, I just start multiple processes
clj -R:figwheel -m figwheel.main --build followers --repl

