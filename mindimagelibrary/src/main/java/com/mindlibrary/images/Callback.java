
package com.mindlibrary.images;

public interface Callback {
  void onSuccess();

  void onError(Exception e);

  class EmptyCallback implements Callback {

    @Override public void onSuccess() {
    }

    @Override public void onError(Exception e) {
    }
  }
}
