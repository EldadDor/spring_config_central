package com.edx.spring.config.spring_config_central;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SpringConfigCentralApplicationTests {

	@Test
	void contextLoads() {
	}

}
