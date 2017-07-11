
package com.mindlibrary.images;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import static com.mindlibrary.images.MindLibImage.Priority;

abstract class Action<T> {
  static class RequestWeakReference<M> extends WeakReference<M> {
    final Action action;

    public RequestWeakReference(Action action, M referent, ReferenceQueue<? super M> q) {
      super(referent, q);
      this.action = action;
    }
  }

  final MindLibImage mindLibImage;
  final Request request;
  final WeakReference<T> target;
  final boolean noFade;
  final int memoryPolicy;
  final int networkPolicy;
  final int errorResId;
  final Drawable errorDrawable;
  final String key;
  final Object tag;

  boolean willReplay;
  boolean cancelled;

  Action(MindLibImage mindLibImage, T target, Request request, int memoryPolicy, int networkPolicy,
         int errorResId, Drawable errorDrawable, String key, Object tag, boolean noFade) {
    this.mindLibImage = mindLibImage;
    this.request = request;
    this.target =
        target == null ? null : new RequestWeakReference<>(this, target, mindLibImage.referenceQueue);
    this.memoryPolicy = memoryPolicy;
    this.networkPolicy = networkPolicy;
    this.noFade = noFade;
    this.errorResId = errorResId;
    this.errorDrawable = errorDrawable;
    this.key = key;
    this.tag = (tag != null ? tag : this);
  }

  abstract void complete(Bitmap result, MindLibImage.LoadedFrom from);

  abstract void error(Exception e);

  void cancel() {
    cancelled = true;
  }

  Request getRequest() {
    return request;
  }

  T getTarget() {
    return target == null ? null : target.get();
  }

  String getKey() {
    return key;
  }

  boolean isCancelled() {
    return cancelled;
  }

  boolean willReplay() {
    return willReplay;
  }

  int getMemoryPolicy() {
    return memoryPolicy;
  }

  int getNetworkPolicy() {
    return networkPolicy;
  }

  MindLibImage getMindLibImage() {
    return mindLibImage;
  }

  Priority getPriority() {
    return request.priority;
  }

  Object getTag() {
    return tag;
  }
}
