
package com.mindlibrary.images;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.RemoteViews;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static com.mindlibrary.images.MindLibImage.Listener;
import static com.mindlibrary.images.MindLibImage.LoadedFrom.MEMORY;
import static com.mindlibrary.images.TestUtils.URI_1;
import static com.mindlibrary.images.TestUtils.URI_KEY_1;
import static com.mindlibrary.images.TestUtils.makeBitmap;
import static com.mindlibrary.images.TestUtils.mockAction;
import static com.mindlibrary.images.TestUtils.mockCanceledAction;
import static com.mindlibrary.images.TestUtils.mockDeferredRequestCreator;
import static com.mindlibrary.images.TestUtils.mockHunter;
import static com.mindlibrary.images.TestUtils.mockImageViewTarget;
import static com.mindlibrary.images.TestUtils.mockTarget;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricGradleTestRunner.class)
@Config(sdk = 23) // Works around https://github.com/robolectric/robolectric/issues/2566.
public class MindLibImageTest {

  @Mock Context context;
  @Mock Downloader downloader;
  @Mock Dispatcher dispatcher;
  @Mock MindLibImage.RequestTransformer transformer;
  @Mock RequestHandler requestHandler;
  @Mock Cache cache;
  @Mock Listener listener;
  @Mock Stats stats;

  private MindLibImage mindLibImage;
  final Bitmap bitmap = makeBitmap();

  @Before public void setUp() {
    initMocks(this);
    mindLibImage = new MindLibImage(context, dispatcher, cache, listener, transformer, null, stats, ARGB_8888,
        false, false);
  }

  @Test public void submitWithNullTargetInvokesDispatcher() {
    Action action = mockAction(URI_KEY_1, URI_1);
    mindLibImage.enqueueAndSubmit(action);
    assertThat(mindLibImage.targetToAction).isEmpty();
    verify(dispatcher).dispatchSubmit(action);
  }

  @Test public void submitWithTargetInvokesDispatcher() {
    Action action = mockAction(URI_KEY_1, URI_1, mockImageViewTarget());
    assertThat(mindLibImage.targetToAction).isEmpty();
    mindLibImage.enqueueAndSubmit(action);
    assertThat(mindLibImage.targetToAction).hasSize(1);
    verify(dispatcher).dispatchSubmit(action);
  }

  @Test public void submitWithSameActionDoesNotCancel() {
    Action action = mockAction(URI_KEY_1, URI_1, mockImageViewTarget());
    mindLibImage.enqueueAndSubmit(action);
    verify(dispatcher).dispatchSubmit(action);
    assertThat(mindLibImage.targetToAction).hasSize(1).containsValue(action);
    mindLibImage.enqueueAndSubmit(action);
    verify(action, never()).cancel();
    verify(dispatcher, never()).dispatchCancel(action);
  }

  @Test public void quickMemoryCheckReturnsBitmapIfInCache() {
    when(cache.get(URI_KEY_1)).thenReturn(bitmap);
    Bitmap cached = mindLibImage.quickMemoryCacheCheck(URI_KEY_1);
    assertThat(cached).isEqualTo(bitmap);
    verify(stats).dispatchCacheHit();
  }

  @Test public void quickMemoryCheckReturnsNullIfNotInCache() {
    Bitmap cached = mindLibImage.quickMemoryCacheCheck(URI_KEY_1);
    assertThat(cached).isNull();
    verify(stats).dispatchCacheMiss();
  }

  @Test public void completeInvokesSuccessOnAllSuccessfulRequests() {
    Action action1 = mockAction(URI_KEY_1, URI_1, mockImageViewTarget());
    Action action2 = mockCanceledAction();
    BitmapHunter hunter = mockHunter(URI_KEY_1, bitmap, false);
    when(hunter.getActions()).thenReturn(Arrays.asList(action1, action2));
    when(hunter.getLoadedFrom()).thenReturn(MEMORY);
    mindLibImage.complete(hunter);
    verify(action1).complete(bitmap, MEMORY);
    verify(action2, never()).complete(eq(bitmap), any(MindLibImage.LoadedFrom.class));
  }

  @Test public void completeInvokesErrorOnAllFailedRequests() {
    Action action1 = mockAction(URI_KEY_1, URI_1, mockImageViewTarget());
    Action action2 = mockCanceledAction();
    Exception exception = mock(Exception.class);
    BitmapHunter hunter = mockHunter(URI_KEY_1, null, false);
    when(hunter.getException()).thenReturn(exception);
    when(hunter.getActions()).thenReturn(Arrays.asList(action1, action2));
    mindLibImage.complete(hunter);
    verify(action1).error(exception);
    verify(action2, never()).error(exception);
    verify(listener).onImageLoadFailed(mindLibImage, URI_1, exception);
  }

  @Test public void completeDeliversToSingle() {
    Action action = mockAction(URI_KEY_1, URI_1, mockImageViewTarget());
    BitmapHunter hunter = mockHunter(URI_KEY_1, bitmap, false);
    when(hunter.getLoadedFrom()).thenReturn(MEMORY);
    when(hunter.getAction()).thenReturn(action);
    when(hunter.getActions()).thenReturn(Collections.<Action>emptyList());
    mindLibImage.complete(hunter);
    verify(action).complete(bitmap, MEMORY);
  }

  @Test public void completeWithReplayDoesNotRemove() {
    Action action = mockAction(URI_KEY_1, URI_1, mockImageViewTarget());
    when(action.willReplay()).thenReturn(true);
    BitmapHunter hunter = mockHunter(URI_KEY_1, bitmap, false);
    when(hunter.getLoadedFrom()).thenReturn(MEMORY);
    when(hunter.getAction()).thenReturn(action);
    mindLibImage.enqueueAndSubmit(action);
    assertThat(mindLibImage.targetToAction).hasSize(1);
    mindLibImage.complete(hunter);
    assertThat(mindLibImage.targetToAction).hasSize(1);
    verify(action).complete(bitmap, MEMORY);
  }

  @Test public void completeDeliversToSingleAndMultiple() {
    Action action = mockAction(URI_KEY_1, URI_1, mockImageViewTarget());
    Action action2 = mockAction(URI_KEY_1, URI_1, mockImageViewTarget());
    BitmapHunter hunter = mockHunter(URI_KEY_1, bitmap, false);
    when(hunter.getLoadedFrom()).thenReturn(MEMORY);
    when(hunter.getAction()).thenReturn(action);
    when(hunter.getActions()).thenReturn(Arrays.asList(action2));
    mindLibImage.complete(hunter);
    verify(action).complete(bitmap, MEMORY);
    verify(action2).complete(bitmap, MEMORY);
  }

  @Test public void completeSkipsIfNoActions() {
    BitmapHunter hunter = mockHunter(URI_KEY_1, bitmap, false);
    mindLibImage.complete(hunter);
    verify(hunter).getAction();
    verify(hunter).getActions();
    verifyNoMoreInteractions(hunter);
  }

  @Test public void loadedFromIsNullThrows() {
    Action action = mockAction(URI_KEY_1, URI_1, mockImageViewTarget());
    BitmapHunter hunter = mockHunter(URI_KEY_1, bitmap, false);
    when(hunter.getAction()).thenReturn(action);
    try {
      mindLibImage.complete(hunter);
      fail("Calling complete() with null LoadedFrom should throw");
    } catch (AssertionError expected) {
    }
  }

  @Test public void resumeActionTriggersSubmitOnPausedAction() {
    Action action = mockAction(URI_KEY_1, URI_1);
    mindLibImage.resumeAction(action);
    verify(dispatcher).dispatchSubmit(action);
  }

  @Test public void resumeActionImmediatelyCompletesCachedRequest() {
    when(cache.get(URI_KEY_1)).thenReturn(bitmap);
    Action action = mockAction(URI_KEY_1, URI_1);
    mindLibImage.resumeAction(action);
    verify(action).complete(bitmap, MEMORY);
  }

  @Test public void cancelExistingRequestWithUnknownTarget() {
    ImageView target = mockImageViewTarget();
    Action action = mockAction(URI_KEY_1, URI_1, target);
    mindLibImage.cancelRequest(target);
    verifyZeroInteractions(action, dispatcher);
  }

  @Test public void cancelExistingRequestWithNullImageView() {
    try {
      mindLibImage.cancelRequest((ImageView) null);
      fail("Canceling with a null ImageView should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test public void cancelExistingRequestWithNullTarget() {
    try {
      mindLibImage.cancelRequest((Target) null);
      fail("Canceling with a null target should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test public void cancelExistingRequestWithImageViewTarget() {
    ImageView target = mockImageViewTarget();
    Action action = mockAction(URI_KEY_1, URI_1, target);
    mindLibImage.enqueueAndSubmit(action);
    assertThat(mindLibImage.targetToAction).hasSize(1);
    mindLibImage.cancelRequest(target);
    assertThat(mindLibImage.targetToAction).isEmpty();
    verify(action).cancel();
    verify(dispatcher).dispatchCancel(action);
  }

  @Test public void cancelExistingRequestWithDeferredImageViewTarget() {
    ImageView target = mockImageViewTarget();
    DeferredRequestCreator deferredRequestCreator = mockDeferredRequestCreator();
    mindLibImage.targetToDeferredRequestCreator.put(target, deferredRequestCreator);
    mindLibImage.cancelRequest(target);
    verify(deferredRequestCreator).cancel();
    assertThat(mindLibImage.targetToDeferredRequestCreator).isEmpty();
  }

  @Test public void enqueueingDeferredRequestCancelsThePreviousOne() throws Exception {
    ImageView target = mockImageViewTarget();
    DeferredRequestCreator firstRequestCreator = mockDeferredRequestCreator();
    mindLibImage.defer(target, firstRequestCreator);
    assertThat(mindLibImage.targetToDeferredRequestCreator).containsKey(target);

    DeferredRequestCreator secondRequestCreator = mockDeferredRequestCreator();
    mindLibImage.defer(target, secondRequestCreator);
    verify(firstRequestCreator).cancel();
    assertThat(mindLibImage.targetToDeferredRequestCreator).containsKey(target);
  }

  @Test public void cancelExistingRequestWithTarget() {
    Target target = mockTarget();
    Action action = mockAction(URI_KEY_1, URI_1, target);
    mindLibImage.enqueueAndSubmit(action);
    assertThat(mindLibImage.targetToAction).hasSize(1);
    mindLibImage.cancelRequest(target);
    assertThat(mindLibImage.targetToAction).isEmpty();
    verify(action).cancel();
    verify(dispatcher).dispatchCancel(action);
  }

  @Test public void cancelExistingRequestWithNullRemoteViews() {
    try {
      mindLibImage.cancelRequest(null, 0);
      fail("Canceling with a null RemoteViews should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Config(sdk = 16) // This test fails on 23 so restore the default level.
  @Test public void cancelExistingRequestWithRemoteViewTarget() {
    int layoutId = 0;
    int viewId = 1;
    RemoteViews remoteViews = new RemoteViews("packageName", layoutId);
    RemoteViewsAction.RemoteViewsTarget target = new RemoteViewsAction.RemoteViewsTarget(remoteViews, viewId);
    Action action = mockAction(URI_KEY_1, URI_1, target);
    mindLibImage.enqueueAndSubmit(action);
    assertThat(mindLibImage.targetToAction).hasSize(1);
    mindLibImage.cancelRequest(remoteViews, viewId);
    assertThat(mindLibImage.targetToAction).isEmpty();
    verify(action).cancel();
    verify(dispatcher).dispatchCancel(action);
  }

  @Test public void cancelNullTagThrows() {
    try {
      mindLibImage.cancelTag(null);
      fail("Canceling with a null tag should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test public void cancelTagAllActions() {
    ImageView target = mockImageViewTarget();
    Action action = mockAction(URI_KEY_1, URI_1, target, "TAG");
    mindLibImage.enqueueAndSubmit(action);
    assertThat(mindLibImage.targetToAction).hasSize(1);
    mindLibImage.cancelTag("TAG");
    assertThat(mindLibImage.targetToAction).isEmpty();
    verify(action).cancel();
  }

  @Test public void cancelTagAllDeferredRequests() {
    ImageView target = mockImageViewTarget();
    DeferredRequestCreator deferredRequestCreator = mockDeferredRequestCreator();
    when(deferredRequestCreator.getTag()).thenReturn("TAG");
    mindLibImage.defer(target, deferredRequestCreator);
    mindLibImage.cancelTag("TAG");
    verify(deferredRequestCreator).cancel();
  }

  @Test public void deferAddsToMap() {
    ImageView target = mockImageViewTarget();
    DeferredRequestCreator deferredRequestCreator = mockDeferredRequestCreator();
    assertThat(mindLibImage.targetToDeferredRequestCreator).isEmpty();
    mindLibImage.defer(target, deferredRequestCreator);
    assertThat(mindLibImage.targetToDeferredRequestCreator).hasSize(1);
  }

  @Test public void shutdown() {
    mindLibImage.shutdown();
    verify(cache).clear();
    verify(stats).shutdown();
    verify(dispatcher).shutdown();
    assertThat(mindLibImage.shutdown).isTrue();
  }

  @Test public void shutdownTwice() {
    mindLibImage.shutdown();
    mindLibImage.shutdown();
    verify(cache).clear();
    verify(stats).shutdown();
    verify(dispatcher).shutdown();
    assertThat(mindLibImage.shutdown).isTrue();
  }

  @Test public void shutdownDisallowedOnSingletonInstance() {
    MindLibImage.singleton = null;
    try {
      MindLibImage mindLibImage = MindLibImage.with(RuntimeEnvironment.application);
      mindLibImage.shutdown();
      fail("Calling shutdown() on static singleton instance should throw");
    } catch (UnsupportedOperationException expected) {
    }
  }

  @Test public void shutdownDisallowedOnCustomSingletonInstance() {
    MindLibImage.singleton = null;
    try {
      MindLibImage mindLibImage = new MindLibImage.Builder(RuntimeEnvironment.application).build();
      MindLibImage.setSingletonInstance(mindLibImage);
      mindLibImage.shutdown();
      fail("Calling shutdown() on static singleton instance should throw");
    } catch (UnsupportedOperationException expected) {
    }
  }

  @Test public void setSingletonInstanceRejectsNull() {
    MindLibImage.singleton = null;

    try {
      MindLibImage.setSingletonInstance(null);
      fail("Can't set singleton instance to null.");
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("MindLibImage must not be null.");
    }
  }

  @Test public void setSingletonInstanceMayOnlyBeCalledOnce() {
    MindLibImage.singleton = null;

    MindLibImage mindLibImage = new MindLibImage.Builder(RuntimeEnvironment.application).build();
    MindLibImage.setSingletonInstance(mindLibImage);

    try {
      MindLibImage.setSingletonInstance(mindLibImage);
      fail("Can't set singleton instance twice.");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Singleton instance already exists.");
    }
  }

  @Test public void setSingletonInstanceAfterWithFails() {
    MindLibImage.singleton = null;

    // Implicitly create the default singleton instance.
    MindLibImage.with(RuntimeEnvironment.application);

    MindLibImage mindLibImage = new MindLibImage.Builder(RuntimeEnvironment.application).build();
    try {
      MindLibImage.setSingletonInstance(mindLibImage);
      fail("Can't set singleton instance after with().");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Singleton instance already exists.");
    }
  }

  @Test public void setSingleInstanceReturnedFromWith() {
    MindLibImage.singleton = null;
    MindLibImage mindLibImage = new MindLibImage.Builder(RuntimeEnvironment.application).build();
    MindLibImage.setSingletonInstance(mindLibImage);
    assertThat(MindLibImage.with(RuntimeEnvironment.application)).isSameAs(mindLibImage);
  }

  @Test public void shutdownClearsDeferredRequests() {
    DeferredRequestCreator deferredRequestCreator = mockDeferredRequestCreator();
    ImageView target = mockImageViewTarget();
    mindLibImage.targetToDeferredRequestCreator.put(target, deferredRequestCreator);
    mindLibImage.shutdown();
    verify(deferredRequestCreator).cancel();
    assertThat(mindLibImage.targetToDeferredRequestCreator).isEmpty();
  }

  @Test public void whenTransformRequestReturnsNullThrows() {
    try {
      when(transformer.transformRequest(any(Request.class))).thenReturn(null);
      mindLibImage.transformRequest(new Request.Builder(URI_1).build());
      fail("Returning null from transformRequest() should throw");
    } catch (IllegalStateException expected) {
    }
  }

  @Test public void getSnapshotInvokesStats() {
    mindLibImage.getSnapshot();
    verify(stats).createSnapshot();
  }

  @Test public void enableIndicators() {
    assertThat(mindLibImage.areIndicatorsEnabled()).isFalse();
    mindLibImage.setIndicatorsEnabled(true);
    assertThat(mindLibImage.areIndicatorsEnabled()).isTrue();
  }

  @Test public void loadThrowsWithInvalidInput() {
    try {
      mindLibImage.load("");
      fail("Empty URL should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
    try {
      mindLibImage.load("      ");
      fail("Empty URL should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
    try {
      mindLibImage.load(0);
      fail("Zero resourceId should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test public void builderInvalidListener() {
    try {
      new MindLibImage.Builder(context).listener(null);
      fail("Null listener should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
    try {
      new MindLibImage.Builder(context).listener(listener).listener(listener);
      fail("Setting Listener twice should throw exception.");
    } catch (IllegalStateException expected) {
    }
  }

  @Test public void builderInvalidLoader() {
    try {
      new MindLibImage.Builder(context).downloader(null);
      fail("Null Downloader should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
    try {
      new MindLibImage.Builder(context).downloader(downloader).downloader(downloader);
      fail("Setting Downloader twice should throw exception.");
    } catch (IllegalStateException expected) {
    }
  }

  @Test public void builderInvalidExecutor() {
    try {
      new MindLibImage.Builder(context).executor(null);
      fail("Null Executor should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
    try {
      ExecutorService executor = mock(ExecutorService.class);
      new MindLibImage.Builder(context).executor(executor).executor(executor);
      fail("Setting Executor twice should throw exception.");
    } catch (IllegalStateException expected) {
    }
  }

  @Test public void builderInvalidCache() {
    try {
      new MindLibImage.Builder(context).memoryCache(null);
      fail("Null Cache should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
    try {
      new MindLibImage.Builder(context).memoryCache(cache).memoryCache(cache);
      fail("Setting Cache twice should throw exception.");
    } catch (IllegalStateException expected) {
    }
  }

  @Test public void builderInvalidRequestTransformer() {
    try {
      new MindLibImage.Builder(context).requestTransformer(null);
      fail("Null request transformer should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
    try {
      new MindLibImage.Builder(context).requestTransformer(transformer).requestTransformer(transformer);
      fail("Setting request transformer twice should throw exception.");
    } catch (IllegalStateException expected) {
    }
  }

  @Test public void builderInvalidRequestHandler() {
    try {
      new MindLibImage.Builder(context).addRequestHandler(null);
      fail("Null request handler should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
    try {
      new MindLibImage.Builder(context).addRequestHandler(requestHandler)
          .addRequestHandler(requestHandler);
      fail("Registering same request handler twice should throw exception.");
    } catch (IllegalStateException expected) {
    }
  }

  @Test public void builderWithoutRequestHandler() {
    MindLibImage mindLibImage = new MindLibImage.Builder(RuntimeEnvironment.application).build();
    assertThat(mindLibImage.getRequestHandlers()).isNotEmpty().doesNotContain(requestHandler);
  }

  @Test public void builderWithRequestHandler() {
    MindLibImage mindLibImage = new MindLibImage.Builder(RuntimeEnvironment.application)
        .addRequestHandler(requestHandler).build();
    assertThat(mindLibImage.getRequestHandlers()).isNotNull().isNotEmpty().contains(requestHandler);
  }

  @Test public void builderInvalidContext() {
    try {
      new MindLibImage.Builder(null);
      fail("Null context should throw exception.");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test public void builderWithDebugIndicators() {
    MindLibImage mindLibImage = new MindLibImage.Builder(RuntimeEnvironment.application).indicatorsEnabled(true).build();
    assertThat(mindLibImage.areIndicatorsEnabled()).isTrue();
  }

  @Test public void invalidateString() {
    mindLibImage.invalidate("http://example.com");
    verify(cache).clearKeyUri("http://example.com");
  }

  @Test public void invalidateFile() {
    mindLibImage.invalidate(new File("/foo/bar/baz"));
    verify(cache).clearKeyUri("file:///foo/bar/baz");
  }

  @Test public void invalidateUri() {
    mindLibImage.invalidate(Uri.parse("mock://12345"));
    verify(cache).clearKeyUri("mock://12345");
  }
}
