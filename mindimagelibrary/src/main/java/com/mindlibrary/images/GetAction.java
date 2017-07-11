
package com.mindlibrary.images;

import android.graphics.Bitmap;

class GetAction extends Action<Void> {
  GetAction(MindLibImage mindLibImage, Request data, int memoryPolicy, int networkPolicy, Object tag,
            String key) {
    super(mindLibImage, null, data, memoryPolicy, networkPolicy, 0, null, key, tag, false);
  }

  @Override void complete(Bitmap result, MindLibImage.LoadedFrom from) {
  }

  @Override public void error(Exception e) {
  }
}
