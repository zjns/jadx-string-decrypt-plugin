pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		google()
	}
}

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenLocal()
		mavenCentral()
		google()
		maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
	}
}

rootProject.name = "jadx-string-decrypt-plugin"
