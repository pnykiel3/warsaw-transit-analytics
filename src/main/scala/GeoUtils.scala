import net.sf.geographiclib.Geodesic

object GeoUtils {

  // returns distance between points in meters
  def geodesicDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double = {
    val geod = Geodesic.WGS84
    geod.Inverse(lat1, lon1, lat2, lon2).s12
  }
}