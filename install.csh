#! /bin/csh -f


ls -l /pro/bubjet/build/libs/bubjet-0.0.1.jar


gradle buildPlugin

if $? == 0 then
   cp /pro/bubjet/build/libs/bubjet-0.0.1.jar /Applications/IntelliJ*/Contents/plugins/bubbles/lib
   cp /pro/ivy/lib/ivy.jar /Applications/IntelliJ*/Contents/plugins/bubbles/lib
   cp /pro/bubjet/build/libs/bubjet-0.0.1.jar /pro/bubbles/lib/bubjet.jar
   scp /pro/bubjet/build/libs/bubjet-0.0.1.jar ssh.cs.brown.edu:/pro/bubbles/lib/bubjet.jar
endif

ls -l /pro/bubjet/build/libs/bubjet-0.0.1.jar

ls -l /Applications/IntelliJ*/Contents/plugins/bubbles/lib











						












