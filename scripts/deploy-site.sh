#!/usr/bin/env bash
make html-author-mode
eval "$(ssh-agent -s)"
echo $SCP_SECRET | base64 -d > /tmp/id_rsa
chmod 600 /tmp/id_rsa
scp -i /tmp/id_rsa -o UserKnownHostsFile=.travis/known_hosts -r target/staging/* akkarepo@gustav.akka.io:akka.io/platform-guide
