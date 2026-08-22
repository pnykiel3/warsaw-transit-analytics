import org.apache.spark.sql.SparkSession

object GtfsBatchProcessor {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("Warsaw Transit Analytics GTFS Batch")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")
    println("Loading raw .json file...")

    val stopsDF = spark.read
      .option("header","true")
      .option("inferSchema", "true")
      .csv("data/gtfs/stops.txt")

    print("Spark schema:")
    stopsDF.printSchema()

    stopsDF.show(5, truncate = false)

    stopsDF.write
      .mode("overwrite")
      .parquet("data/parquet/stops")

    spark.stop()
  }
}