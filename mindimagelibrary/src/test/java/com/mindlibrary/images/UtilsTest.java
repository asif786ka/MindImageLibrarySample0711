
package com.mindlibrary.images;

import android.content.res.Resources;
import java.io.IOException;
import okio.Buffer;

import org.fest.assertions.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static com.mindlibrary.images.TestUtils.RESOURCE_ID_1;
import static com.mindlibrary.images.TestUtils.RESOURCE_ID_URI;
import static com.mindlibrary.images.TestUtils.RESOURCE_TYPE_URI;
import static com.mindlibrary.images.TestUtils.URI_1;
import static com.mindlibrary.images.TestUtils.mockPackageResourceContext;
import static com.mindlibrary.images.Utils.createKey;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricGradleTestRunner.class)
public class UtilsTest {

  @Test public void matchingRequestsHaveSameKey() throws Exception {
    Request request = new Request.Builder(URI_1).build();
    String key1 = Utils.createKey(request);
    String key2 = Utils.createKey(request);
    assertThat(key1).isEqualTo(key2);

    Transformation t1 = new TestTransformation("foo", null);
    Transformation t2 = new TestTransformation("foo", null);

    Request requestTransform1 = new Request.Builder(URI_1).transform(t1).build();
    Request requestTransform2 = new Request.Builder(URI_1).transform(t2).build();

    String single1 = Utils.createKey(requestTransform1);
    String single2 = Utils.createKey(requestTransform2);
    assertThat(single1).isEqualTo(single2);

    Transformation t3 = new TestTransformation("foo", null);
    Transformation t4 = new TestTransformation("bar", null);

    Request requestTransform3 = new Request.Builder(URI_1).transform(t3).transform(t4).build();
    Request requestTransform4 = new Request.Builder(URI_1).transform(t3).transform(t4).build();

    String double1 = Utils.createKey(requestTransform3);
    String double2 = Utils.createKey(requestTransform4);
    assertThat(double1).isEqualTo(double2);

    Transformation t5 = new TestTransformation("foo", null);
    Transformation t6 = new TestTransformation("bar", null);

    Request requestTransform5 = new Request.Builder(URI_1).transform(t5).transform(t6).build();
    Request requestTransform6 = new Request.Builder(URI_1).transform(t6).transform(t5).build();

    String order1 = Utils.createKey(requestTransform5);
    String order2 = Utils.createKey(requestTransform6);
    assertThat(order1).isNotEqualTo(order2);
  }

  @Test public void detectedWebPFile() throws Exception {
    Assertions.assertThat(Utils.isWebPFile(new Buffer().writeUtf8("RIFFxxxxWEBP"))).isTrue();
    Assertions.assertThat(Utils.isWebPFile(new Buffer().writeUtf8("RIFFxxxxxWEBP"))).isFalse();
    Assertions.assertThat(Utils.isWebPFile(new Buffer().writeUtf8("ABCDxxxxWEBP"))).isFalse();
    Assertions.assertThat(Utils.isWebPFile(new Buffer().writeUtf8("RIFFxxxxABCD"))).isFalse();
    Assertions.assertThat(Utils.isWebPFile(new Buffer().writeUtf8("RIFFxxWEBP"))).isFalse();
  }

  @Test public void ensureBuilderIsCleared() throws Exception {
    Request request1 = new Request.Builder(RESOURCE_ID_URI).build();
    Request request2 = new Request.Builder(URI_1).build();
    Utils.createKey(request1);
    assertThat(Utils.MAIN_THREAD_KEY_BUILDER.length()).isEqualTo(0);
    Utils.createKey(request2);
    assertThat(Utils.MAIN_THREAD_KEY_BUILDER.length()).isEqualTo(0);
  }

  @Test public void getResourceById() throws IOException {
    Request request = new Request.Builder(RESOURCE_ID_URI).build();
    Resources res = Utils.getResources(mockPackageResourceContext(), request);
    int id = Utils.getResourceId(res, request);
    assertThat(id).isEqualTo(RESOURCE_ID_1);
  }

  @Test public void getResourceByTypeAndName() throws IOException {
    Request request = new Request.Builder(RESOURCE_TYPE_URI).build();
    Resources res = Utils.getResources(mockPackageResourceContext(), request);
    int id = Utils.getResourceId(res, request);
    assertThat(id).isEqualTo(RESOURCE_ID_1);
  }
}
