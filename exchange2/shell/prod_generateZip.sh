cd /var/coinport/code/frontend/exchange
git fetch
releaseBranch=`git branch | grep $1`
if [ -z "$releaseBranch" ];then
  git checkout -b $1 origin/$1
else
  git checkout $1
fi
./activator clean dist
rm -rf /var/coinport/frontend/coinport-frontend-*
cp target/universal/coinport-frontend-* /var/coinport/frontend
cd /var/coinport/frontend
unzip coinport-frontend-*
