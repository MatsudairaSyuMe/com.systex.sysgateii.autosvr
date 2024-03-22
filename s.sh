#!/bin/sh

while :
do
  tput clear
  tput cup 1 20
  uname -n
  tput cup 1 35
  date
  tput cup 2 1
  echo '======================================================================='
  tput cup 3 0
#  netstat -na |grep "192.168.30.59.12000"
#  netstat -na |grep "192.168.14.91.15000"
  netstat -na |grep "\.12000"
#  netstat -na |grep "\.15000"
  netstat -na |grep "\:15000"|grep -v "LISTEN"
  netstat -na |grep "\:4001"|grep -v "LISTEN"
  echo '-----------------------------------------------------------------------'
#  PIDfile="/biscon/sysgateii/autosvr/AUTOSVRPIDfile"
  PIDfile="${PWD}/AUTOSVRPIDfile"
  if [ -f $PIDfile ];then
    RATEPID=`cat "${PIDfile}"`
    ps -ef | grep ${RATEPID} | grep -v grep | if read OWN PID TTY TIM CMD
    then 
      if [ $PID ]
      then
         echo    "AUTOSVR service working pid  = " $PID ""
      fi
    fi 
  else
    RATEPID="^"
  fi
#  PIDfilesvr="${PWD}/AUTOSVRPIDfile.svrid"
  for n in ${PWD}/AUTOSVRPIDfile[.1-9].svrid; do
    PIDfilesvr=$n
    fn=`echo $n | awk -F \/ '{print $NF}'`
    fn=`echo $fn | sed 's/\.svrid$//'`
    fn=`echo $fn | sed 's/^AUTOSVRPIDfile//'`
    if [ -f $PIDfilesvr ];then
      RATEPIDSVR=`cat "${PIDfilesvr}"`
      ps -ef | grep ${RATEPIDSVR} | grep -v grep | if read OWN PID TTY TIM CMD
      then 
        if [ $PID ]
        then
          echo    "AUTOSVR ID: ${fn} service working pid  = " $PID ""
        fi
      fi 
    else
      RATEPIDSVR="^"
    fi
  done
  PIDfilecon="${PWD}/AUTOSVRPIDfile.conduct"
  if [ -f $PIDfilecon ];then
    RATEPIDCON=`cat "${PIDfilecon}"`
    ps -ef | grep ${RATEPIDCON} | grep -v grep | if read OWN PID TTY TIM CMD
    then 
      if [ $PID ]
      then
         echo    "AUTOSVR conduct working pid  = " $PID ""
      fi
    fi 
  else
    RATEPIDCON="^"
  fi
  echo '#######################################################################'
  sleep 5
done
