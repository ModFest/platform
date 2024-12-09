package template

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import net.modfest.platform.gson.GsonCommon

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

	fun healthCheck() {

	}

	suspend fun getVersion(): String {
		val response: DefaultResponse = client.get("/").body()
		return response.version;
	}
}
