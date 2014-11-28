## JVM SETTING FOR FRONTENT
MaxHeapSizeM=4096  # The max heap size
MaxPermSizeM=512  # The permanent size
CMSRatio=70
InitHeapSizeRatio=4  # the max heap size / the init heap size
NewRatioA=3  # all heap space size / new heap space size
XmsSizeM=`expr $MaxHeapSizeM / $InitHeapSizeRatio`
NewSizeM=`expr $XmsSizeM / $NewRatioA`
MaxNewSizeM=`expr $MaxHeapSizeM / $NewRatioA`
NumOfFullGCBeforeCompaction=1
maillist=chunming@coinport.com,c@coinport.com,d@coinport.com

Xms="-Xms${XmsSizeM}m"  # The init heap size
Xmx="-Xmx${MaxHeapSizeM}m"

NewSize="-XX:NewSize=${NewSizeM}m"  # The init size of new heap space
MaxNewSize="-XX:MaxNewSize=${MaxNewSizeM}m"  # The max size of new heap space

PermSize="-XX:PermSize=${MaxPermSizeM}m"
MaxPermSize="-XX:MaxPermSize=${MaxPermSizeM}m"

# If full GC use CMS, this is the default new GC. Also explicit lists here
UseParNewGC="-XX:+UseParNewGC"
UseConcMarkSweepGc="-XX:+UseConcMarkSweepGC"  # Use CMS as full GC
CMSInitOccupancyFraction="-XX:CMSInitiatingOccupancyFraction=${CMSRatio}"
CMSFullGCsBeforeCompaction="-XX:CMSFullGCsBeforeCompaction=${NumOfFullGCBeforeCompaction}"

cd /var/coinport/frontend/coinport-frontend-*/bin/
nohup ./coinport-frontend -J$Xms -J$Xmx -J$NewSize -J$MaxNewSize -J$PermSize -J$MaxPermSize -J$UseParNewGC -J$UseConcMarkSweepGc -J$CMSInitOccupancyFraction -J$CMSFullGCsBeforeCompaction -Dakka.config=akka-prod.conf &
