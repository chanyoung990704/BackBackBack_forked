package com.aivle.project;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModulithVerificationTest {

	private final ApplicationModules modules = ApplicationModules.of(ProjectApplication.class);

	@Test
	void writeDocumentation() {
		new Documenter(modules)
				.writeDocumentation()
				.writeModulesAsPlantUml();
	}

	@Test
	void verifyModules() {
		modules.verify();
	}
}
