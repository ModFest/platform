package net.modfest.platform;

import net.modfest.platform.infra.ConfigurePlatformTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
			.andExpect(status().isNotFound()); // There should be no users inside the test db
	}
}
