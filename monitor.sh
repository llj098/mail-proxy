#!/bin/bash

function start_pop3 {
	nohup lein run -m mail-proxy.core 110 995 pop3.feinno.com true &
}

function start_smtp {
	nohup lein run -m mail-proxy.core 25 587 smtp.feinno.com false &
}

while true;
do

port=$( netstat -ntpl | grep :110 )
if [ "$port" = "" ]; then
	echo "no pop3 start it" >> svc.stat
	start_pop3;
fi

port=$( netstat -ntpl | grep :25 )
if [ "$port" = "" ]; then
	echo "no smtp start it" >> svc.stat
	start_smtp;
fi

sleep 5;
done

