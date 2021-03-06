
package com.mindlibrary.images;

import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.RemoteViews;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static com.mindlibrary.images.TestUtils.makeBitmap;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
public class RequestCreatorTest {

  @Mock
  MindLibImage mindLibImage;
  @Captor ArgumentCaptor<Action> actionCaptor;

  final Bitmap bitmap = TestUtils.makeBitmap();

  @Before public void shutUp() {
    initMocks(this);
    when(mindLibImage.transformRequest(any(Request.class))).thenAnswer(TestUtils.TRANSFORM_REQUEST_ANSWER);
  }

  @Test
  public void getOnMainCrashes() throws IOException {
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).get();
      fail("Calling get() on main thread should throw exception");
    } catch (IllegalStateException ignored) {
    }
  }

  @Test public void loadWithShutdownCrashes() {
    mindLibImage.shutdown = true;
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).fetch();
      fail("Should have crashed with a shutdown mindLibImage.");
    } catch (IllegalStateException ignored) {
    }
  }

  @Test public void getReturnsNullIfNullUriAndResourceId() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final Bitmap[] result = new Bitmap[1];

    new Thread(new Runnable() {
      @Override public void run() {
        try {
          result[0] = new RequestCreator(mindLibImage, null, 0).get();
        } catch (IOException e) {
          fail(e.getMessage());
        } finally {
          latch.countDown();
        }
      }
    }).start();
    latch.await();

    assertThat(result[0]).isNull();
    verifyZeroInteractions(mindLibImage);
  }

  @Test public void fetchSubmitsFetchRequest() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).fetch();
    verify(mindLibImage).submit(actionCaptor.capture());
    assertThat(actionCaptor.getValue()).isInstanceOf(FetchAction.class);
  }

  @Test public void fetchWithFitThrows() {
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).fit().fetch();
      fail("Calling fetch() with fit() should throw an exception");
    } catch (IllegalStateException ignored) {
    }
  }

  @Test public void fetchWithDefaultPriority() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).fetch();
    verify(mindLibImage).submit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getPriority()).isEqualTo(MindLibImage.Priority.LOW);
  }

  @Test public void fetchWithCustomPriority() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).priority(MindLibImage.Priority.HIGH).fetch();
    verify(mindLibImage).submit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getPriority()).isEqualTo(MindLibImage.Priority.HIGH);
  }

  @Test public void fetchWithCache() {
    when(mindLibImage.quickMemoryCacheCheck(TestUtils.URI_KEY_1)).thenReturn(bitmap);
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).memoryPolicy(MemoryPolicy.NO_CACHE).fetch();
    verify(mindLibImage, never()).enqueueAndSubmit(any(Action.class));
  }

  @Test public void fetchWithMemoryPolicyNoCache() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).memoryPolicy(MemoryPolicy.NO_CACHE).fetch();
    verify(mindLibImage, never()).quickMemoryCacheCheck(TestUtils.URI_KEY_1);
    verify(mindLibImage).submit(actionCaptor.capture());
  }

  @Test
  public void intoTargetWithNullThrows() {
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into((Target) null);
      fail("Calling into() with null Target should throw exception");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test
  public void intoTargetWithFitThrows() {
    try {
      Target target = TestUtils.mockTarget();
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).fit().into(target);
      fail("Calling into() target with fit() should throw exception");
    } catch (IllegalStateException ignored) {
    }
  }

  public void intoTargetNoPlaceholderCallsWithNull() {
    Target target = TestUtils.mockTarget();
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).noPlaceholder().into(target);
    verify(target).onPrepareLoad(null);
  }

  @Test
  public void intoTargetWithNullUriAndResourceIdSkipsAndCancels() {
    Target target = TestUtils.mockTarget();
    Drawable placeHolderDrawable = mock(Drawable.class);
    new RequestCreator(mindLibImage, null, 0).placeholder(placeHolderDrawable).into(target);
    verify(mindLibImage).cancelRequest(target);
    verify(target).onPrepareLoad(placeHolderDrawable);
    verifyNoMoreInteractions(mindLibImage);
  }

  @Test
  public void intoTargetWithQuickMemoryCacheCheckDoesNotSubmit() {
    when(mindLibImage.quickMemoryCacheCheck(TestUtils.URI_KEY_1)).thenReturn(bitmap);
    Target target = TestUtils.mockTarget();
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(target);
    verify(target).onBitmapLoaded(bitmap, MindLibImage.LoadedFrom.MEMORY);
    verify(mindLibImage).cancelRequest(target);
    verify(mindLibImage, never()).enqueueAndSubmit(any(Action.class));
  }

  @Test
  public void intoTargetWithSkipMemoryPolicy() {
    Target target = TestUtils.mockTarget();
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).memoryPolicy(MemoryPolicy.NO_CACHE).into(target);
    verify(mindLibImage, never()).quickMemoryCacheCheck(TestUtils.URI_KEY_1);
  }

  @Test
  public void intoTargetAndNotInCacheSubmitsTargetRequest() {
    Target target = TestUtils.mockTarget();
    Drawable placeHolderDrawable = mock(Drawable.class);
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).placeholder(placeHolderDrawable).into(target);
    verify(target).onPrepareLoad(placeHolderDrawable);
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue()).isInstanceOf(TargetAction.class);
  }

  @Test public void targetActionWithDefaultPriority() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(TestUtils.mockTarget());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getPriority()).isEqualTo(MindLibImage.Priority.NORMAL);
  }

  @Test public void targetActionWithCustomPriority() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).priority(MindLibImage.Priority.HIGH).into(TestUtils.mockTarget());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getPriority()).isEqualTo(MindLibImage.Priority.HIGH);
  }

  @Test public void targetActionWithDefaultTag() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(TestUtils.mockTarget());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getTag()).isEqualTo(actionCaptor.getValue());
  }

  @Test public void targetActionWithCustomTag() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).tag("tag").into(TestUtils.mockTarget());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getTag()).isEqualTo("tag");
  }

  @Test
  public void intoImageViewWithNullThrows() {
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into((ImageView) null);
      fail("Calling into() with null ImageView should throw exception");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test
  public void intoImageViewWithNullUriAndResourceIdSkipsAndCancels() {
    ImageView target = TestUtils.mockImageViewTarget();
    new RequestCreator(mindLibImage, null, 0).into(target);
    verify(mindLibImage).cancelRequest(target);
    verify(mindLibImage, never()).quickMemoryCacheCheck(anyString());
    verify(mindLibImage, never()).enqueueAndSubmit(any(Action.class));
  }

  @Test
  public void intoImageViewWithQuickMemoryCacheCheckDoesNotSubmit() {
    MindLibImage mindLibImage =
        spy(new MindLibImage(RuntimeEnvironment.application, mock(Dispatcher.class), Cache.NONE, null,
            MindLibImage.RequestTransformer.IDENTITY, null, mock(Stats.class), ARGB_8888, false, false));
    doReturn(bitmap).when(mindLibImage).quickMemoryCacheCheck(TestUtils.URI_KEY_1);
    ImageView target = TestUtils.mockImageViewTarget();
    Callback callback = TestUtils.mockCallback();
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(target, callback);
    verify(target).setImageDrawable(any(MindLibDrawable.class));
    verify(callback).onSuccess();
    verify(mindLibImage).cancelRequest(target);
    verify(mindLibImage, never()).enqueueAndSubmit(any(Action.class));
  }

  @Test
  public void intoImageViewSetsPlaceholderDrawable() {
    MindLibImage mindLibImage =
        spy(new MindLibImage(RuntimeEnvironment.application, mock(Dispatcher.class), Cache.NONE, null,
            MindLibImage.RequestTransformer.IDENTITY, null, mock(Stats.class), ARGB_8888, false, false));
    ImageView target = TestUtils.mockImageViewTarget();
    Drawable placeHolderDrawable = mock(Drawable.class);
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).placeholder(placeHolderDrawable).into(target);
    verify(target).setImageDrawable(placeHolderDrawable);
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue()).isInstanceOf(ImageViewAction.class);
  }

  @Test
  public void intoImageViewNoPlaceholderDrawable() {
    MindLibImage mindLibImage =
        spy(new MindLibImage(RuntimeEnvironment.application, mock(Dispatcher.class), Cache.NONE, null, MindLibImage.RequestTransformer.IDENTITY,
            null, mock(Stats.class), ARGB_8888, false, false));
    ImageView target = TestUtils.mockImageViewTarget();
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).noPlaceholder().into(target);
    verifyNoMoreInteractions(target);
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue()).isInstanceOf(ImageViewAction.class);
  }

  @Test
  public void intoImageViewSetsPlaceholderWithResourceId() {
    MindLibImage mindLibImage =
        spy(new MindLibImage(RuntimeEnvironment.application, mock(Dispatcher.class), Cache.NONE, null, MindLibImage.RequestTransformer.IDENTITY,
            null, mock(Stats.class), ARGB_8888, false, false));
    ImageView target = TestUtils.mockImageViewTarget();
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).placeholder(android.R.drawable.picture_frame).into(target);
    ArgumentCaptor<Drawable> drawableCaptor = ArgumentCaptor.forClass(Drawable.class);
    verify(target).setImageDrawable(drawableCaptor.capture());
    assertThat(shadowOf(drawableCaptor.getValue()).getCreatedFromResId()) //
        .isEqualTo(android.R.drawable.picture_frame);
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue()).isInstanceOf(ImageViewAction.class);
  }

  @Test
  public void cancelNotOnMainThreadCrashes() throws InterruptedException {
    doCallRealMethod().when(mindLibImage).cancelRequest(any(Target.class));
    final CountDownLatch latch = new CountDownLatch(1);
    new Thread(new Runnable() {
      @Override public void run() {
        try {
          new RequestCreator(mindLibImage, null, 0).into(TestUtils.mockTarget());
          fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException ignored) {
        } finally {
          latch.countDown();
        }
      }
    }).start();
    latch.await();
  }

  @Test
  public void intoNotOnMainThreadCrashes() throws InterruptedException {
    doCallRealMethod().when(mindLibImage).enqueueAndSubmit(any(Action.class));
    final CountDownLatch latch = new CountDownLatch(1);
    new Thread(new Runnable() {
      @Override public void run() {
        try {
          new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(TestUtils.mockImageViewTarget());
          fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException ignored) {
        } finally {
          latch.countDown();
        }
      }
    }).start();
    latch.await();
  }

  @Test
  public void intoImageViewAndNotInCacheSubmitsImageViewRequest() {
    ImageView target = TestUtils.mockImageViewTarget();
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(target);
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue()).isInstanceOf(ImageViewAction.class);
  }

  @Test
  public void intoImageViewWithFitAndNoDimensionsQueuesDeferredImageViewRequest() {
    ImageView target = TestUtils.mockFitImageViewTarget(true);
    when(target.getWidth()).thenReturn(0);
    when(target.getHeight()).thenReturn(0);
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).fit().into(target);
    verify(mindLibImage, never()).enqueueAndSubmit(any(Action.class));
    verify(mindLibImage).defer(eq(target), any(DeferredRequestCreator.class));
  }

  @Test
  public void intoImageViewWithFitAndRequestedLayoutQueuesDeferredImageViewRequest() {
    ImageView target = TestUtils.mockFitImageViewTarget(true);
    when(target.getWidth()).thenReturn(100);
    when(target.getHeight()).thenReturn(100);
    when(target.isLayoutRequested()).thenReturn(true);
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).fit().into(target);
    verify(mindLibImage, never()).enqueueAndSubmit(any(Action.class));
    verify(mindLibImage).defer(eq(target), any(DeferredRequestCreator.class));
  }

  @Test
  public void intoImageViewWithFitAndDimensionsQueuesImageViewRequest() {
    ImageView target = TestUtils.mockFitImageViewTarget(true);
    when(target.getWidth()).thenReturn(100);
    when(target.getHeight()).thenReturn(100);
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).fit().into(target);
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue()).isInstanceOf(ImageViewAction.class);
  }

  @Test
  public void intoImageViewWithSkipMemoryCachePolicy() {
    ImageView target = TestUtils.mockImageViewTarget();
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).memoryPolicy(MemoryPolicy.NO_CACHE).into(target);
    verify(mindLibImage, never()).quickMemoryCacheCheck(TestUtils.URI_KEY_1);
  }

  @Test
  public void intoImageViewWithFitAndResizeThrows() {
    try {
      ImageView target = TestUtils.mockImageViewTarget();
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).fit().resize(10, 10).into(target);
      fail("Calling into() ImageView with fit() and resize() should throw exception");
    } catch (IllegalStateException ignored) {
    }
  }

  @Test public void imageViewActionWithDefaultPriority() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(TestUtils.mockImageViewTarget());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getPriority()).isEqualTo(MindLibImage.Priority.NORMAL);
  }

  @Test public void imageViewActionWithCustomPriority() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).priority(MindLibImage.Priority.HIGH).into(TestUtils.mockImageViewTarget());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getPriority()).isEqualTo(MindLibImage.Priority.HIGH);
  }

  @Test public void imageViewActionWithDefaultTag() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(TestUtils.mockImageViewTarget());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getTag()).isEqualTo(actionCaptor.getValue());
  }

  @Test public void imageViewActionWithCustomTag() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).tag("tag").into(TestUtils.mockImageViewTarget());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getTag()).isEqualTo("tag");
  }

  @Test public void intoRemoteViewsWidgetQueuesAppWidgetAction() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(TestUtils.mockRemoteViews(), 0, new int[] { 1, 2, 3 });
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue()).isInstanceOf(RemoteViewsAction.AppWidgetAction.class);
  }

  @Test public void intoRemoteViewsNotificationQueuesNotificationAction() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(TestUtils.mockRemoteViews(), 0, 0, TestUtils.mockNotification());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue()).isInstanceOf(RemoteViewsAction.NotificationAction.class);
  }

  @Test
  public void intoRemoteViewsNotificationWithNullRemoteViewsThrows() {
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(null, 0, 0, TestUtils.mockNotification());
      fail("Calling into() with null RemoteViews should throw exception");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test
  public void intoRemoteViewsWidgetWithPlaceholderDrawableThrows() {
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).placeholder(new ColorDrawable(0))
          .into(TestUtils.mockRemoteViews(), 0, new int[] { 1, 2, 3 });
      fail("Calling into() with placeholder drawable should throw exception");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test
  public void intoRemoteViewsWidgetWithErrorDrawableThrows() {
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).error(new ColorDrawable(0))
          .into(TestUtils.mockRemoteViews(), 0, new int[] { 1, 2, 3 });
      fail("Calling into() with error drawable should throw exception");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test
  public void intoRemoteViewsNotificationWithPlaceholderDrawableThrows() {
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).placeholder(new ColorDrawable(0))
          .into(TestUtils.mockRemoteViews(), 0, 0, TestUtils.mockNotification());
      fail("Calling into() with error drawable should throw exception");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test
  public void intoRemoteViewsNotificationWithErrorDrawableThrows() {
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).error(new ColorDrawable(0))
          .into(TestUtils.mockRemoteViews(), 0, 0, TestUtils.mockNotification());
      fail("Calling into() with error drawable should throw exception");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test
  public void intoRemoteViewsWidgetWithNullRemoteViewsThrows() {
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(null, 0, new int[] { 1, 2, 3 });
      fail("Calling into() with null RemoteViews should throw exception");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test
  public void intoRemoteViewsWidgetWithNullAppWidgetIdsThrows() {
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(TestUtils.mockRemoteViews(), 0, null);
      fail("Calling into() with null appWidgetIds should throw exception");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test
  public void intoRemoteViewsNotificationWithNullNotificationThrows() {
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(TestUtils.mockRemoteViews(), 0, 0, null);
      fail("Calling into() with null Notification should throw exception");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test
  public void intoRemoteViewsWidgetWithFitThrows() {
    try {
      RemoteViews remoteViews = TestUtils.mockRemoteViews();
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).fit().into(remoteViews, 1, new int[] { 1, 2, 3 });
      fail("Calling fit() into remote views should throw exception");
    } catch (IllegalStateException ignored) {
    }
  }

  @Test
  public void intoRemoteViewsNotificationWithFitThrows() {
    try {
      RemoteViews remoteViews = TestUtils.mockRemoteViews();
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).fit().into(remoteViews, 1, 1, TestUtils.mockNotification());
      fail("Calling fit() into remote views should throw exception");
    } catch (IllegalStateException ignored) {
    }
  }

  @Test
  public void intoTargetNoResizeWithCenterInsideOrCenterCropThrows() {
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).centerInside().into(TestUtils.mockTarget());
      fail("Center inside with unknown width should throw exception.");
    } catch (IllegalStateException ignored) {
    }
    try {
      new RequestCreator(mindLibImage, TestUtils.URI_1, 0).centerCrop().into(TestUtils.mockTarget());
      fail("Center inside with unknown height should throw exception.");
    } catch (IllegalStateException ignored) {
    }
  }

  @Test public void appWidgetActionWithDefaultPriority() throws Exception {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(TestUtils.mockRemoteViews(), 0, new int[] { 1, 2, 3 });
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getPriority()).isEqualTo(MindLibImage.Priority.NORMAL);
  }

  @Test public void appWidgetActionWithCustomPriority() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).priority(MindLibImage.Priority.HIGH)
        .into(TestUtils.mockRemoteViews(), 0, new int[]{1, 2, 3});
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getPriority()).isEqualTo(MindLibImage.Priority.HIGH);
  }

  @Test public void notificationActionWithDefaultPriority() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(TestUtils.mockRemoteViews(), 0, 0, TestUtils.mockNotification());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getPriority()).isEqualTo(MindLibImage.Priority.NORMAL);
  }

  @Test public void notificationActionWithCustomPriority() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).priority(MindLibImage.Priority.HIGH)
        .into(TestUtils.mockRemoteViews(), 0, 0, TestUtils.mockNotification());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getPriority()).isEqualTo(MindLibImage.Priority.HIGH);
  }

  @Test public void appWidgetActionWithDefaultTag() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(TestUtils.mockRemoteViews(), 0, new int[] { 1, 2, 3 });
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getTag()).isEqualTo(actionCaptor.getValue());
  }

  @Test public void appWidgetActionWithCustomTag() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).tag("tag")
        .into(TestUtils.mockRemoteViews(), 0, new int[] { 1, 2, 3 });
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getTag()).isEqualTo("tag");
  }

  @Test public void notificationActionWithDefaultTag() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(TestUtils.mockRemoteViews(), 0, 0, TestUtils.mockNotification());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getTag()).isEqualTo(actionCaptor.getValue());
  }

  @Test public void notificationActionWithCustomTag() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).tag("tag")
        .into(TestUtils.mockRemoteViews(), 0, 0, TestUtils.mockNotification());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getTag()).isEqualTo("tag");
  }

  @Test public void nullMemoryPolicy() {
    try {
      new RequestCreator().memoryPolicy(null);
      fail("Null memory policy should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test public void nullAdditionalMemoryPolicy() {
    try {
      new RequestCreator().memoryPolicy(MemoryPolicy.NO_CACHE, (MemoryPolicy[]) null);
      fail("Null additional memory policy should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test public void nullMemoryPolicyAssholeStyle() {
    try {
      new RequestCreator().memoryPolicy(MemoryPolicy.NO_CACHE, new MemoryPolicy[] { null });
      fail("Null additional memory policy should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test public void nullNetworkPolicy() {
    try {
      new RequestCreator().networkPolicy(null);
      fail("Null network policy should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test public void nullAdditionalNetworkPolicy() {
    try {
      new RequestCreator().networkPolicy(NetworkPolicy.NO_CACHE, (NetworkPolicy[]) null);
      fail("Null additional network policy should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test public void nullNetworkPolicyAssholeStyle() {
    try {
      new RequestCreator().networkPolicy(NetworkPolicy.NO_CACHE, new NetworkPolicy[] { null });
      fail("Null additional network policy should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test public void invalidResize() {
    try {
      new RequestCreator().resize(-1, 10);
      fail("Negative width should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
    try {
      new RequestCreator().resize(10, -1);
      fail("Negative height should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
    try {
      new RequestCreator().resize(0, 0);
      fail("Zero dimensions should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test public void invalidCenterCrop() {
    try {
      new RequestCreator().resize(10, 10).centerInside().centerCrop();
      fail("Calling center crop after center inside should throw exception.");
    } catch (IllegalStateException ignored) {
    }
  }

  @Test public void invalidCenterInside() {
    try {
      new RequestCreator().resize(10, 10).centerInside().centerCrop();
      fail("Calling center inside after center crop should throw exception.");
    } catch (IllegalStateException ignored) {
    }
  }

  @Test public void invalidPlaceholderImage() {
    try {
      new RequestCreator().placeholder(0);
      fail("Resource ID of zero should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
    try {
      new RequestCreator().placeholder(1).placeholder(new ColorDrawable(0));
      fail("Two placeholders should throw exception.");
    } catch (IllegalStateException ignored) {
    }
    try {
      new RequestCreator().placeholder(new ColorDrawable(0)).placeholder(1);
      fail("Two placeholders should throw exception.");
    } catch (IllegalStateException ignored) {
    }
  }

  @Test public void invalidNoPlaceholder() {
    try {
      new RequestCreator().noPlaceholder().placeholder(new ColorDrawable(0));
      fail("Placeholder after no placeholder should throw exception.");
    } catch (IllegalStateException ignored) {
    }
    try {
      new RequestCreator().noPlaceholder().placeholder(1);
      fail("Placeholder after no placeholder should throw exception.");
    } catch (IllegalStateException ignored) {
    }
    try {
      new RequestCreator().placeholder(1).noPlaceholder();
      fail("No placeholder after placeholder should throw exception.");
    } catch (IllegalStateException ignored) {
    }
    try {
      new RequestCreator().placeholder(new ColorDrawable(0)).noPlaceholder();
      fail("No placeholder after placeholder should throw exception.");
    } catch (IllegalStateException ignored) {
    }
  }

  @Test public void invalidErrorImage() {
    try {
      new RequestCreator().error(0);
      fail("Resource ID of zero should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
    try {
      new RequestCreator().error(null);
      fail("Null drawable should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
    try {
      new RequestCreator().error(1).error(new ColorDrawable(0));
      fail("Two placeholders should throw exception.");
    } catch (IllegalStateException ignored) {
    }
    try {
      new RequestCreator().error(new ColorDrawable(0)).error(1);
      fail("Two placeholders should throw exception.");
    } catch (IllegalStateException ignored) {
    }
  }

  @Test public void invalidPriority() {
    try {
      new RequestCreator().priority(null);
      fail("Null priority should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
    try {
      new RequestCreator().priority(MindLibImage.Priority.LOW).priority(MindLibImage.Priority.HIGH);
      fail("Two priorities should throw exception.");
    } catch (IllegalStateException ignored) {
    }
  }


  @Test public void invalidTag() {
    try {
      new RequestCreator().tag(null);
      fail("Null tag should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
    try {
      new RequestCreator().tag("tag1").tag("tag2");
      fail("Two tags should throw exception.");
    } catch (IllegalStateException ignored) {
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullTransformationsInvalid() {
    new RequestCreator().transform((Transformation) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullTransformationListInvalid() {
    new RequestCreator().transform((List<Transformation>) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullKeyTransformationInvalid() {
    new RequestCreator().transform(new Transformation() {
      @Override public Bitmap transform(Bitmap source) {
        return source;
      }

      @Override public String key() {
        return null;
      }
    });
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullKeyInTransformationListInvalid() {
    List<? extends Transformation> transformations =
        Collections.singletonList(new Transformation() {
          @Override public Bitmap transform(Bitmap source) {
            return source;
          }

          @Override public String key() {
            return null;
          }
        });
    new RequestCreator().transform(transformations);
  }

  @Test public void transformationListImplementationValid() {
    List<TestTransformation> transformations =
        Collections.singletonList(new TestTransformation("test"));
    new RequestCreator().transform(transformations);
    // TODO verify something!
  }

  @Test public void nullTargetsInvalid() {
    try {
      new RequestCreator().into((ImageView) null);
      fail("Null ImageView should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
    try {
      new RequestCreator().into((Target) null);
      fail("Null Target should throw exception.");
    } catch (IllegalArgumentException ignored) {
    }
  }

  @Test public void imageViewActionWithStableKey() throws Exception {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).stableKey(TestUtils.STABLE_1).into(TestUtils.mockImageViewTarget());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getKey()).isEqualTo(TestUtils.STABLE_URI_KEY_1);
  }

  @Test public void imageViewActionWithStableKeyNull() throws Exception {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).stableKey(null).into(TestUtils.mockImageViewTarget());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getKey()).isEqualTo(TestUtils.URI_KEY_1);
  }

  @Test public void notPurgeable() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).into(TestUtils.mockImageViewTarget());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getRequest().purgeable).isFalse();
  }

  @Test public void purgeable() {
    new RequestCreator(mindLibImage, TestUtils.URI_1, 0).purgeable().into(TestUtils.mockImageViewTarget());
    verify(mindLibImage).enqueueAndSubmit(actionCaptor.capture());
    assertThat(actionCaptor.getValue().getRequest().purgeable).isTrue();
  }
}
