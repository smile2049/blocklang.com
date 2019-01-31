package com.blocklang.release.task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class AppBuildContext {
	
	private static final Logger logger = LoggerFactory.getLogger(AppBuildContext.class);

	private String owner;
	private String projectName;
	private String version;
	private String mavenRootPath;
	private String projectsRootPath;
	
	private LocalDateTime startLogTime;
	private Path logFilePath;
	
	public AppBuildContext(String projectsRootPath, 
			String mavenRootPath, 
			String projectName, 
			String version) {
		Assert.hasLength(projectsRootPath, "存放项目的根路径不能为空");
		Assert.hasLength(mavenRootPath, "maven 仓库的根路径不能为空");
		Assert.hasLength(projectName, "项目名不能为空");
		Assert.hasLength(version, "项目版本号不能为空");
		
		this.projectsRootPath = projectsRootPath;
		this.mavenRootPath = mavenRootPath;
		this.projectName = projectName;
		this.version = version;
	}
	
	public AppBuildContext(String projectsRootPath, 
			String mavenRootPath, 
			String owner,
			String projectName, 
			String version) {
		this(projectsRootPath, mavenRootPath, projectName, version);

		Assert.hasLength(owner, "项目拥有者的登录名不能为空");

		this.owner = owner;
	}

	private Path getProjectRootDirectory() {
		return Paths.get(this.projectsRootPath, "projects", this.projectName);
	}
	
	public Path getClientProjectRootDirectory() {
		return this.getProjectRootDirectory().resolve("client");
	}

	public Path getServerProjectRootDirectory() {
		return this.getProjectRootDirectory().resolve("server");
	}

	public Path getMavenInstallJar() {
		return Paths.get(this.mavenRootPath, 
				"repository", 
				"com", 
				"blocklang", 
				this.projectName, 
				this.version,
				this.projectName + "-" + this.version + ".jar");
	}

	private Path getLogDirectory() {
		return this.getProjectRootDirectory().resolve("logs");
	}
	
	private String getLogFileName() {
		if(startLogTime == null) {
			startLogTime = LocalDateTime.now();
		}
		return this.projectName + "-" + this.version + "-" + startLogTime.format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")) + ".log";
	}
	
	public Path getLogFilePath() throws IOException {
		if(logFilePath == null) {
			Path logFileDir = this.getLogDirectory();
			String logFileName = this.getLogFileName();
			Files.createDirectories(logFileDir);
			
			logFilePath = logFileDir.resolve(logFileName);
			if(Files.notExists(logFilePath)) {
				Files.createFile(logFilePath);
			}
		}
		
		return logFilePath;
	}

	public Path getDojoDistDirectory() {
		return this.getClientProjectRootDirectory().resolve("output").resolve("dist");
	}

	private Path getSpringBootMainResourcesDirectory() {
		return this.getServerProjectRootDirectory().resolve("src").resolve("main").resolve("resources");
	}
	
	public Path getSpringBootTemplatesDirectory() {
		return getSpringBootMainResourcesDirectory().resolve("templates");
	}
	
	public Path getSpringBootStaticDirectory() {
		return getSpringBootMainResourcesDirectory().resolve("static");
	}
	
	public String getIndexFileName() {
		return "index.html";
	}

	public Path getGitRepositoryDirectory() {
		return Paths.get(this.projectsRootPath, "gitRepo", this.owner, this.projectName);
	}

	public String getTagName() {
		if(this.version.startsWith("v")) {
			return this.version;
		}
		return "v" + this.version;
	}
	
	public void info(String pattern, Object... arguments) {
		String message = null;
		if(arguments.length == 0) {
			message = pattern;
		} else {
			message = MessageFormat.format(pattern, arguments);
		}
		try {
			Files.writeString(getLogFilePath(), "[INFO] " + message + System.lineSeparator(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			logger.error("not found log file", e);
		}
	}
	
	public void log(String pattern, Object... arguments) {
		String message = null;
		if(arguments.length == 0) {
			message = pattern;
		} else {
			message = MessageFormat.format(pattern, arguments);
		}
		try {
			Files.writeString(getLogFilePath(), message + System.lineSeparator(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			logger.error("not found log file", e);
		}
	}

	public void error(Throwable e) {
		try {
			Files.writeString(getLogFilePath(), e.getMessage(), StandardOpenOption.APPEND);
		} catch (IOException e1) {
			logger.error("not found log file", e);
		}
	}
	
	public void error(String pattern, Object... arguments) {
		String message = null;
		if(arguments.length == 0) {
			message = pattern;
		} else {
			message = MessageFormat.format(pattern, arguments);
		}
		try {
			Files.writeString(getLogFilePath(), "[ERROR] " + message + System.lineSeparator(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			logger.error("not found log file", e);
		}
	}
}