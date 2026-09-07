object GeoUtils {

  def haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double = {

    val R: Double = 6371000.0
    val dLat: Double = lat2.toRadians - lat1.toRadians
    val dLon: Double = lon2.toRadians - lon1.toRadians
    val sinDLat2: Double = Math.sin(dLat / 2)
    val sinDLon2: Double = Math.sin(dLon / 2)

    val a: Double = sinDLat2 * sinDLat2 +
      Math.cos(lat1.toRadians) * Math.cos(lat2.toRadians) * sinDLon2 * sinDLon2

    2.0 * R * Math.asin(Math.sqrt(a))
  }
}