import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
	`java-library`
	id("com.github.johnrengelman.shadow") version "8.1.1"
}

val jadxVersion = "1.5.3"

dependencies {
	compileOnly("io.github.skylot:jadx-core:$jadxVersion")
	compileOnly("org.slf4j:slf4j-api:2.0.17")

	testImplementation("io.github.skylot:jadx-core:$jadxVersion")
	testImplementation("org.slf4j:slf4j-api:2.0.17")
	testImplementation("io.github.skylot:jadx-smali-input:$jadxVersion")
	testImplementation("ch.qos.logback:logback-classic:1.5.18")
	testImplementation("org.assertj:assertj-core:3.27.3")
	testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

version = System.getenv("VERSION") ?: "dev"

tasks {
	withType<Test> {
		useJUnitPlatform()
	}

	val shadowJar = withType<ShadowJar> {
		archiveClassifier.set("")
	}

	register<Copy>("dist") {
		group = "jadx-plugin"
		dependsOn(shadowJar)
		from(shadowJar)
		into(layout.buildDirectory.dir("dist"))
	}
}
