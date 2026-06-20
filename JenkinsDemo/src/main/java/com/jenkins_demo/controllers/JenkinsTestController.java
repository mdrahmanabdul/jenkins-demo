package com.jenkins_demo.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JenkinsTestController {

	@GetMapping("/")
	public String greetFromJenkins() {
		return "Greeting from Jenkins !!!";
	}
	
}
