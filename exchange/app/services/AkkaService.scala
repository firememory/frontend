package services

import akka.util.Timeout
import scala.concurrent.duration._

trait AkkaService {
  implicit val timeout = Timeout(2 seconds)
}
