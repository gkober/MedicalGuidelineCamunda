package com.spirit.DMRE.camunda;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class startMedicalGuidelineProcess implements JavaDelegate {

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		System.out.println("startMedicalGuidelineProcess-Class");
		System.out.println("Starting the MedicalGuideLineProcess");
	
	}

}
