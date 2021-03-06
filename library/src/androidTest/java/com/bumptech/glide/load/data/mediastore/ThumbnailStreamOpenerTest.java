package com.bumptech.glide.load.data.mediastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.bumptech.glide.load.engine.bitmap_recycle.ByteArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.LruByteArrayPool;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboCursor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, emulateSdk = 18)
public class ThumbnailStreamOpenerTest {
  private Harness harness;

  @Before
  public void setUp() {
    harness = new Harness();
  }

  @Test
  public void testReturnsNullIfCursorIsNull() throws FileNotFoundException {
    when(harness.query.query(eq(RuntimeEnvironment.application), eq(harness.uri))).thenReturn(null);
    assertNull(harness.get().open(RuntimeEnvironment.application, harness.uri));
  }

  @Test
  public void testReturnsNullIfCursorIsEmpty() throws FileNotFoundException {
    when(harness.query.query(eq(RuntimeEnvironment.application), eq(harness.uri)))
        .thenReturn(new MatrixCursor(new String[1]));
    assertNull(harness.get().open(RuntimeEnvironment.application, harness.uri));
  }

  @Test
  public void testReturnsNullIfCursorHasEmptyPath() throws FileNotFoundException {
    MatrixCursor cursor = new MatrixCursor(new String[1]);
    cursor.addRow(new Object[] { "" });
    when(harness.query.query(eq(RuntimeEnvironment.application), eq(harness.uri)))
        .thenReturn(cursor);
    assertNull(harness.get().open(RuntimeEnvironment.application, harness.uri));
  }

  @Test
  public void testReturnsNullIfFileDoesNotExist() throws FileNotFoundException {
    when(harness.service.get(anyString())).thenReturn(harness.file);
    when(harness.service.exists(eq(harness.file))).thenReturn(false);
    assertNull(harness.get().open(RuntimeEnvironment.application, harness.uri));
  }

  @Test
  public void testReturnNullIfFileLengthIsZero() throws FileNotFoundException {
    when(harness.service.get(anyString())).thenReturn(harness.file);
    when(harness.service.length(eq(harness.file))).thenReturn(0L);
    assertNull(harness.get().open(RuntimeEnvironment.application, harness.uri));
  }

  @Test
  public void testClosesCursor() throws FileNotFoundException {
    harness.get().open(RuntimeEnvironment.application, harness.uri);
    assertTrue(harness.cursor.isClosed());
  }

  @Test
  public void testReturnsOpenedInputStreamWhenFileFound() throws FileNotFoundException {
    InputStream expected = new ByteArrayInputStream(new byte[0]);
    Shadows.shadowOf(RuntimeEnvironment.application.getContentResolver())
        .registerInputStream(harness.uri, expected);
    assertEquals(expected, harness.get().open(RuntimeEnvironment.application, harness.uri));
  }

  @Test
  public void testVideoQueryReturnsVideoCursor() {
    Uri queryUri = MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI;
    ThumbFetcher.VideoThumbnailQuery query = new ThumbFetcher.VideoThumbnailQuery();
    RoboCursor testCursor = new RoboCursor();
    Shadows.shadowOf(RuntimeEnvironment.application.getContentResolver())
        .setCursor(queryUri, testCursor);
    assertEquals(testCursor, query.query(RuntimeEnvironment.application, harness.uri));
  }

  @Test
  public void testImageQueryReturnsImageCurosr() {
    Uri queryUri = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI;
    ThumbFetcher.ImageThumbnailQuery query = new ThumbFetcher.ImageThumbnailQuery();
    RoboCursor testCursor = new RoboCursor();
    Shadows.shadowOf(RuntimeEnvironment.application.getContentResolver())
        .setCursor(queryUri, testCursor);
    assertEquals(testCursor, query.query(RuntimeEnvironment.application, harness.uri));
  }

  private static class Harness {
    MatrixCursor cursor = new MatrixCursor(new String[1]);
    File file = new File("fake/uri");
    Uri uri = Uri.fromFile(file);
    ThumbnailQuery query = mock(ThumbnailQuery.class);
    FileService service = mock(FileService.class);
    ByteArrayPool byteArrayPool = new LruByteArrayPool();

    public Harness() {
      cursor.addRow(new String[] { file.getAbsolutePath() });
      when(query.query(eq(RuntimeEnvironment.application), eq(uri))).thenReturn(cursor);
      when(service.get(eq(file.getAbsolutePath()))).thenReturn(file);
      when(service.exists(eq(file))).thenReturn(true);
      when(service.length(eq(file))).thenReturn(1L);
    }

    public ThumbnailStreamOpener get() {
      return new ThumbnailStreamOpener(service, query, byteArrayPool);
    }
  }
}