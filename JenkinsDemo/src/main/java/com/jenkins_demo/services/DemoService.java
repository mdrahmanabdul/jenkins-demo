package com.jenkins_demo.services;

import org.springframework.stereotype.Service;

@Service
public class DemoService {

	public String testAOP(String message) {
		System.out.println("Calling testAOP()");
		String result = "Message received : "+message;
		return result;
	}
}
