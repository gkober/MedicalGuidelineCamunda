package com.spirit.DMRE.camunda;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class TimerWaitingExtension implements JavaDelegate {
	ResultToFileWriter r = new ResultToFileWriter();
	@Override
	public void execute(DelegateExecution execution) throws Exception {
		System.out.println("now in waiting for a next execution");
		r.writeResultTofile("now in waiting for a next execution");
		String millisToWait = execution.getVariable("millisToWait").toString();
		System.out.println(millisToWait);
		r.writeResultTofile("millisToWait: "+ millisToWait);
		Thread.sleep(Integer.parseInt(millisToWait));
		r.writeResultTofile("Proceeding after sleep");
		r.writeResultTofile("Clearing the TMP RDF store");
		this.clearTmpRDFStore();
	}
	public boolean clearTmpRDFStore() throws IOException {
		r.writeResultTofile("clearing the RDF-TMP-Store for avoiding duplicates");
		URL url = new URL("http://localhost:8080/DMRE-TRUNK-SNAPSHOT/DMSE/SPARQL/CleanRDF");
		HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
		httpConnection.setRequestMethod("POST");
		httpConnection.setRequestProperty("Content-Type", "application/json");
		r.writeResultTofile("CleanRDF-TMP-Store - ResponseMessage: " + httpConnection.getResponseMessage());
		httpConnection.disconnect();
		if(httpConnection.getResponseCode()==200) {
			System.out.println("Clean TmpRDF-Store done");
			return true;
		}else {
			System.out.println("Clean TmpRDF-Store NOT done - error happened");
			return false;
		}	
	}

}
