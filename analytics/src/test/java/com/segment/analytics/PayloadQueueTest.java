package com.segment.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.squareup.burst.BurstJUnit4;
import com.squareup.burst.annotation.Burst;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import okio.ByteString;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

@RunWith(BurstJUnit4.class)
public class PayloadQueueTest {
  public enum QueueFactory {
    FILE() {
      @Override
      public PayloadQueue create(QueueFile queueFile) throws IOException {
        return new PayloadQueue.PersistentQueue(queueFile);
      }
    },
    MEMORY() {
      @Override
      public PayloadQueue create(QueueFile file) {
        return new PayloadQueue.MemoryQueue();
      }
    };

    public abstract PayloadQueue create(QueueFile queueFile) throws IOException;
  }

  @Rule public TemporaryFolder folder = new TemporaryFolder();
  @Burst QueueFactory factory;
  PayloadQueue queue;

  @Before
  public void setUp() throws IOException {
    File parent = folder.getRoot();
    File file = new File(parent, "payload-queue");
    QueueFile queueFile = new QueueFile(file);

    queue = factory.create(queueFile);
    queue.add(bytes("one"));
    queue.add(bytes("two"));
    queue.add(bytes("three"));
  }

  @Test
  public void size() throws IOException {
    assertThat(queue.size()).isEqualTo(3);
  }

  @Test
  public void forEach() throws IOException {
    List<byte[]> seen = readQueue(queue.size() + 1);
    assertThat(seen).containsExactly(bytes("one"), bytes("two"), bytes("three"));
  }

  @Test
  public void forEachEarlyReturn() throws IOException {
    List<byte[]> seen = readQueue(2);
    assertThat(seen).containsExactly(bytes("one"), bytes("two"));
  }

  @Test
  public void remove() throws IOException {
    queue.remove(2);

    assertThat(queue.size()).isEqualTo(1);

    List<byte[]> seen = readQueue(1);
    assertThat(seen).containsExactly(bytes("three"));
  }

  private static byte[] bytes(String s) {
    return ByteString.encodeUtf8(s).toByteArray();
  }

  private List<byte[]> readQueue(final int maxCount) throws IOException {
    final List<byte[]> seen = new ArrayList<>();
    queue.forEach(
        new PayloadQueue.ElementVisitor() {
          int count = 1;

          @Override
          public boolean read(InputStream in, int length) throws IOException {
            byte[] data = new byte[length];
            assertThat(in.read(data)).isEqualTo(length);
            seen.add(data);
            return count++ < maxCount;
          }
        });
    return seen;
  }
}
