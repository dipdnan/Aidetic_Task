import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{avg, lit, to_timestamp, udf,concat}

object Task1 {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("SparkWebUI")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // 1. Load the dataset into a PySpark DataFrame.
    val df = spark.read
      .option("header", true)
      .csv("C:/Users/Ganesh/Desktop/SRC_Files/earthquakes_distribution.csv")

    // 2. Convert the Date and Time columns into a timestamp column named Timestamp.
    val dfwithtimestamp = df.withColumn("Timestamp",
      to_timestamp(concat($"Date", lit(" "), $"Time"), "yyyy-MM-dd HH:mm:ss"))

    // 3. Filter the dataset to include only earthquakes with a magnitude greater than 5.0.
    val filterdf = dfwithtimestamp.filter($"Magnitude" > 5.0)

    // 4. Calculate the average depth and magnitude of earthquakes for each earthquake type.
    val avgDepthAndMagnitudedf = filterdf.groupBy("Type")
      .agg(avg("Depth").alias("AvgDepth"),
        avg("Magnitude").alias("AvgMagnitude"))

    // 5. Implement a UDF to categorize the earthquakes into levels (e.g., Low, Moderate, High) based on their magnitudes.
    val categorisedMagnitudeLevel = udf((Magnitude: Double) => {
      if (Magnitude < 6.0) "Low"
      else if (Magnitude < 7.0) "Moderate"
      else "High"
    })

    val categorisedDF = filterdf.withColumn("MagnitudeLevel", categorisedMagnitudeLevel($"Magnitude"))

    // 6. Calculate the distance of each earthquake from a reference location (e.g., (0, 0)).
    val distanceDfReference = udf((lat: Double, lon: Double) => {
      val referencelat = 0.0
      val referencelon = 0.0
      math.sqrt(math.pow(lat - referencelat, 2) + math.pow(lon - referencelon, 2))
    })

    val distancedf = categorisedDF.withColumn("DistancefromReference", distanceDfReference($"Latitude", $"Longitude"))

    // 7. Write the result to a CSV file.
    distancedf.write
      .format("csv")
      .option("header", true)
      .csv("C:/Users/Ganesh/Desktop/SRC_Files/earthquakes_output")

    spark.stop()
  }
}
