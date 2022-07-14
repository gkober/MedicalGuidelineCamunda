package com.spirit.DMRE.camunda;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class sendRestRequest implements JavaDelegate 
{
	private final Logger log = Logger.getLogger(sendRestRequest.class.getName());
	ResultToFileWriter r = new ResultToFileWriter();
	public sendRestRequest() {
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		//getting the input parameters
		r.writeResultTofile("checking the severity");
		String severity = (String) execution.getVariable("severity");
		r.writeResultTofile("Severity-status: " + severity);
		if(severity.equalsIgnoreCase("high")) {
			System.out.println("\n\n Severity: high");
		}
		if(severity.equalsIgnoreCase("moderate")) {
			System.out.println("\n\n Severity: moderate");
		}
		if(severity.equalsIgnoreCase("low")) {
			System.out.println("\n\n Severity: low");
		}
		System.out.println("\n\nSending the RestRequest to somewhere...\n\n\n");
		//here the restSendLogic ... 
		URL url = new URL("http://ip.jsontest.com/");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("accept", "application/json");
		byte[] out = "{\"username\":\"root\",\"password\":\"password\"}" .getBytes(StandardCharsets.UTF_8);

		connection.setDoOutput(true);
		try{
			DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
			dos.write(out);
		}catch(Exception e) {
			e.printStackTrace();
		}

		//reading the response from the server-side
		BufferedReader responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while((inputLine = responseReader.readLine())!=null) {
			content.append(inputLine);
		}
		System.out.println(content.toString());
		responseReader.close();
		connection.disconnect();
		r.writeResultTofile("sendRestRequest completed");

	}
}
