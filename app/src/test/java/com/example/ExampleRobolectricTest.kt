package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DistanceUnit
import com.example.data.model.GolfCartLocation
import com.example.data.repository.CartLocationRepository
import com.example.location.LocationTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CartFinder", appName)
  }

  @Test
  fun `cart location repository saves and completely forgets spot`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = CartLocationRepository(context)

    // Save parked location
    val spot = GolfCartLocation(
        id = "test_cart_1",
        latitude = 36.5688,
        longitude = -121.9486,
        accuracyMeters = 2.5f,
        spotLabel = "Bay 12 Clubhouse"
    )
    repo.saveParkedLocation(spot)
    assertEquals(spot, repo.currentParkedLocation.value)

    // Clear parked location (completely forgets the point)
    repo.clearParkedLocation()
    assertNull(repo.currentParkedLocation.value)
  }

  @Test
  fun `distance calculation is positive between distinct coordinates`() {
    val dist = LocationTracker.calculateDistanceMeters(
        36.5688, -121.9486,
        36.5694, -121.9478
    )
    assertTrue("Distance should be greater than 0", dist > 10f)
  }
}
