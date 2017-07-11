
package com.mindlibrary.images;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.RemoteViews;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static com.mindlibrary.images.MindLibImage.LoadedFrom.NETWORK;
import static com.mindlibrary.images.MindLibImage.RequestTransformer.IDENTITY;
import static com.mindlibrary.images.TestUtils.URI_KEY_1;
import static com.mindlibrary.images.TestUtils.makeBitmap;
import static com.mindlibrary.images.TestUtils.mockCallback;
import static com.mindlibrary.images.TestUtils.mockImageViewTarget;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class) public class RemoteViewsActionTest {

  private MindLibImage mindLibImage;
  private RemoteViews remoteViews;

  @Before public void setUp() {
    mindLibImage = createPicasso();
    remoteViews = mock(RemoteViews.class);
    when(remoteViews.getLayoutId()).thenReturn(android.R.layout.list_content);
  }

  @Test public void completeSetsBitmapOnRemoteViews() throws Exception {
    Callback callback = mockCallback();
    Bitmap bitmap = makeBitmap();
    RemoteViewsAction action = createAction(callback);
    action.complete(bitmap, NETWORK);
    verify(remoteViews).setImageViewBitmap(1, bitmap);
    verify(callback).onSuccess();
  }

  @Test public void errorWithNoResourceIsNoop() throws Exception {
    Callback callback = mockCallback();
    RemoteViewsAction action = createAction(callback);
    Exception e = new RuntimeException();
    action.error(e);
    verifyZeroInteractions(remoteViews);
    verify(callback).onError(e);
  }

  @Test public void errorWithResourceSetsResource() throws Exception {
    Callback callback = mockCallback();
    RemoteViewsAction action = createAction(1, callback);
    Exception e = new RuntimeException();
    action.error(e);
    verify(remoteViews).setImageViewResource(1, 1);
    verify(callback).onError(e);
  }

  @Test public void clearsCallbackOnCancel() throws Exception {
    MindLibImage mindLibImage = mock(MindLibImage.class);
    ImageView target = mockImageViewTarget();
    Callback callback = mockCallback();
    ImageViewAction request =
        new ImageViewAction(mindLibImage, target, null, 0, 0, 0, null, URI_KEY_1, null, callback, false);
    request.cancel();
    assertThat(request.callback).isNull();
  }

  private TestableRemoteViewsAction createAction(Callback callback) {
    return createAction(0, callback);
  }

  private TestableRemoteViewsAction createAction(int errorResId, Callback callback) {
    return new TestableRemoteViewsAction(mindLibImage, null, remoteViews, 1, errorResId, 0, 0, null,
        URI_KEY_1, callback);
  }

  private MindLibImage createPicasso() {
    return new MindLibImage(RuntimeEnvironment.application, mock(Dispatcher.class), Cache.NONE, null,
        IDENTITY, null, mock(Stats.class), ARGB_8888, false, false);
  }

  static class TestableRemoteViewsAction extends RemoteViewsAction {
    TestableRemoteViewsAction(MindLibImage mindLibImage, Request data, RemoteViews remoteViews, int viewId,
                              int errorResId, int memoryPolicy, int networkPolicy, String tag, String key,
                              Callback callback) {
      super(mindLibImage, data, remoteViews, viewId, errorResId, memoryPolicy, networkPolicy, tag, key,
          callback);
    }

    @Override void update() {
    }
  }
}
