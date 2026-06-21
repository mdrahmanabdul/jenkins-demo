package com.jenkins_demo.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jenkins_demo.services.DemoService;

@RestController
public class AopTestController {

	private final DemoService demoService;
	
	public AopTestController(DemoService demoService) {
		this.demoService=demoService;
	}
	
	@GetMapping("/test-aop")
	public String saveUser() {
		return demoService.testAOP("Hello from TestAOP !!!");
	}
}
