#!/usr/bin/env bash
set -e

if [[ "$#" -lt "2" ]]; then
  echo "Please supply at least 2 parameters: gchat-webhook-url [--thread threadID] text"
  echo "Text formatting: https://developers.google.com/chat/reference/message-formats/basic"
  exit 1
fi

gchat_url=$1
shift

if [[ "$1" == "--thread" ]]; then
  if [[ "$#" -lt "3" ]]; then
    echo "Not enough parameters supplied"
    exit 1
  fi
  #https://developers.google.com/chat/reference/rest/v1/spaces.messages/create
  gchat_url="$gchat_url&threadKey=$2"
  shift 2
fi

#https://developers.google.com/chat/reference/rest/v1/spaces.messages
gchat_json="{\"text\": \"$*\"}"

curl -X POST  -H 'Content-Type: application/json' "$gchat_url" -d "${gchat_json}" || true
