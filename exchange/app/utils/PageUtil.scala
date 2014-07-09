package utils

object PageUtil {

  def encryptEmail(emailOpt: Option[String]): String = emailOpt.map {
    email =>
    val atPos = email.indexOf("@")
    if (atPos > 3) email.substring(0, 3) + "****" + email.substring(atPos)
    else email.substring(0, atPos) + "****" + email.substring(atPos)
  } getOrElse ""

}
