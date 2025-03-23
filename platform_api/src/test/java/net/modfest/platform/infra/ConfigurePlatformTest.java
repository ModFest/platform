package net.modfest.platform.infra;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Import(TestConfig.class)
public @interface ConfigurePlatformTest {
}
