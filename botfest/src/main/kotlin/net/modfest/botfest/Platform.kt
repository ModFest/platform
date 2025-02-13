package net.modfest.botfest

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.UserBehavior
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.util.logging.*
import net.modfest.platform.gson.GsonCommon
import net.modfest.platform.pojo.CurrentEventData
import net.modfest.platform.pojo.EventData
import net.modfest.platform.pojo.HealthData
import net.modfest.platform.pojo.PlatformErrorResponse
import net.modfest.platform.pojo.SubmissionData
import net.modfest.platform.pojo.SubmitRequest
import net.modfest.platform.pojo.UserCreateData
import net.modfest.platform.pojo.UserData
import net.modfest.platform.pojo.UserPatchData
import net.modfest.platform.pojo.Whoami
import nl.theepicblock.sseclient.ReconnectionInfo
import nl.theepicblock.sseclient.SseClient
import nl.theepicblock.sseclient.SseEvent
import java.net.URI
import java.net.http.HttpRequest
import kotlin.time.Duration

val LOGGER = KotlinLogging.logger("Platform API")

class Platform(baseUrl: String) {
	private val baseUrl: String
	private val client: HttpClient

	init {
		var tmpUrl = baseUrl
		if (!tmpUrl.startsWith("http")) {
			tmpUrl = "http://$baseUrl";
		}
		tmpUrl = tmpUrl.removeSuffix("/")
		this.baseUrl = tmpUrl
		client = HttpClient() {
			defaultRequest {
				url(tmpUrl)

				// Serialize body with json by default
				contentType(ContentType.Application.Json)
			}

			install(ContentNegotiation) {
				gson {
					GsonCommon.configureGson(this)
				}
			}

			// Server sent events
			install(SSE) {
				reconnectionTime = Duration.parse("5s")
			}

			install(HttpRequestRetry)
		}
	}


	fun withAuth(user: UserBehavior): PlatformAuthenticated {
		return PlatformAuthenticated(this.client, user.id)
	}

	fun withAuth(user: Snowflake): PlatformAuthenticated {
		return PlatformAuthenticated(this.client, user)
	}

	fun authenticatedAsBotFest(): PlatformBotFestAuthenticated {
		return PlatformBotFestAuthenticated(this.client, this.baseUrl)
	}

	suspend fun getHealth(): HealthData {
		return client.get("/health").unwrapErrors().body()
	}

	suspend fun getCurrentEvent(): CurrentEventData {
		return client.get("/currentevent/").unwrapErrors().body()
	}

	suspend fun setCurrentEvent(data: CurrentEventData) {
		client.put("/currentevent/") {
			setBody(data)
		}.unwrapErrors()
	}

	suspend fun getEvents(): List<EventData> {
		return client.get("/events").unwrapErrors().body()
	}

	suspend fun getEvent(eventId: String): EventData {
		return client.get("/event/$eventId").unwrapErrors().body()
	}

	suspend fun getEventIds(): List<String> {
		return getEvents().map { e -> e.id }
	}

	/**
	 * Retrieve a user by their discord id. Will be null if the user does not exist
	 */
	suspend fun getUser(user: UserBehavior): UserData? {
		return getUser(user.id)
	}

	/**
	 * Retrieve a user by their discord id. Will be null if the user does not exist
	 */
	suspend fun getUser(user: Snowflake): UserData? {
		return client.get("/user/dc:$user").apply {
			// Map 404 errors to be null
			if (status == HttpStatusCode.NotFound) return null
		}.unwrapErrors().body()
	}

	/**
	 * Retrieve a user by their modfest id. Will be null if the user does not exist
	 */
	suspend fun getUser(user: String): UserData? {
		return client.get("/user/$user").apply {
			// Map 404 errors to be null
			if (status == HttpStatusCode.NotFound) return null
		}.unwrapErrors().body()
	}
}

/**
 * This class represents all platform api's that need an authenticated user.
 * This is for routes authenticated as a user, if you need the call to be
 * authenticated as BotFest itself, use [PlatformBotFestAuthenticated]
 */
class PlatformAuthenticated(var client: HttpClient, var discordUser: Snowflake) {
	private fun HttpRequestBuilder.addAuth() {
		header("BotFest-Secret", PLATFORM_SHARED_SECRET)
		header("BotFest-Target-User", discordUser.value)
	}
	
	suspend fun getAuthenticatedUserInfo(): Whoami {
		return client.get("/meta/me") {
			addAuth()
		}.unwrapErrors().body()
	}

	suspend fun reloadFromFilesystem(): Int {
		return client.post("/meta/reload") {
			addAuth()
		}.unwrapErrors().body()
	}

	suspend fun patchUserData(patch: UserPatchData) {
		client.patch("/user/@me") {
			addAuth()
			setBody(patch)
		}.unwrapErrors();
	}

	suspend fun submitModrinth(eventId: String, mrId: String): SubmissionData {
		return client.post("/event/$eventId/submissions") {
			addAuth()
			setBody(SubmitRequest(mrId))
		}.unwrapErrors().body()
	}

	suspend fun registerMe(event: EventData) {
		client.put("/event/"+event.id+"/registrations/dc:"+discordUser.value) {
			addAuth()
		}.unwrapErrors()
	}

	suspend fun unregisterMe(event: EventData) {
		client.delete("/event/"+event.id+"/registrations/dc:"+discordUser.value) {
			addAuth()
		}.unwrapErrors()
	}
}

/**
 * This class represents all platform api's that need to be authenticated.
 * This will authenticate as BotFest itself. If BotFest is performing an action on behalf of
 * an already existing user, use [PlatformAuthenticated].
 */
class PlatformBotFestAuthenticated(val client: HttpClient, val base_url: String) {
	private fun HttpRequestBuilder.addAuth() {
		header("BotFest-Secret", PLATFORM_SHARED_SECRET)
		header("BotFest-Target-User", "@self")
	}

	private fun HttpRequest.Builder.addAuth() {
		header("BotFest-Secret", PLATFORM_SHARED_SECRET)
		header("BotFest-Target-User", "@self")
	}

	suspend fun createUser(data: UserCreateData): UserData {
		return client.post("/users") {
			addAuth()
			setBody(data)
		}.unwrapErrors().body()
	}

	fun userChanges(onEvent: (SseEvent) -> Unit, onConnect: () -> Unit): SseClient {
		return object : SseClient() {
			init {
			    this.retryDelayMillis = 5_000
			}

			override fun onEvent(e: SseEvent?) {
				try {
					if (e != null) onEvent(e)
				} catch (e: Throwable) {
					LOGGER.warn(e) { "error whilst processing SSE event "}
				}
			}

			override fun configureRequest(builder: HttpRequest.Builder?) {
				builder?.uri(URI.create("$base_url/users/subscribe"))
				builder?.addAuth()
			}

			override fun onConnect() {
				LOGGER.debug { "Platform SSE stream connected" }
				try {
					onConnect()
				} catch (e: Throwable) {
					LOGGER.warn(e) { "error in onConnect "}
				}
			}

			override fun onReconnect(reconnectionInfo: ReconnectionInfo): java.time.Duration? {
				if (reconnectionInfo.statusCode() == 403) {
					LOGGER.error { "Failed to subscribe to platform. Http status code 403" }
					return null // don't reconnect
				}
				var reconnTime = if (reconnectionInfo.wasConnectionInvalid()) {
					// Something is majorly wrong. Give the server some time
					java.time.Duration.ofMinutes(1)
				} else {
					java.time.Duration.ofMillis(when (1) {
						in 0..1 -> retryDelayMillis!!
						in 2..5 -> retryDelayMillis!! * retryDelayMillis
						else -> retryDelayMillis!! * 7
					})
				}
				LOGGER.debug { "Attempting to reconnect to platform SSE in $reconnTime. " +
					"(did connect = ${!reconnectionInfo.connectionFailed()}, " +
					"was valid = ${reconnectionInfo.wasConnectionInvalid()}, " +
					"status code = ${reconnectionInfo.statusCode()}, " +
					"error = ${reconnectionInfo.error()})" }
				return reconnTime
			}

			override fun onDisconnect() {
				LOGGER.warn { "Platform SSE stream was disconnected" }
			}
		}
	}

	suspend fun getUsers(): List<UserData> {
		return client.get("/users") {
			addAuth()
		}.unwrapErrors().body()
	}
}

/**
 * Helper method to unwrap platform's errors into friendly exceptions
 */
private suspend fun HttpResponse.unwrapErrors(): HttpResponse {
	if (!this.status.isSuccess()) {
		val v: PlatformErrorResponse = this.body();
		throw PlatformException(v)
	} else {
		return this
	}
}

/**
 * Data model for platform's 400 return messages.
 * There are more fields, but this is the one we care about
 */
private class SpringBadRequest(val message: String) {
}

class PlatformException(val data: PlatformErrorResponse) : Exception() {
	override val message: String
		get() = data.toString()
}
