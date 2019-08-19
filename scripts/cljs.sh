#! /bin/bash

# The value of the CLJS_NAMESPACE must match a *.cljs.edn file.
VERSION="0.0.1"

version()
{
    echo ""
    echo "$0: version $VERSION"
    echo ""
}

help()
{
    echo ""
    echo "USAGE: $0 <options>"
    echo "  -c pass name of *.cljs.edn file"
    echo "  -h display this help"
    echo ""
}

while getopts hc: flag
do
    case "$flag" in
	(h) help; exit 0;;
	(c) CLJS_NAMESPACE="$OPTARG";;
    esac
done
shift $(expr $OPTIND - 1)

if [ -n "$CLJS_NAMESPACE" ]; then

    clj -R:figwheel -m figwheel.main --build "$CLJS_NAMESPACE" --repl

else
    
    echo "Please specify name of *.cljs.edn file to use."
    help; exit 1;
    
fi



