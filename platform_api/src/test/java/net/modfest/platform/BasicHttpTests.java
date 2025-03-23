package net.modfest.platform;

import net.modfest.platform.infra.ConfigurePlatformTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ConfigurePlatformTest
@SpringBootTest
@AutoConfigureMockMvc
public class BasicHttpTests {
	@Autowired
	private MockMvc mockMvc;

	@Test
	public void testHealth() throws Exception {
		this.mockMvc.perform(get("/health"))
			.andExpect(status().isOk());
	}

	@Test
	public void testGetUsers() throws Exception {
		this.mockMvc.perform(get("/user/123456"))
			.andExpect(status().isNotFound())
			.andExpectAll(isProperPlatformError()); // There should be no users inside the test db
	}

	@Test
	public void notFound() throws Exception {
		this.mockMvc.perform(get("/dauhdawhduiwhodhwqojdwo"))
			.andExpect(status().isNotFound())
			.andExpectAll(isProperPlatformError());
	}

	/**
	 * Tests if a response matches a {@link net.modfest.platform.pojo.PlatformErrorResponse}
	 */
	private static ResultMatcher[] isProperPlatformError() {
		return new ResultMatcher[]{
			content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
			jsonPath("$.type").isString(),
			jsonPath("$.data").exists()
		};
	}
}
