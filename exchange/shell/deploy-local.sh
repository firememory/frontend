cd /coinport/frontend/exchange
git fetch && git rebase origin/master
nohup ./activator clean run &
