package utils

object PageUtil {

  def encryptEmail(emailOpt: Option[String]): String = emailOpt.map {
    email =>
    val atPos = email.indexOf("@")
    if (atPos > 3) email.substring(0, 3) + "****" + email.substring(atPos)
    else email.substring(0, atPos) + "****" + email.substring(atPos)
  } getOrElse ""

  def encryptMobile(mobileOpt: Option[String]): String = mobileOpt.map {
    mobile =>
    val spacePos = mobile.indexOf(" ")
    val m = mobile.substring(spacePos + 1)
    val l = m.length
    if (l > 7) m.substring(0, 3) + "*" * (l - 7) + m.substring(l - 4)
    else m
  } getOrElse ""

}
