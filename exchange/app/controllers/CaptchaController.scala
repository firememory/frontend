package controllers

import javax.imageio.ImageIO
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID
import java.awt.Color
import java.awt.Font
import java.awt.GraphicsEnvironment

import play.api.mvc._
import play.api.libs.json._
import com.coinport.coinex.api.model._

import com.octo.captcha.service.image.DefaultManageableImageCaptchaService
import com.octo.captcha.service.image.ImageCaptchaService
import com.octo.captcha.service.captchastore.FastHashMapCaptchaStore
import com.octo.captcha.component.image.backgroundgenerator.BackgroundGenerator
import com.octo.captcha.component.image.backgroundgenerator.GradientBackgroundGenerator
import com.octo.captcha.component.image.color.SingleColorGenerator
import com.octo.captcha.component.image.fontgenerator.FontGenerator
import com.octo.captcha.component.image.fontgenerator.RandomFontGenerator
import com.octo.captcha.component.image.textpaster.DecoratedRandomTextPaster
import com.octo.captcha.component.image.textpaster.TextPaster
import com.octo.captcha.component.image.textpaster.textdecorator.BaffleTextDecorator
import com.octo.captcha.component.image.textpaster.textdecorator.LineTextDecorator
import com.octo.captcha.component.image.textpaster.textdecorator.TextDecorator
import com.octo.captcha.component.image.wordtoimage.ComposedWordToImage
import com.octo.captcha.component.image.wordtoimage.WordToImage
import com.octo.captcha.component.word.wordgenerator.RandomWordGenerator
import com.octo.captcha.component.word.wordgenerator.WordGenerator
import com.octo.captcha.engine.image.ListImageCaptchaEngine
import com.octo.captcha.image.gimpy.GimpyFactory

import com.github.tototoshi.play2.json4s.native.Json4s
import com.google.common.io.BaseEncoding

object CaptchaController extends Controller with Json4s {
  val captchaService: ImageCaptchaService = new DefaultManageableImageCaptchaService(
    new FastHashMapCaptchaStore(), new CaptchaEngineEx(), 180, 100000, 75000)

  def captcha = Action { implicit request =>
    // https://groups.google.com/forum/?fromgroups#!searchin/play-framework/2.0$20image/play-framework/5h5wh3eCiYo/1uTKQ2AQ3g4J
    // http://stackoverflow.com/questions/8305853/how-to-render-a-binary-with-play-2-0
    // http://d.hatena.ne.jp/kaiseh/20090502/1241286415
    val uuid = UUID.randomUUID().toString()
    println(s"captcha: uuid: $uuid")
    val baos = new ByteArrayOutputStream
    ImageIO.write(captchaService.getImageChallengeForID(uuid, Locale.getDefault()), "jpg", baos)
    val imageBase64 = BaseEncoding.base64.encode(baos.toByteArray())
    val imageSrc = "data:image/jpeg;base64," + imageBase64
    val captcha = Captcha(imageSrc, uuid)
    val apiResult = ApiResult(true, 0, "", Some(captcha))
    Ok(apiResult.toJson)
  }

  def validate(uuid: String, text: String): Boolean = {
    println(s"captchaController.validate, uuid: $uuid, text: $text")
    try {
      captchaService.validateResponseForID(uuid, text)
      true
    } catch {
      case e: Throwable =>
        e.printStackTrace
        false
    }
  }

  class CaptchaEngineEx extends ListImageCaptchaEngine {
    override def buildInitialFactories {
      //Set Captcha Word Length Limitation which should not over 6
      val minAcceptedWordLength = new Integer(4)
      val maxAcceptedWordLength = new Integer(5)

      //Set up Captcha Image Size: Height and Width
      val imageHeight = new Integer(50)
      val imageWidth = new Integer(115)

      //Set Captcha Font Size
      val minFontSize = new Integer(22)
      val maxFontSize = new Integer(24)

      val wordGenerator: WordGenerator = new RandomWordGenerator("abcdefghijklmnopqrstuvwxyz")

      val bgGen: BackgroundGenerator = new GradientBackgroundGenerator(
        imageWidth, imageHeight, new Color(0xE0, 0xE8, 0xF0), new Color(0xE0, 0xFF, 0xFF))

      //font is not helpful for security but it really increase difficultness for attacker
      val fontGenerator: FontGenerator = new RandomFontGenerator(minFontSize,
        maxFontSize, getProperFonts)

      // Note that our captcha color is Blue
      val scg: SingleColorGenerator = new SingleColorGenerator(Color.blue)

      //decorator is very useful pretend captcha attack. we use two line text
      //decorators.
      // LineTextDecorator lineDecorator = new LineTextDecorator(1, Color.blue);
      // LineTextDecorator line_decorator2 = new LineTextDecorator(1, Color.blue);
      // TextDecorator[] textdecorators = new TextDecorator[1];

      // textdecorators[0] = lineDecorator;
      // textdecorators[1] = line_decorator2;

      val textPaster: TextPaster = new DecoratedRandomTextPaster(
        minAcceptedWordLength, maxAcceptedWordLength, scg,
        Array[TextDecorator](new BaffleTextDecorator(new Integer(1), Color.white)))

      //ok, generate the WordToImage Object for logon service to use.
      val wordToImage: WordToImage = new ComposedWordToImage(fontGenerator, bgGen, textPaster)
      addFactory(new GimpyFactory(wordGenerator, wordToImage))
    }

    private[this] def getProperFonts: Array[Font] = {
      val properFontFamilies = Array[String]("Serif", "SansSerif", "Monospaced", "Tahoma", "Arial", "Helvetica", "Times", "Courier").map(_.toUpperCase)

      val e: GraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment
      val allFonts: Array[Font] = e.getAllFonts

      val properFontFamiliesStr = properFontFamilies.mkString(", ")
      //println(s"proper font families: $properFontFamiliesStr")

      val properFonts = allFonts.filter {
        font =>
        val family = font.getFamily.toUpperCase
        properFontFamilies.filter(s => family.contains(s)).nonEmpty
      }
      properFonts.foreach {
        font =>
        val fontName = font.getFontName
        val familyName = font.getFamily
        val fname = font.getName
        //println(s"name: $fname, font name: $fontName, family name: $familyName")
      }
      properFonts
    }
  }

}
