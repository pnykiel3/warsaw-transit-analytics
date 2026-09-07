import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object GpsStreamingProcessor {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("Warsaw Transit Analytics - GPS Streaming")
      .master("local[*]")
      .config("spark.sql.crossJoin.enabled", "true")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")
    import spark.implicits._

    val distanceUdf = udf((lat1: Double, lon1: Double, lat2: Double, lon2: Double) => {
      GeoUtils.haversine(lat1, lon1, lat2, lon2)
    })

    val stopsDF = spark.read.parquet("data/parquet/stops")
      .select(
        col("stop_name"),
        col("stop_lat"),
        col("stop_lon")
      )

    val vehicleSchema = new StructType()
      .add("Brigade", StringType, nullable = true)
      .add("Lines", StringType, nullable = true)
      .add("Lat", DoubleType, nullable = true)
      .add("Lon", DoubleType, nullable = true)
      .add("Time", StringType, nullable = true)
      .add("VehicleNumber", StringType, nullable = true)

    val jsonSchema = ArrayType(vehicleSchema)

    val kafkaStreamDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "warsaw-transit-gps")
      .option("startingOffsets", "latest")
      .load()

    val vehiclesDF = kafkaStreamDF
      .selectExpr("CAST(value AS STRING) as json_str")
      .select(from_json($"json_str", jsonSchema).as("data"))
      .select(explode($"data").as("vehicle"))
      .select(
        $"vehicle.Lines".as("line"),
        $"vehicle.VehicleNumber".as("vehicle_number"),
        $"vehicle.Brigade".as("brigade"),
        $"vehicle.Lat".as("lat"),
        $"vehicle.Lon".as("lon"),
        to_timestamp($"vehicle.Time", "yyyy-MM-dd HH:mm:ss").as("event_time")
      )

    val enrichedDF = vehiclesDF.join(
      broadcast(stopsDF),
      distanceUdf($"lat", $"lon", $"stop_lat", $"stop_lon") < 50.0, "left")
      .withColumn(
        "status", when($"stop_name".isNotNull, lit("AT_STOP")).otherwise(lit("IN_TRANSIT"))
      )
      .drop("stop_lat", "stop_lon")

    val query = enrichedDF.writeStream
      .outputMode("append")
      .format("console")
      .option("truncate", "false")
      .start()

    println("Spark Structured Streaming uruchomiony. Oczekiwanie na dane z Kafki...")
    query.awaitTermination()
  }
}