package utils

/**
  *  SecurityPreference structure:
  *  consist of two character, each with value 0 or 1.
  *  0 stand for disable, 1 enable.
  *  the last character stand for Email verification, the last second
  *  character for Mobile SMS verification.
  *  for example:
  *   sms enable   email disable
  *       |            |
  *  |-----------|-----------|
  *  |     1     |     0     |
  *  |-----------|-----------|
  */
object SecurityPreferenceUtil {
  val SpLen = 2
  val DefaultSP = "01"  // enable email verification if no security preference set.

  def standardization(sp: String): String = {
    if (sp == null || sp.trim.isEmpty) DefaultSP
    else {
      val sp0 = sp.getBytes.map(c => if(c == '1') '1' else '0').mkString
      if (sp0.length < SpLen) "0" * (SpLen - sp0.length) + sp0
      else sp0.substring(sp0.length - SpLen)
    }
  }

  def getUserSecPrefer(spOpt: Option[String]): String = spOpt match {
    case Some(sp) => standardization(sp)
    case None => DefaultSP
  }

  def isEmailVerificationOn(spOpt: Option[String]): Boolean = {
    val sp = getUserSecPrefer(spOpt)
    sp.charAt(sp.length - 1) == '1'
  }

  def enableEmailVerification(spOpt: Option[String]): String = {
    val sp = getUserSecPrefer(spOpt)
    sp.substring(0, sp.length - 1) + "1"
  }

  def disableEmailVerification(spOpt: Option[String]): String = {
    val sp = getUserSecPrefer(spOpt)
    sp.substring(0, sp.length - 1) + "0"
  }

  def updateEmailVerification(spOpt: Option[String], emailSP: String): String = {
    if ("1".equals(emailSP)) enableEmailVerification(spOpt)
    else disableEmailVerification(spOpt)
  }

  def isMobileVerificationOn(spOpt: Option[String]): Boolean = {
    val sp = getUserSecPrefer(spOpt)
    sp.charAt(sp.length - 2) == '1'
  }

  def enableMobileVerification(spOpt: Option[String]): String = {
    val sp = getUserSecPrefer(spOpt)
    sp.substring(0, sp.length - 2) + "1" + sp.charAt(sp.length - 1)
  }

  def disableMobileVerification(spOpt: Option[String]): String = {
    val sp = getUserSecPrefer(spOpt)
    sp.substring(0, sp.length - 2) + "0" + sp.charAt(sp.length - 1)
  }

  def updateMobileVerification(spOpt: Option[String], mobileSP: String): String = {
    if ("1".equals(mobileSP)) enableMobileVerification(spOpt)
    else disableMobileVerification(spOpt)
  }

}
