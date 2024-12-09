package net.modfest.botfest

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
 * There are more fields, but this is the one we care about
 */
private class SpringBadRequest(val message: String) {
}

class HttpBadRequest(message: String) : Exception(message) {
}
