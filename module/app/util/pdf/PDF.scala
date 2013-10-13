package util.pdf


import java.io._
import org.w3c.tidy.Tidy
import org.xhtmlrenderer.pdf._
import org.xhtmlrenderer.resource.{ImageResource, CSSResource, XMLResource}

import play.api.{Logger, Play}
import play.api.templates.Html

import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.Image
import java.net.{URL, MalformedURLException}
import scalax.io.Resource
import play.mvc.Results


object PDF {
  val playDefaultUrl = "http://localhost:9000"

  val logger = Logger("pdf")

  def ok(html: Html, documentBaseURL: String = playDefaultUrl, tidy: Boolean = true) = {
    val input = if (tidy) tidify(html.body) else html.body
    val pdf = toBytes(input, documentBaseURL)
    Results.ok(pdf).as("application/pdf").getWrappedResult
  }

  def tidify(body: String): String = {
    logger.trace(s"Before tidy:\n$body")
    val tidy = new Tidy()
    tidy.setXHTML(true)
    val writer = new StringWriter()
    tidy.parse(new StringReader(body), writer)
    writer.getBuffer.toString
  }


  def toBytes(string: String, documentBaseURL: String) = {
    val os = new ByteArrayOutputStream()
    buildStream(string, os, documentBaseURL)
    os.toByteArray
  }

  def buildStream(string: String, os: OutputStream, documentBaseURL: String) {
    val reader: Reader = new StringReader(string)
    buildStream(reader, os, documentBaseURL)
    ()
  }

  def buildStream(reader: Reader, os: OutputStream, documentBaseURL: String) {
    try {
      val renderer = new ITextRenderer()
      addFontDirectory(renderer.getFontResolver, Play.current.path + "/conf/fonts")

      val myUserAgent = MyUserAgent(renderer.getOutputDevice)
      myUserAgent.setSharedContext(renderer.getSharedContext)
      renderer.getSharedContext.setUserAgentCallback(myUserAgent)

      val resource = XMLResource.load(reader)
      val document = resource.getDocument

      renderer.setDocument(document, documentBaseURL)
      renderer.layout()
      renderer.createPDF(os)
    } catch {
      case e: Exception =>
        Logger.error("Creating document from template", e)
    }
    ()
  }

  def addFontDirectory(fontResolver: ITextFontResolver, directory: String) {
    def addFont(file: File) = {
      fontResolver.addFont(file.getAbsolutePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
    }
    val dir = new File(directory)
    dir.listFiles.foreach(addFont)
    ()
  }
}


case class MyUserAgent(outputDevice: ITextOutputDevice) extends ITextUserAgent(outputDevice) {

  override def getImageResource(uri: String): ImageResource = {
    PDF.logger.info(s"Loading ImageResource $uri")
    Play.current.resourceAsStream(uri) match {
      case None => super.getImageResource(uri)
      case Some(stream) =>
        try {
          val bytes = Resource.fromInputStream(stream).byteArray
          val image = Image.getInstance(bytes)
          scaleToOutputResolution(image)
          new ImageResource(uri, new ITextFSImage(image))
        } catch {
          case e: Exception => {
            Logger.error(s"Fetching image $uri", e)
            throw new RuntimeException(e)
          }
        }
    }
  }

  override def getCSSResource(uri: String): CSSResource = try {
    PDF.logger.info(s"Loading CSSResource $uri")
    val path = new URL(uri).getPath // uri is in fact a complete URL
    Play.current.resourceAsStream(path) match {
      case None => super.getCSSResource(uri)
      case Some(stream) => new CSSResource(stream)
    }
  } catch {
    case e: MalformedURLException => {
      Logger.error(s"Fetching css $uri", e)
      throw new RuntimeException(e)
    }
  }

  override def getBinaryResource(uri: String): Array[Byte] = {
    PDF.logger.info(s"Loading BinaryResource $uri")
    Play.current.resourceAsStream(uri) match {
      case None => super.getBinaryResource(uri)
      case Some(stream) => Resource.fromInputStream(stream).byteArray
    }
  }

  override def getXMLResource(uri: String): XMLResource = {
    PDF.logger.info(s"Loading XmlResource $uri")
    Play.current.resourceAsStream(uri) match {
      case None => super.getXMLResource(uri)
      case Some(stream) => XMLResource.load(stream)
    }
  }

  private def scaleToOutputResolution(image: Image) {
    val factor = getSharedContext.getDotsPerPixel
    image.scaleAbsolute(image.getPlainWidth * factor, image.getPlainHeight * factor)
  }
}
