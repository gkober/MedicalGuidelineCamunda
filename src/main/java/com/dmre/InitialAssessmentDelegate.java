package com.spirit.DMRE.camunda;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.jayway.jsonpath.JsonPath;



public class InitialAssessmentDelegate implements JavaDelegate {

	private final Logger log = Logger.getLogger(InitialAssessmentDelegate.class.getName());
	ResultToFileWriter r = new ResultToFileWriter();
	public InitialAssessmentDelegate() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		System.out.println("InitialAssessmentDelegate - going to check for the given codes");
		r.writeResultTofile("InitialAssessment");
		System.out.println("Here in the Delegate-class");
		/** for testing purposes
		execution.setVariable("Name", "free");
		execution.setVariable("speaksFine", "yes");
		execution.setVariable("psychomotor_activity", "confused");
		execution.setVariable("accessoryMusclesUsed", "yes");
		execution.setVariable("cyanosis", "yes");
		System.out.println(execution.getVariable("speaksFine").toString());
		**/
		
		
		//get list of codes to evaluate 
		Map<String,String> codesToCheck = (java.util.TreeMap<String,String>) execution.getVariable("checkingCodes");
		for(String key:codesToCheck.keySet()) {
			String code = codesToCheck.get(key);
			System.out.println("Hashmap-key_value: " + key +" " + code);
			System.out.println("Going to check for the code in the FHIR-RDF: " + code);
//			String sparqlQuery ="PREFIX fhir:<http://hl7.org/fhir/> SELECT ?value WHERE {" + 
//					" ?root fhir:Bundle.entry ?resource." + 
//					" ?resource fhir:Bundle.entry.resource/fhir:Observation.code/fhir:CodeableConcept.coding/fhir:Coding.code/fhir:value ?code." + 
//					" ?value ^fhir:value/^fhir:Observation.valueBoolean/^fhir:Bundle.entry.resource ?resource." + 
//					"    FILTER(?code="+ code +")." + 
//					"}";
			String sparqlQuery ="PREFIX fhir:<http://hl7.org/fhir/> SELECT ?value WHERE {"
					+ "?root fhir:Bundle.entry ?resource. ?resource fhir:Bundle.entry.resource/fhir:Observation.code/fhir:CodeableConcept.coding/fhir:Coding.code/fhir:value ?code." 
					+ "?value ^fhir:value/^fhir:Quantity.value/^fhir:Observation.valueQuantity/^fhir:Bundle.entry.resource ?resource."
					+ "?resource fhir:Bundle.entry.resource/fhir:Observation.effectiveDateTime/fhir:value ?date."
					+ " FILTER(?code=\""+code+"\")."
					+ "}order by desc(?date) limit 1";
			sparqlQuery = URLEncoder.encode(sparqlQuery,"UTF-8").replace("+", "%20");
			String sparqlURL="http://localhost:8080/DMRE-TRUNK-SNAPSHOT/DMSE/SPARQL/SPARQL"+"?query="+sparqlQuery;
			System.out.println("sending the message to: " + sparqlURL);
			r.writeResultTofile("sending the message to: " + sparqlURL);
			//r.writeResultTofile("sparqlQuery: " + sparqlQuery);
			URL url = new URL(sparqlURL);
			HttpURLConnection httpConnection = (HttpURLConnection)url.openConnection();
			httpConnection.setRequestMethod("GET");
			int responseCode = httpConnection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) { // success
				System.out.println("getting the HTTP-GET-Request SPARQL-Query to: " + sparqlURL);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						httpConnection.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				System.out.println("here the result from the SPARQL-Query:");
				System.out.println(response.toString());
				System.out.println("now trying to parse the json-result");
				if(this.parseSPARQLResult(response.toString()) != null) {
					// print result
					String setCodeValue = this.parseSPARQLResult(response.toString());
					r.writeResultTofile("SPARQL-QueryResponse: " + setCodeValue);
					System.out.println("SPARQL-QueryResponse: " + setCodeValue);
					
					//Setting codes ...
					r.writeResultTofile("Seetting executionVariables: Code: " + code + " variable: " + setCodeValue);
					execution.setVariable(key, setCodeValue);
				}else {
					String setCodeValue = this.alternativeSparqlQuery(code);
					if(!setCodeValue.isEmpty()) {
						System.out.println("AlternativeSparqlQuery executed and code is " + setCodeValue);
						r.writeResultTofile("AlternativeSparqlQuery executed and code is " + setCodeValue);
						execution.setVariable(key, setCodeValue);
					}else {
						System.out.println("even alternativeSparqlQuery did not deliver anything");
						r.writeResultTofile("even alternativeSparqlQuery did not deliver anything");
						r.writeResultTofile("Seetting executionVariables: Code: " + code + " variable: gets value NOTHING per default");
						execution.setVariable(key, "NOTHING");
					}
				}
			} else {
				System.out.println("GET request not worked");
				System.out.println(httpConnection.getResponseMessage());
				r.writeResultTofile("Seetting executionVariables: Code: " + code + " variable: gets value NOTHING per default");
				execution.setVariable(code, "NOTHING");
			}
		}
		

		System.out.println(
				"\n\n ... InitialAssesmentDelegate invoked by "
				+ "\n processDefinitionID: " + execution.getProcessDefinitionId()
				+ "\n activityID: " + execution.getCurrentActivityId()
				+ "\n activityName: " + execution.getCurrentActivityName()
				+ "\n processInstanceID: " + execution.getProcessInstanceId()
				+ "\n businessKey: " + execution.getBusinessKey()
				+ "\n execution: " + execution.getId()
				+ "\n\n"
				);
	}
	private String parseSPARQLResult(String response) {
        //JSONObject json = new JSONObject(response);
        String value = JsonPath.parse(response.toString()).read("$.results..['bindings']..*..['value'].['value']").toString();
		if(value.length()>=4) {
			String cutValue = value.substring(2, value.length()-2);
			return cutValue;
		}else
			return null;
	}
	/**
	 * ?resource fhir:Observation.valueCodeableConcept/fhir:CodeableConcept.coding/fhir:Coding.code/fhir:value ?value.
       ?code ^fhir:value/^fhir:Coding.code/^fhir:CodeableConcept.coding/^ fhir:Observation.code ?resource.
	 * @param code
	 * @return
	 * @throws IOException 
	 */
	private String alternativeSparqlQuery(String code) throws IOException {
		System.out.println("executing alternativeSparqlQuery with code " + code);
		String sparqlQuery ="PREFIX fhir:<http://hl7.org/fhir/> SELECT ?value WHERE {"
				+ "?root fhir:Bundle.entry ?resource. ?resource fhir:Bundle.entry.resource/fhir:Observation.valueCodeableConcept/fhir:CodeableConcept.coding/fhir:Coding.code/fhir:value ?value." 
				+ "?code ^fhir:value/^fhir:Coding.code/^fhir:CodeableConcept.coding/^ fhir:Observation.code/^fhir:Bundle.entry.resource ?resource."
				+ "?resource fhir:Bundle.entry.resource/fhir:Observation.effectivePeriod/fhir:Period.start/fhir:value ?date."
				+ " FILTER(?code=\""+code+"\")."
				+ "} order by desc(?date) limit 1";
		sparqlQuery = URLEncoder.encode(sparqlQuery,"UTF-8").replace("+", "%20");
		String sparqlURL="http://localhost:8080/DMRE-TRUNK-SNAPSHOT/DMSE/SPARQL/SPARQL"+"?query="+sparqlQuery;
		r.writeResultTofile("sending the message to: " + sparqlURL);
		URL url = new URL(sparqlURL);
		HttpURLConnection httpConnection = (HttpURLConnection)url.openConnection();
		httpConnection.setRequestMethod("GET");
		int responseCode = httpConnection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			System.out.println("getting the HTTP-GET-Request SPARQL-Query to: " + sparqlURL);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					httpConnection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			System.out.println("here the result from the alternative SPARQL-Query:");
			System.out.println(response.toString());
			System.out.println("now trying to parse the json-result");
			if(this.parseSPARQLResult(response.toString()) != null) {
				// print result
				String setCodeValue = this.parseSPARQLResult(response.toString());
				r.writeResultTofile("Alternative SPARQL-QueryResponse: " + setCodeValue);
				System.out.println(setCodeValue);
				//Setting codes ...
				r.writeResultTofile("Seetting Alternative executionVariables: Code: " + code + " variable: " + setCodeValue);
				return setCodeValue;
			}else {
				r.writeResultTofile("Seetting Alternative executionVariables: Code: " + code + " variable: gets value NOTHING_2 per default");
				return "NOTHING_2";
			}
		} else {
			System.out.println("GET request not worked");
			System.out.println(httpConnection.getResponseMessage());
			r.writeResultTofile("Seetting executionVariables: Code: " + code + " variable: gets value NOTHING_2 per default");
			return "NOTHING_2";
		}
	}

}
