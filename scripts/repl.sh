#! /bin/bash

clj -R:repl -m nrepl.cmdline --middleware "[cider.nrepl/cider-middleware]"
