# Gradle

> 开发配置

	dependencies {
		compile (
			// Gradle
			[group: 'org.ow2.asm', name: 'asm-all', version: '5.1'],
			fileTree(
				dir:gradle.gradleHomeDir.getAbsolutePath() + '/lib',
				include:['gradle-*.jar', 'native-*.jar', 'jansi-*.jar', 'kotlin-*.jar', 'groovy-*.jar', 'log4j-*.jar']
			),
		)
	}
	configurations {
		all*.exclude group: 'org.slf4j', module: 'slf4j-log4j12'
	}

> 期望目标

通过嵌入Gradle管理插件之间的依赖，但目前这方面的文档比较欠缺，暂时搁置。