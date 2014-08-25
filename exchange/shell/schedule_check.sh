#!/bin/sh #
# Copyright 2014 Coinport Inc. All Rights Reserved.
# Author: xiaolu@coinport.com (Wu Xiaolu)

retry=3

check_service(){
  res=`curl https://coinport.com/api/open/reserve/BTC`
  echo "=====>>>>>>>  response is "$res
  isOk=`echo $res | grep '"success":true,"code":0' | wc -l`
  
  if [ "$isOk" -eq "1" ];then
    echo "all service is ok now"
    return 0
  else
    echo "some service is not ok"
    return 1
  fi
}

try() {
  tryMax=$1
  echo $tryMax
  tried=1
  while [ $tried -le $tryMax ]
  do
    check_service
    if [ $? -eq 0 ];then
      return 0
    else
      echo "can't fetch data from coinport"
      tried=`expr $tried + 1`
      sleep 5
    fi
  done
  return 1
}

try $retry
if [ $? -eq 1 ];then
  sh /var/coinport/code/frontend/exchange/shell/stop.sh
  isRunning=`ps -ef | grep 'coinport-frontend1' | grep -v 'grep' | wc -l`
  if [ $isRunning -eq 0 ];then
    echo "frontend has been stopped"
  fi
  sh /var/coinport/code/frontend/exchange/shell/prod_start.sh
  isRunning=`ps -ef | grep 'coinport-frontend1' | grep -v 'grep' | wc -l`
  if [ $isRunning -eq 1 ];then
    echo "frontend has been started"
    exit(0)
  fi
fi
