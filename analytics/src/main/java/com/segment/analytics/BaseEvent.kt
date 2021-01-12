package com.segment.analytics

import com.segment.analytics.internal.Utils
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Date

class DateSerializer : KSerializer<Date> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Date {
        val timestamp = decoder.decodeString()
        return Utils.parseISO8601Date(timestamp)
    }

    override fun serialize(encoder: Encoder, value: Date) {
        val timestamp = Utils.toISO8601String(value)
        encoder.encodeString(timestamp)
    }

}

//typealias Context = JsonObject
//typealias Integrations = JsonObject
//typealias Properties = JsonObject
//typealias Traits = JsonObject
//
//abstract class BaseEvent {
//    abstract var anonymousId: String
//    abstract var messageId: String
//    abstract var timestamp: NanoDate
//    abstract var context: Context
//    abstract var integrations: Integrations
//}
//
//@Serializable
//data class TrackEvent(
//        override var anonymousId: String,
//        override var messageId: String,
//        @Serializable(with = DateSerializer::class) override var timestamp: NanoDate,
//        override var context: Context,
//        override var integrations: Integrations,
//        var properties: Properties
//) : BaseEvent()
//
//@Serializable
//data class IdentifyEvent(
//        override var anonymousId: String,
//        override var messageId: String,
//        @Serializable(with = DateSerializer::class) override var timestamp: NanoDate,
//        override var context: Context,
//        override var integrations: Integrations,
//        var traits: Traits,
//        var userId: String
//) : BaseEvent()
//
//
//@Serializable
//data class GroupEvent(
//        override var anonymousId: String,
//        override var messageId: String,
//        @Serializable(with = DateSerializer::class) override var timestamp: NanoDate,
//        override var context: Context,
//        override var integrations: Integrations,
//        var traits: Traits,
//        var groupId: String
//) : BaseEvent()
//
//@Serializable
//data class Alias(
//        override var anonymousId: String,
//        override var messageId: String,
//        @Serializable(with = DateSerializer::class) override var timestamp: NanoDate,
//        override var context: Context,
//        override var integrations: Integrations,
//        var previousId: String,
//        var userId: String
//) : BaseEvent()
//
//@Serializable
//data class ScreenEvent(
//        override var anonymousId: String,
//        override var messageId: String,
//        @Serializable(with = DateSerializer::class) override var timestamp: NanoDate,
//        override var context: Context,
//        override var integrations: Integrations,
//        var category: String,
//        var name: String,
//        var properties: Properties
//) : BaseEvent()