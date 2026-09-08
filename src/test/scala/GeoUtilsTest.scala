import org.scalatest.funsuite.AnyFunSuite

class GeoUtilsTest extends AnyFunSuite {

  test("haversine returns zero for identical coordinates") {
    val dist = GeoUtils.haversine(
      lat1 = 50.1234,
      lon1 = 20.2222,
      lat2 = 50.1234,
      lon2 = 20.2222
    )
    assert(math.abs(dist) == 0.0)
  }

  test("haversine returns expected distance") {
    val dist = GeoUtils.haversine(
      lat1 = 0.0,
      lon1 = 0.0,
      lat2 = 0.0,
      lon2 = 1.0
    )

    val expectedMeters = 111200.0

    assert(math.abs(dist - expectedMeters) < 10.0 )
  }

  test("haversine is symmetric") {
    val warsaw = (52.2297, 21.0122)
    val debica = (50.0515, 21.4114)

    val warsawToDebica = GeoUtils.haversine(
      warsaw._1, warsaw._2, debica._1, debica._2
    )
    val debicaToWarsaw = GeoUtils.haversine(
      debica._1, debica._2, warsaw._1, warsaw._2
    )

    assert(math.abs(warsawToDebica - debicaToWarsaw) == 0.0)
  }

  test("haversine never returns a negative distance") {
    val distance = GeoUtils.haversine(
      lat1 = 52.2297,
      lon1 = 21.0122,
      lat2 = 50.0647,
      lon2 = 19.9450
    )

    assert(distance >= 0.0)
  }
}
