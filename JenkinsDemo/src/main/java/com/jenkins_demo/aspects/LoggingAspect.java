package com.jenkins_demo.aspects;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

	
	 @Before("execution(* com.jenkins_demo.services.*.*(..))")
	 public void logMethodCall(JoinPoint joinPoint) {

	        System.out.println(
	                "Method Called : "
	                + joinPoint.getSignature().getName());

	        System.out.println(
	                "Arguments : "
	                + Arrays.toString(joinPoint.getArgs()));
	    }
	 
	 @After("execution(* com.jenkins_demo.services.*.*(..))")
	    public void afterAdvice(JoinPoint joinPoint) {

	        System.out.println(
	                "After : "
	                + joinPoint.getSignature().getName());
	    }
	 
	 @AfterReturning(
		        pointcut = "execution(* com.jenkins_demo.services.*.*(..))",
		        returning = "result")
		public void afterReturningAdvice(
		        JoinPoint joinPoint,
		        Object result) {

		    System.out.println(
		            "Method Returned : "
		            + result);
		}
	 
	 @AfterThrowing(
		        pointcut = "execution(* com.jenkins_demo.services.*.*(..))",
		        throwing = "exception")
		public void afterThrowingAdvice(
		        JoinPoint joinPoint,
		        Exception exception) {

		    System.out.println(
		            "Exception Occurred : "
		            + exception.getMessage());
		}
	 
	 @Around("execution(* com.jenkins_demo.services.*.*(..))")
	 public Object aroundAdvice(ProceedingJoinPoint joinPoint)
	         throws Throwable {

	     long startTime = System.currentTimeMillis();

	     Object result = joinPoint.proceed();

	     long endTime = System.currentTimeMillis();

	     System.out.println(
	             joinPoint.getSignature().getName()
	             + " executed in "
	             + (endTime - startTime)
	             + " ms");

	     return result;
	 }
	 
	 
}
