package com.spirit.DMRE.camunda;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class Transport implements JavaDelegate {
	ResultToFileWriter r = new ResultToFileWriter();
	@Override
	public void execute(DelegateExecution execution) throws Exception {
		r.writeResultTofile("Starting Transport-phase");

	}

}
