cd ~/coinport/frontend/exchange
releaseBranch=`git branch -a | grep $1`
if [ -z "$releaseBranch" ];then
  git checkout -b $1 remotes/origin/$1
else
  git checkout $1
fi
./activator clean dist
rm -rf ~/coinport/coinport-frontend-*
cp target/universal/coinport-frontend-* ~/coinport/
cd ~/coinport/
unzip coinport-frontend-*
cd coinport-frontend-*/bin/
nohup ./coinport-frontend -Dakka.config=akka-prod.conf &
