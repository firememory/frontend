ps -ef | grep 'coinport-frontend' | grep -v 'grep' | awk '{print $2}' | xargs kill
