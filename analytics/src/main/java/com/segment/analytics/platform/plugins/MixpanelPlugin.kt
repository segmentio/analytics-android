package com.segment.analytics.platform.plugins

import android.app.Activity
import android.os.Bundle
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.segment.analytics.Analytics
import com.segment.analytics.Properties
import com.segment.analytics.Traits
import com.segment.analytics.ValueMap
import com.segment.analytics.integrations.AliasPayload
import com.segment.analytics.integrations.BasePayload
import com.segment.analytics.integrations.GroupPayload
import com.segment.analytics.integrations.IdentifyPayload
import com.segment.analytics.integrations.ScreenPayload
import com.segment.analytics.integrations.TrackPayload
import com.segment.analytics.internal.Utils
import com.segment.analytics.platform.AndroidLifecycle
import com.segment.analytics.platform.DestinationPlugin
import java.util.Date

class MixpanelPlugin : DestinationPlugin(), AndroidLifecycle {
    override val name: String = "Mixpanel"

    private lateinit var mixpanel: MixpanelAPI
    private lateinit var mixpanelPeople: MixpanelAPI.People

    // Config
    private var isPeopleEnabled: Boolean = false
    private var setAllTraitsByDefault: Boolean = false
    private var trackAllPages: Boolean = false
    private var consolidatedPageCalls: Boolean = false
    private var trackCategorizedPages: Boolean = false
    private var trackNamedPages: Boolean = false
    private var token: String = ""
    private var increments: Set<String> = emptySet()
    private var peopleProperties: Set<String> = emptySet()
    private var superProperties: Set<String> = emptySet()

    companion object {
        private const val VIEWED_EVENT_FORMAT = "Viewed %s Screen"
        private val MAPPER: Map<String, String> = mapOf(
                "email" to "\$email",
                "phone" to "\$phone",
                "firstName" to "\$first_name",
                "lastName" to "\$last_name",
                "name" to "\$name",
                "username" to "\$username",
                "createdAt" to "\$created",
        )
    }

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        // Retrieve mixpanel config
        val settings = analytics.projectSettings?.getValueMap("integrations")?.getValueMap("Mixpanel")
        settings?.let {
            consolidatedPageCalls = settings.getBoolean("consolidatedPageCalls", true)
            trackAllPages = settings.getBoolean("trackAllPages", false)
            trackCategorizedPages = settings.getBoolean("trackCategorizedPages", false)
            trackNamedPages = settings.getBoolean("trackNamedPages", false)
            isPeopleEnabled = settings.getBoolean("people", false)
            token = settings.getString("token")
            increments = settings.getStringSet("increments")
            setAllTraitsByDefault = settings.getBoolean("setAllTraitsByDefault", true)
            peopleProperties = settings.getStringSet("peopleProperties")
            superProperties = settings.getStringSet("superProperties")
        }

        mixpanel = MixpanelAPI.getInstance(analytics.application, token)
        analytics.log("MixpanelAPI.getInstance(context, $token)")
        mixpanelPeople = mixpanel.people
    }

    override fun track(payload: TrackPayload?): BasePayload? {
        payload?.let {
            val event: String = payload.event()
            event(event, payload.properties())
            if (increments.contains(event) && isPeopleEnabled) {
                mixpanelPeople.increment(event, 1.0)
                mixpanelPeople["Last $event"] = Date()
            }
        }
        return null
    }

    override fun identify(payload: IdentifyPayload?): BasePayload? {
        payload?.let {
            val userId = payload.userId()
            if (userId != null) {
                mixpanel.identify(userId)
                analytics?.log("mixpanel.identify($userId)")
                if (isPeopleEnabled) {
                    mixpanelPeople.identify(userId)
                    analytics?.log("mixpanel.getPeople().identify($userId)")
                }
            }

            val traits: Traits = payload.traits()

            if (setAllTraitsByDefault) {
                registerSuperProperties(traits)
                setPeopleProperties(traits)
            } else {
                val superPropertyTraits: Map<String, Any> = filter(traits, superProperties)
                registerSuperProperties(superPropertyTraits)
                val peoplePropertyTraits: Map<String, Any> = filter(traits, peopleProperties)
                setPeopleProperties(peoplePropertyTraits)
            }
        }
        return null
    }

    override fun screen(payload: ScreenPayload?): BasePayload? {
        payload?.let {
            if (consolidatedPageCalls) {
                val properties = Properties()
                properties.putAll(payload.properties())
                properties["name"] = payload.name()
                event("Loaded a Screen", properties)
                return null
            }

            if (trackAllPages) {
                event(String.format(VIEWED_EVENT_FORMAT, payload.event()), payload.properties())
            } else if (trackCategorizedPages && !payload.category().isNullOrEmpty()) {
                event(String.format(VIEWED_EVENT_FORMAT, payload.category()), payload.properties())
            } else if (trackNamedPages && !payload.name().isNullOrEmpty()) {
                event(String.format(VIEWED_EVENT_FORMAT, payload.name()), payload.properties())
            }
        }
        return null
    }

    override fun group(payload: GroupPayload?): BasePayload? {
        payload?.let {
            val traits: Traits = payload.traits()
            val groupId: String = payload.groupId()
            var groupName = traits.name()

            // set default groupName

            // set default groupName
            if (groupName.isNullOrEmpty()) {
                groupName = "[Segment] Group"
            }

            // set group traits
            if (!traits.isNullOrEmpty()) {
                mixpanel.getGroup(groupName, groupId).setOnce(traits.toJsonObject())
            }

            // set group
            mixpanel.setGroup(groupName, groupId)
            analytics?.log("mixpanel.setGroup($groupName, $groupId)")
        }
        return null
    }

    override fun alias(payload: AliasPayload?): BasePayload? {
        payload?.let {
            var previousId: String = payload.previousId()
            if (previousId == payload.anonymousId()) {
                // Instead of using our own anonymousId, we use Mixpanel's own generated Id.
                previousId = mixpanel.distinctId
            }
            val userId = payload.userId()
            if (userId != null) {
                mixpanel.alias(userId, previousId)
                analytics?.log("mixpanel.alias($userId, $previousId)")
            }
        }
        return null
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        super.onActivityCreated(activity, savedInstanceState)

        // This is needed to trigger a call to #checkIntentForInboundAppLink.
        // From Mixpanel's source, this won't trigger a creation of another instance. It caches
        // instances by the application context and token, both of which remain the same.
        MixpanelAPI.getInstance(activity, token)
    }

    override fun flush() {
        mixpanel.flush()
        analytics?.log("mixpanel.flush()")
    }

    override fun reset() {
        mixpanel.reset()
        analytics?.log("mixpanel.reset()")
    }

    private fun event(name: String, properties: Properties) {
        val props = properties.toJsonObject()
        mixpanel.track(name, props)
        analytics?.log("mixpanel.track($name, $props)")
        if (isPeopleEnabled) {
            val revenue = properties.revenue()
            if (revenue == 0.0) {
                return
            }
            mixpanelPeople.trackCharge(revenue, props)
            analytics?.log("mixpanelPeople.trackCharge($revenue, $props)")
        }
    }

    private fun registerSuperProperties(props: Map<String, Any>) {
        if (props.isNullOrEmpty()) {
            return
        }
        val superProperties = ValueMap(Utils.transform(props, MAPPER)).toJsonObject()
        mixpanel.registerSuperProperties(superProperties)
        analytics?.log("mixpanel.registerSuperProperties($superProperties)")
    }

    private fun setPeopleProperties(props: Map<String, Any>) {
        if (props.isNullOrEmpty() || !isPeopleEnabled) {
            return
        }
        val peopleProperties = ValueMap(Utils.transform(props, MAPPER)).toJsonObject()
        mixpanelPeople.set(peopleProperties)
        analytics?.log("mixpanel.getPeople().set($peopleProperties)")
    }

    private fun <T> filter(input: Map<String, T>, filter: Iterable<String>): Map<String, T> =
            input.filter { (k, _) -> k in filter }

}

fun ValueMap.getStringSet(key: String): Set<String> {
    return try {
        val incrementEvents = get(key) as List<String>
        if (incrementEvents.isEmpty()) {
            return emptySet()
        }
        incrementEvents.toSet()
    } catch (e: Exception) {
        emptySet()
    }
}