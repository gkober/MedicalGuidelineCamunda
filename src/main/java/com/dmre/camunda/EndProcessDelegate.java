package com.spirit.DMRE.camunda;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class EndProcessDelegate implements JavaDelegate {
	ResultToFileWriter r = new ResultToFileWriter();
	public EndProcessDelegate() {

	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		System.out.println("\n\n ending the Process now");
		r.writeResultTofile("Ending Process now");
	}

}
