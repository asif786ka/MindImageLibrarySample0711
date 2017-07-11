
package com.mindlibrary.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static android.graphics.Bitmap.Config.RGB_565;
import static com.mindlibrary.images.RequestHandler.calculateInSampleSize;
import static com.mindlibrary.images.RequestHandler.createBitmapOptions;
import static com.mindlibrary.images.RequestHandler.requiresInSampleSize;
import static com.mindlibrary.images.TestUtils.URI_1;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricGradleTestRunner.class)
public class RequestHandlerTest {

  @Test public void bitmapConfig() throws Exception {
    for (Bitmap.Config config : Bitmap.Config.values()) {
      Request data = new Request.Builder(URI_1).config(config).build();
      Request copy = data.buildUpon().build();

      assertThat(createBitmapOptions(data).inPreferredConfig).isSameAs(config);
      assertThat(createBitmapOptions(copy).inPreferredConfig).isSameAs(config);
    }
  }

  @Test public void requiresComputeInSampleSize() {
    assertThat(requiresInSampleSize(null)).isFalse();
    final BitmapFactory.Options defaultOptions = new BitmapFactory.Options();
    assertThat(requiresInSampleSize(defaultOptions)).isFalse();
    final BitmapFactory.Options justBounds = new BitmapFactory.Options();
    justBounds.inJustDecodeBounds = true;
    assertThat(requiresInSampleSize(justBounds)).isTrue();
  }

  @Test public void calculateInSampleSizeNoResize() {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    Request data = new Request.Builder(URI_1).build();
    calculateInSampleSize(100, 100, 150, 150, options, data);
    assertThat(options.inSampleSize).isEqualTo(1);
  }

  @Test public void calculateInSampleSizeResize() {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    Request data = new Request.Builder(URI_1).build();
    calculateInSampleSize(100, 100, 200, 200, options, data);
    assertThat(options.inSampleSize).isEqualTo(2);
  }

  @Test public void calculateInSampleSizeResizeCenterInside() {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    Request data = new Request.Builder(URI_1).centerInside().resize(100, 100).build();
    calculateInSampleSize(data.targetWidth, data.targetHeight, 400, 200, options, data);
    assertThat(options.inSampleSize).isEqualTo(4);
  }

  @Test public void calculateInSampleSizeKeepAspectRatioWithWidth() {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    Request data = new Request.Builder(URI_1).resize(400, 0).build();
    calculateInSampleSize(data.targetWidth, data.targetHeight, 800, 200, options, data);
    assertThat(options.inSampleSize).isEqualTo(2);
  }

  @Test public void calculateInSampleSizeKeepAspectRatioWithHeight() {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    Request data = new Request.Builder(URI_1).resize(0, 100).build();
    calculateInSampleSize(data.targetWidth, data.targetHeight, 800, 200, options, data);
    assertThat(options.inSampleSize).isEqualTo(2);
  }

  @Test public void nullBitmapOptionsIfNoResizingOrPurgeable() {
    // No resize must return no bitmap options
    final Request noResize = new Request.Builder(URI_1).build();
    final BitmapFactory.Options noResizeOptions = createBitmapOptions(noResize);
    assertThat(noResizeOptions).isNull();
  }

  @Test public void inJustDecodeBoundsIfResizing() {
    // Resize must return bitmap options with inJustDecodeBounds = true
    final Request requiresResize = new Request.Builder(URI_1).resize(20, 15).build();
    final BitmapFactory.Options resizeOptions = createBitmapOptions(requiresResize);
    assertThat(resizeOptions).isNotNull();
    assertThat(resizeOptions.inJustDecodeBounds).isTrue();
    assertThat(resizeOptions.inPurgeable).isFalse();
    assertThat(resizeOptions.inInputShareable).isFalse();
  }

  @Test public void inPurgeableIfInPurgeable() {
    final Request request = new Request.Builder(URI_1).purgeable().build();
    final BitmapFactory.Options options = createBitmapOptions(request);
    assertThat(options).isNotNull();
    assertThat(options.inPurgeable).isTrue();
    assertThat(options.inInputShareable).isTrue();
    assertThat(options.inJustDecodeBounds).isFalse();
  }

  @Test public void createWithConfigAndNotInJustDecodeBoundsOrInPurgeable() {
    // Given a config, must return bitmap options and false inJustDecodeBounds/inPurgeable
    final Request config = new Request.Builder(URI_1).config(RGB_565).build();
    final BitmapFactory.Options configOptions = createBitmapOptions(config);
    assertThat(configOptions).isNotNull();
    assertThat(configOptions.inJustDecodeBounds).isFalse();
    assertThat(configOptions.inPurgeable).isFalse();
    assertThat(configOptions.inInputShareable).isFalse();
  }
}
