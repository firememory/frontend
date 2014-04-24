package utils

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{Path, FileSystem}
import java.net.URI
import models.FileItem
import play.api.Play

object HdfsAccess {
  val basePath = Play.current.configuration.getString("hdfs.export.dir").getOrElse("/export/")
  val uri = Play.current.configuration.getString("hdfs.uri").getOrElse("hdfs://hadoop:54310")

  println("connecting to hdfs at " + uri)

  val conf = new Configuration()
  lazy val hdfs = FileSystem.get(URI.create(uri), conf)

  def getFileStream(path: String, name: String) = {
    val fullPath = basePath + path + "/" + name
    hdfs.open(new Path(fullPath)).asInstanceOf[java.io.InputStream]
  }

  def listFiles(path: String) = {
    val fullPath = basePath + path
    hdfs.listStatus(new Path(fullPath)).map {
      file =>
        FileItem(file.getPath.getName, file.getLen, file.getModificationTime, path)
    }
  }
}
