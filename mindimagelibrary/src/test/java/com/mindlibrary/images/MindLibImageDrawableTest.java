
package com.mindlibrary.images;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import static android.graphics.Color.RED;
import static com.mindlibrary.images.MindLibImage.LoadedFrom.DISK;
import static com.mindlibrary.images.MindLibImage.LoadedFrom.MEMORY;
import static com.mindlibrary.images.TestUtils.makeBitmap;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricGradleTestRunner.class)
public class MindLibImageDrawableTest {
  private final Context context = RuntimeEnvironment.application;
  private final Drawable placeholder = new ColorDrawable(RED);
  private final Bitmap bitmap = makeBitmap();
  
  @Test public void createWithNoPlaceholderAnimation() {
    MindLibDrawable pd = new MindLibDrawable(context, bitmap, null, DISK, false, false);
    assertThat(pd.getBitmap()).isSameAs(bitmap);
    assertThat(pd.placeholder).isNull();
    assertThat(pd.animating).isTrue();
  }

  @Test public void createWithPlaceholderAnimation() {
    MindLibDrawable pd = new MindLibDrawable(context, bitmap, placeholder, DISK, false, false);
    assertThat(pd.getBitmap()).isSameAs(bitmap);
    assertThat(pd.placeholder).isSameAs(placeholder);
    assertThat(pd.animating).isTrue();
  }

  @Test public void createWithBitmapCacheHit() {
    MindLibDrawable pd = new MindLibDrawable(context, bitmap, placeholder, MEMORY, false, false);
    assertThat(pd.getBitmap()).isSameAs(bitmap);
    assertThat(pd.placeholder).isNull();
    assertThat(pd.animating).isFalse();
  }
}
