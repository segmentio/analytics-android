package com.segment.analytics.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.segment.analytics.integrations.BasePayload.Builder;
import com.segment.analytics.integrations.BasePayload.Channel;
import com.segment.analytics.integrations.BasePayload.Type;
import java.util.Date;
import java.util.List;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;

public class BasePayloadTest {

  private List<Builder<? extends BasePayload, ? extends Builder<?, ?>>> builders;

  @Before
  public void setUp() {
    builders =
        ImmutableList.of(
            new AliasPayload.Builder().previousId("previousId").userId("userId"),
            new TrackPayload.Builder().event("event"),
            new ScreenPayload.Builder().name("name"),
            new GroupPayload.Builder().groupId("groupId"),
            new IdentifyPayload.Builder().traits(ImmutableMap.<String, Object>of("foo", "bar")));
  }

  @Test
  public void channelIsSet() {
    for (Builder builder : builders) {
      BasePayload payload = builder.userId("user_id").build();
      assertThat(payload).containsEntry(BasePayload.CHANNEL_KEY, Channel.mobile);
    }
  }

  @Test
  public void nullTimestampThrows() {
    for (int i = 1; i < builders.size(); i++) {
      Builder builder = builders.get(i);

      try {
        //noinspection CheckResult,ConstantConditions
        builder.timestamp(null);
        fail();
      } catch (NullPointerException e) {
        assertThat(e).hasMessage("timestamp == null");
      }
    }
  }

  @Test
  public void timestamp() {
    Date timestamp = new Date();
    for (Builder builder : builders) {
      BasePayload payload = builder.userId("user_id").timestamp(timestamp).build();
      assertThat(payload.timestamp()).isEqualTo(timestamp);
      assertThat(payload).containsKey(BasePayload.TIMESTAMP_KEY);
    }
  }

  @Test
  public void type() {
    for (Builder builder : builders) {
      BasePayload payload = builder.userId("user_id").build();
      assertThat(payload.type())
          .isIn(Type.alias, Type.track, Type.screen, Type.group, Type.identify);
      assertThat(payload).containsKey(BasePayload.TYPE_KEY);
    }
  }

  @Test
  public void anonymousId() {
    for (Builder builder : builders) {
      BasePayload payload = builder.anonymousId("anonymous_id").build();
      assertThat(payload.anonymousId()).isEqualTo("anonymous_id");
      assertThat(payload).containsEntry(BasePayload.ANONYMOUS_ID_KEY, "anonymous_id");
    }
  }

  @Test
  public void invalidUserIdThrows() {
    for (int i = 1; i < builders.size(); i++) {
      Builder builder = builders.get(i);

      try {
        //noinspection CheckResult,ConstantConditions
        builder.userId(null);
        fail();
      } catch (NullPointerException e) {
        assertThat(e).hasMessage("userId cannot be null or empty");
      }

      try {
        //noinspection CheckResult
        builder.userId("");
        fail();
      } catch (NullPointerException e) {
        assertThat(e).hasMessage("userId cannot be null or empty");
      }
    }
  }

  @Test
  public void userId() {
    for (Builder builder : builders) {
      BasePayload payload = builder.userId("user_id").build();
      assertThat(payload.userId()).isEqualTo("user_id");
      assertThat(payload).containsEntry(BasePayload.USER_ID_KEY, "user_id");
    }
  }

  @Test
  public void requiresUserIdOrAnonymousId() {
    for (int i = 1; i < builders.size(); i++) {
      Builder builder = builders.get(i);
      try {
        //noinspection CheckResult
        builder.build();
        fail();
      } catch (NullPointerException e) {
        assertThat(e).hasMessage("either userId or anonymousId is required");
      }
    }
  }

  @Test
  public void invalidMessageIdThrows() {
    for (int i = 1; i < builders.size(); i++) {
      Builder builder = builders.get(i);

      try {
        //noinspection CheckResult,ConstantConditions
        builder.messageId(null);
        fail();
      } catch (NullPointerException e) {
        assertThat(e).hasMessage("messageId cannot be null or empty");
      }

      try {
        //noinspection CheckResult
        builder.messageId("");
        fail();
      } catch (NullPointerException e) {
        assertThat(e).hasMessage("messageId cannot be null or empty");
      }
    }
  }

  @Test
  public void messageId() {
    for (Builder builder : builders) {
      BasePayload payload = builder.userId("user_id").messageId("message_id").build();
      assertThat(payload.messageId()).isEqualTo("message_id");
      assertThat(payload).containsEntry(BasePayload.MESSAGE_ID, "message_id");
    }
  }

  @Test
  public void messageIdIsGenerated() {
    for (Builder builder : builders) {
      BasePayload payload = builder.userId("user_id").build();
      assertThat(payload.messageId()).isNotEmpty();
      assertThat(payload).containsKey(BasePayload.MESSAGE_ID);
    }
  }

  @Test
  public void nullContextThrows() {
    for (int i = 1; i < builders.size(); i++) {
      Builder builder = builders.get(i);

      try {
        //noinspection CheckResult,ConstantConditions
        builder.context(null);
        fail();
      } catch (NullPointerException e) {
        assertThat(e).hasMessage("context == null");
      }
    }
  }

  @Test
  public void context() {
    for (Builder builder : builders) {
      BasePayload payload =
          builder.userId("user_id").context(ImmutableMap.of("foo", "bar")).build();
      assertThat(payload.context()).containsExactly(MapEntry.entry("foo", "bar"));
      assertThat(payload).containsEntry(BasePayload.CONTEXT_KEY, ImmutableMap.of("foo", "bar"));
    }
  }

  @Test
  public void invalidIntegrationKeyThrows() {
    for (int i = 1; i < builders.size(); i++) {
      Builder builder = builders.get(i);

      try {
        //noinspection CheckResult,ConstantConditions
        builder.integration(null, false);
        fail();
      } catch (NullPointerException e) {
        assertThat(e).hasMessage("key cannot be null or empty");
      }

      try {
        //noinspection CheckResult,ConstantConditions
        builder.integration("", true);
        fail();
      } catch (NullPointerException e) {
        assertThat(e).hasMessage("key cannot be null or empty");
      }
    }
  }

  @Test
  public void invalidIntegrationOption() {
    for (int i = 1; i < builders.size(); i++) {
      Builder builder = builders.get(i);

      try {
        //noinspection CheckResult,ConstantConditions
        builder.integration(null, ImmutableMap.of("foo", "bar"));
        fail();
      } catch (NullPointerException e) {
        assertThat(e).hasMessage("key cannot be null or empty");
      }

      try {
        //noinspection CheckResult,ConstantConditions
        builder.integration("", ImmutableMap.of("foo", "bar"));
        fail();
      } catch (NullPointerException e) {
        assertThat(e).hasMessage("key cannot be null or empty");
      }

      try {
        //noinspection CheckResult,ConstantConditions
        builder.integration("foo", null);
        fail();
      } catch (NullPointerException e) {
        assertThat(e).hasMessage("options cannot be null or empty");
      }

      try {
        //noinspection CheckResult,ConstantConditions
        builder.integration("bar", ImmutableMap.of());
        fail();
      } catch (NullPointerException e) {
        assertThat(e).hasMessage("options cannot be null or empty");
      }
    }
  }

  @Test
  public void integrations() {
    for (Builder builder : builders) {
      BasePayload payload =
          builder.userId("user_id").integrations(ImmutableMap.of("foo", "bar")).build();
      assertThat(payload.integrations()).containsExactly(MapEntry.entry("foo", "bar"));
      assertThat(payload)
          .containsEntry(BasePayload.INTEGRATIONS_KEY, ImmutableMap.of("foo", "bar"));
    }
  }

  @Test
  public void integration() {
    for (Builder builder : builders) {
      BasePayload payload = builder.userId("user_id").integration("foo", false).build();
      assertThat(payload.integrations()).containsExactly(MapEntry.entry("foo", false));
    }
  }

  @Test
  public void integrationOptions() {
    for (Builder builder : builders) {
      BasePayload payload =
          builder.userId("user_id").integration("foo", ImmutableMap.of("bar", true)).build();
      assertThat(payload.integrations())
          .containsExactly(MapEntry.entry("foo", ImmutableMap.of("bar", true)));
    }
  }

  @Test
  public void putValue() {
    for (Builder builder : builders) {
      BasePayload payload = builder.userId("user_id").build().putValue("foo", "bar");
      assertThat(payload).containsEntry("foo", "bar");
    }
  }

  @Test
  public void builderCopy() {
    for (Builder builder : builders) {
      BasePayload payload =
          builder.userId("user_id").build().toBuilder().userId("a_new_user_id").build();
      assertThat(payload.userId()).isEqualTo("a_new_user_id");
    }
  }
}
