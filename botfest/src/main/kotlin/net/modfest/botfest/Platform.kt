package net.modfest.botfest

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.UserBehavior
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import net.modfest.platform.gson.GsonCommon
import net.modfest.platform.pojo.CurrentEventData
import net.modfest.platform.pojo.EventData
import net.modfest.platform.pojo.HealthData
import net.modfest.platform.pojo.UserPatchData
import net.modfest.platform.pojo.Whoami

class Platform(var base_url: String) {
	val client = HttpClient() {
		defaultRequest {
			// Set base url for all requests
			if (!base_url.startsWith("http")) {
				base_url = "http://$base_url";
			}
			url(base_url)

			// Serialize body with json by default
			contentType(ContentType.Application.Json)
		}

		install(ContentNegotiation) {
			gson {
				GsonCommon.configureGson(this)
			}
		}
	}

	fun withAuth(user: UserBehavior): PlatformAuthenticated {
		return PlatformAuthenticated(this.client, user.id)
	}

	fun withAuth(user: Snowflake): PlatformAuthenticated {
		return PlatformAuthenticated(this.client, user)
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

	suspend fun getEventIds(): List<String> {
		return getEvents().map { e -> e.id }
	}
}

/**
 * This class represents all platform api's that need an authenticated user.
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
}

/**
 * Helper method to unwrap platform's errors into friendly exceptions
 */
private suspend fun HttpResponse.unwrapErrors(): HttpResponse {
	if (this.status == HttpStatusCode.BadRequest) {
		val v: SpringBadRequest = this.body();
		throw HttpBadRequest(v.message)
	} else if (!this.status.isSuccess()) {
		throw RuntimeException("Http call return "+this.status)
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

class HttpBadRequest(message: String) : Exception(message) {
}
