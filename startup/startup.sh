#!/bin/env bash

if [ ! -f /tmp/cloud-failer.log ]; then
    touch /tmp/cloud-failer.log
fi

while :
do
    java -jar /home/fteychene/Downloads/cloud-failer-0.1.0.jar >> /tmp/cloud-failer.log &
    sleep 3
    while [ true ]; do
        curl --output /dev/null -s -m 10 --retry 5 http://localhost:8080/health
        if [[ "$?" -ne 0 ]]; then
            echo "Health check is not responding"
            break
        fi
        sleep 1
    done
    echo "Restarting"
done