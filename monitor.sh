#!/bin/bash

function start_pop3 {
	nohup java -jar target/mail-proxy-0.1.0-SNAPSHOT-standalone.jar 110 pop3.feinno.com 995 true true &
}

function start_smtp {
	nohup java -jar target/mail-proxy-0.1.0-SNAPSHOT-standalone.jar 25 smtp.feinno.com 587 false false &
}

while true;
do

port=$( netstat -ntpl | grep :110 )
if [ "$port" = "" ]; then
	echo "no pop3 start it" >> svc.stat
	start_pop3;
	sleep 10;
fi

port=$( netstat -ntpl | grep :25 )
if [ "$port" = "" ]; then
	echo "no smtp start it" >> svc.stat
	start_smtp;
	sleep 10;
fi

sleep 5;


for i in `pgrep java`;
do 
n=`ls -l /proc/$i/task | wc -l`
if [ $n -gt 250 ]; then
	echo 'too many threads, kill....'
	kill $i
fi
done;

done

