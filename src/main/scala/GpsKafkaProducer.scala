import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import sttp.client3._
import java.util.Properties
import io.github.cdimascio.dotenv.Dotenv

object GpsKafkaProducer {

  def main(args: Array[String]): Unit = {

    val dotenv = Dotenv.configure().ignoreIfMissing().load()
    val apiKey = dotenv.get("WARSAW_API_KEY")
    val topicName: String = "warsaw-transit-gps"
    val kafkaBootstrapServer: String = "localhost:9092"

    val props: Properties = new Properties()
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServer)
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
      props.put(ProducerConfig.ACKS_CONFIG, "1")

    val producer = new KafkaProducer[String, String](props)
    val backend = HttpURLConnectionBackend()

    val gpsEndpoint = uri"https://dane.um.warszawa.pl/api/action/get_ztm_lokalizacja_pojazdow"
    val vehicleTypes = List(
      (1, "Bus"),
      (2, "Tram")
    )

    println(s"Running a GPS Producer -> Kafka topic [$topicName]")

    sys.addShutdownHook {
      println(s"Closing Kafka producer: $topicName ...")
      producer.flush()
      producer.close()
    }

    while (true) {
      try {
        for ((vType, label) <- vehicleTypes) {
          val payLoad = s"""{"type": $vType}"""

          val request = basicRequest
            .post(gpsEndpoint)
            .header("Authorization", apiKey)
            .contentType("application/json")
            .body(payLoad)
            .response(asString)

          val response = request.send(backend)

          response.body match {
            case Right(jsonPayload) =>
              val record = new ProducerRecord[String, String](topicName, null, jsonPayload)
              producer.send(record)
              println(s"$label - send GPS batch to Kafka (${jsonPayload.length} bytes)")

            case Left(err) =>
              println(s"$label - error HTTP request: $err")
          }
        }
        Thread.sleep(10000)

      } catch {
        case e: Exception =>
          println(s"Exception: ${e.getMessage}")
          Thread.sleep(5000)
      }
    }
  }
}