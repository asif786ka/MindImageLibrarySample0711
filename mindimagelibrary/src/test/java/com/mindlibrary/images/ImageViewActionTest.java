
package com.mindlibrary.images;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.mindlibrary.images.TestUtils.makeBitmap;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class ImageViewActionTest {

  @Test(expected = AssertionError.class)
  public void throwsErrorWithNullResult() throws Exception {
    ImageViewAction action =
        new ImageViewAction(mock(MindLibImage.class), TestUtils.mockImageViewTarget(), null, 0, 0, 0,
            null, TestUtils.URI_KEY_1, null, null, false);
    action.complete(null, MindLibImage.LoadedFrom.MEMORY);
  }

  @Test
  public void returnsIfTargetIsNullOnComplete() throws Exception {
    Bitmap bitmap = TestUtils.makeBitmap();
    MindLibImage mindLibImage = mock(MindLibImage.class);
    ImageView target = TestUtils.mockImageViewTarget();
    Callback callback = TestUtils.mockCallback();
    ImageViewAction request =
        new ImageViewAction(mindLibImage, target, null, 0, 0, 0, null, TestUtils.URI_KEY_1, null,
            callback, false);
    request.target.clear();
    request.complete(bitmap, MindLibImage.LoadedFrom.MEMORY);
    verifyZeroInteractions(target);
    verifyZeroInteractions(callback);
  }

  @Test
  public void returnsIfTargetIsNullOnError() throws Exception {
    MindLibImage mindLibImage = mock(MindLibImage.class);
    ImageView target = TestUtils.mockImageViewTarget();
    Callback callback = TestUtils.mockCallback();
    ImageViewAction request =
        new ImageViewAction(mindLibImage, target, null, 0, 0, 0, null, TestUtils.URI_KEY_1, null,
            callback, false);
    request.target.clear();
    request.error(new RuntimeException());
    verifyZeroInteractions(target);
    verifyZeroInteractions(callback);
  }

  @Test
  public void invokesTargetAndCallbackSuccessIfTargetIsNotNull() throws Exception {
    Bitmap bitmap = TestUtils.makeBitmap();
    MindLibImage mindLibImage =
        new MindLibImage(RuntimeEnvironment.application, mock(Dispatcher.class), Cache.NONE, null, MindLibImage.RequestTransformer.IDENTITY,
            null, mock(Stats.class), Bitmap.Config.ARGB_8888, false, false);
    ImageView target = TestUtils.mockImageViewTarget();
    Callback callback = TestUtils.mockCallback();
    ImageViewAction request =
        new ImageViewAction(mindLibImage, target, null, 0, 0, 0, null, TestUtils.URI_KEY_1, null,
            callback, false);
    request.complete(bitmap, MindLibImage.LoadedFrom.MEMORY);
    verify(target).setImageDrawable(any(MindLibDrawable.class));
    verify(callback).onSuccess();
  }

  @Test
  public void invokesTargetAndCallbackErrorIfTargetIsNotNullWithErrorResourceId() throws Exception {
    ImageView target = TestUtils.mockImageViewTarget();
    Callback callback = TestUtils.mockCallback();
    MindLibImage mock = mock(MindLibImage.class);
    ImageViewAction request =
        new ImageViewAction(mock, target, null, 0, 0, TestUtils.RESOURCE_ID_1, null, null, null,
            callback, false);
    Exception e = new RuntimeException();
    request.error(e);
    verify(target).setImageResource(TestUtils.RESOURCE_ID_1);
    verify(callback).onError(e);
  }

  @Test
  public void invokesErrorIfTargetIsNotNullWithErrorResourceId() throws Exception {
    ImageView target = TestUtils.mockImageViewTarget();
    Callback callback = TestUtils.mockCallback();
    MindLibImage mock = mock(MindLibImage.class);
    ImageViewAction request =
        new ImageViewAction(mock, target, null, 0, 0, TestUtils.RESOURCE_ID_1, null, null, null,
            callback, false);
    Exception e = new RuntimeException();
    request.error(e);
    verify(target).setImageResource(TestUtils.RESOURCE_ID_1);
    verify(callback).onError(e);
  }

  @Test
  public void invokesErrorIfTargetIsNotNullWithErrorDrawable() throws Exception {
    Drawable errorDrawable = mock(Drawable.class);
    ImageView target = TestUtils.mockImageViewTarget();
    Callback callback = TestUtils.mockCallback();
    MindLibImage mock = mock(MindLibImage.class);
    ImageViewAction request =
        new ImageViewAction(mock, target, null, 0, 0, 0, errorDrawable, TestUtils.URI_KEY_1, null,
            callback, false);
    Exception e = new RuntimeException();
    request.error(e);
    verify(target).setImageDrawable(errorDrawable);
    verify(callback).onError(e);
  }

  @Test
  public void clearsCallbackOnCancel() throws Exception {
    MindLibImage mindLibImage = mock(MindLibImage.class);
    ImageView target = TestUtils.mockImageViewTarget();
    Callback callback = TestUtils.mockCallback();
    ImageViewAction request =
        new ImageViewAction(mindLibImage, target, null, 0, 0, 0, null, TestUtils.URI_KEY_1, null,
            callback, false);
    request.cancel();
    assertThat(request.callback).isNull();
  }

  @Test
  public void stopPlaceholderAnimationOnError() throws Exception {
    MindLibImage mindLibImage = mock(MindLibImage.class);
    AnimationDrawable placeholder = mock(AnimationDrawable.class);
    ImageView target = TestUtils.mockImageViewTarget();
    when(target.getDrawable()).thenReturn(placeholder);
    ImageViewAction request =
        new ImageViewAction(mindLibImage, target, null, 0, 0, 0, null, TestUtils.URI_KEY_1, null,
            null, false);
    request.error(new RuntimeException());
    verify(placeholder).stop();
  }
}
