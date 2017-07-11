
package com.mindlibrary.images;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static com.mindlibrary.images.MindLibImage.LoadedFrom.MEMORY;
import static com.mindlibrary.images.MindLibImage.RequestTransformer.IDENTITY;
import static com.mindlibrary.images.TestUtils.RESOURCE_ID_1;
import static com.mindlibrary.images.TestUtils.URI_KEY_1;
import static com.mindlibrary.images.TestUtils.makeBitmap;
import static com.mindlibrary.images.TestUtils.mockTarget;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class TargetActionTest {

  @Test(expected = AssertionError.class)
  public void throwsErrorWithNullResult() throws Exception {
    TargetAction request =
        new TargetAction(mock(MindLibImage.class), mockTarget(), null, 0, 0, null, URI_KEY_1, null, 0);
    request.complete(null, MEMORY);
  }

  @Test
  public void invokesSuccessIfTargetIsNotNull() throws Exception {
    Bitmap bitmap = makeBitmap();
    Target target = mockTarget();
    TargetAction request =
        new TargetAction(mock(MindLibImage.class), target, null, 0, 0, null, URI_KEY_1, null, 0);
    request.complete(bitmap, MEMORY);
    verify(target).onBitmapLoaded(bitmap, MEMORY);
  }

  @Test
  public void invokesOnBitmapFailedIfTargetIsNotNullWithErrorDrawable() throws Exception {
    Drawable errorDrawable = mock(Drawable.class);
    Target target = mockTarget();
    TargetAction request =
        new TargetAction(mock(MindLibImage.class), target, null, 0, 0, errorDrawable, URI_KEY_1, null,
            0);
    Exception e = new RuntimeException();
    request.error(e);
    verify(target).onBitmapFailed(e, errorDrawable);
  }

  @Test
  public void invokesOnBitmapFailedIfTargetIsNotNullWithErrorResourceId() throws Exception {
    Drawable errorDrawable = mock(Drawable.class);
    Target target = mockTarget();
    Context context = mock(Context.class);
    MindLibImage mindLibImage =
        new MindLibImage(context, mock(Dispatcher.class), Cache.NONE, null, IDENTITY, null,
            mock(Stats.class), ARGB_8888, false, false);
    Resources res = mock(Resources.class);
    TargetAction request =
        new TargetAction(mindLibImage, target, null, 0, 0, null, URI_KEY_1, null, RESOURCE_ID_1);

    when(context.getResources()).thenReturn(res);
    when(res.getDrawable(RESOURCE_ID_1)).thenReturn(errorDrawable);
    Exception e = new RuntimeException();
    request.error(e);
    verify(target).onBitmapFailed(e, errorDrawable);
  }

  @Test public void recyclingInSuccessThrowsException() {
    Target bad = new Target() {
      @Override public void onBitmapLoaded(Bitmap bitmap, MindLibImage.LoadedFrom from) {
        bitmap.recycle();
      }

      @Override public void onBitmapFailed(Exception e, Drawable errorDrawable) {
        throw new AssertionError();
      }

      @Override public void onPrepareLoad(Drawable placeHolderDrawable) {
        throw new AssertionError();
      }
    };
    MindLibImage mindLibImage = mock(MindLibImage.class);

    Bitmap bitmap = makeBitmap();
    TargetAction tr = new TargetAction(mindLibImage, bad, null, 0, 0, null, URI_KEY_1, null, 0);
    try {
      tr.complete(bitmap, MEMORY);
      fail();
    } catch (IllegalStateException ignored) {
    }
  }
}
