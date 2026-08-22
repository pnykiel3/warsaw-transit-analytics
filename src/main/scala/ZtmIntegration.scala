import sttp.client3._
import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets
import io.github.cdimascio.dotenv.Dotenv

object ZtmIntegration {

  def downloadFromEndpoint(endpointUrl: String, apiKey: String, outputPath: String): Unit = {
    val request = basicRequest
      .post(uri"$endpointUrl")
      .header("Authorization", apiKey)
      .response(asString)

    val backend = HttpURLConnectionBackend()
    println("Sending API request")
    val response = request.send(backend)

    response.body match {
      case Right(data) =>
        val path = Paths.get(outputPath)
        if (Files.notExists(path.getParent)) Files.createDirectories(path.getParent)
        Files.write(path, data.getBytes(StandardCharsets.UTF_8))
        println(s"Success, the data are saved in: ${path.toAbsolutePath}")

      case Left(error) =>
        println(s"Error while downloading or parsing: $error")
    }
  }

  def main(args : Array[String]) : Unit = {

    val dotenv = Dotenv.configure().ignoreIfMissing().load()
    val apiKey = dotenv.get("WARSAW_API_KEY")
    val stopsUrl: String = "https://dane.um.warszawa.pl/api/action/get_ztm_przystanki_komunikacji_miejskiej"
    val schedulesUrl: String = "https://dane.um.warszawa.pl/api/action/get_ztm_odjazdy_linii_z_przystanku"
    val dataPath : String = "data"

    println("Downloading stops data...")
    downloadFromEndpoint(stopsUrl, apiKey, dataPath + "/stops_raw.json")

    println("Downloading schedule data...")
    downloadFromEndpoint(schedulesUrl, apiKey, dataPath + "/schedule_raw.json")
  }
}