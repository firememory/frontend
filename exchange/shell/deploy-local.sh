cd /coinport/frontend/exchange
git fetch && git rebase origin/master
./activator clean dist
cd target/universal/
unzip coinport-frontend-*
cd coinport-frontend-*/bin/
nohup ./coinport-frontend &
