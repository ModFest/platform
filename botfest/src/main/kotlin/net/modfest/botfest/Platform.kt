package net.modfest.botfest

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.gson.*
import net.modfest.platform.gson.GsonCommon
import net.modfest.platform.pojo.HealthData

class Platform(var base_url: String) {
	val client = HttpClient() {
		defaultRequest {
			if (!base_url.startsWith("http")) {
				base_url = "http://$base_url";
			}
			url(base_url)
		}

		install(ContentNegotiation) {
			gson {
				GsonCommon.configureGson(this)
			}
		}
	}

	suspend fun getHealth(): HealthData {
		return client.get("/health").body()
	}
}
