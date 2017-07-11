
package com.mindlibrary.images;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

final class TargetAction extends Action<Target> {

  TargetAction(MindLibImage mindLibImage, Target target, Request data, int memoryPolicy, int networkPolicy,
               Drawable errorDrawable, String key, Object tag, int errorResId) {
    super(mindLibImage, target, data, memoryPolicy, networkPolicy, errorResId, errorDrawable, key, tag,
        false);
  }

  @Override void complete(Bitmap result, MindLibImage.LoadedFrom from) {
    if (result == null) {
      throw new AssertionError(
          String.format("Attempted to complete action with no result!\n%s", this));
    }
    Target target = getTarget();
    if (target != null) {
      target.onBitmapLoaded(result, from);
      if (result.isRecycled()) {
        throw new IllegalStateException("Target callback must not recycle bitmap!");
      }
    }
  }

  @Override void error(Exception e) {
    Target target = getTarget();
    if (target != null) {
      if (errorResId != 0) {
        target.onBitmapFailed(e, mindLibImage.context.getResources().getDrawable(errorResId));
      } else {
        target.onBitmapFailed(e, errorDrawable);
      }
    }
  }
}
