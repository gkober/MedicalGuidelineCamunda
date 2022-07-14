package com.spirit.DMRE.camunda;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Properties;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

/** this fhir data preparer constructs, based on given input files the needed observations
 * 
 * @author gerhard
 *
 */
public class FHIRDataPreparer {
	
	/**
	 * 
	 * @param fileName
	 * @throws IOException
	 * #0 = Type (Observation)
	 * #1 = Notes
	 * #2 = valueQuantity
	 * #3 = unit
	 * #4 = system
	 * #5 = code
	 * #6 = subjectReference
	 * #7 = codeSystem
	 * #8 = codeCode
	 * #9 = codeDisplay
	 */

	FhirContext ctx = FhirContext.forR4();
	IGenericClient client = ctx.newRestfulGenericClient("http://localhost:8080/DMRE-TRUNK-SNAPSHOT/fhir/");
	
	public void readInputFile(String fileName) throws IOException {
		String file = new java.io.File(".").getCanonicalPath()+"/configuration/FHIRDataPreparer/"+fileName;
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
	    String currentLine;
	    
	    while ((currentLine = reader.readLine())!= null) {
	    	 //System.out.println(currentLine);
	    	 if(!currentLine.startsWith("#")) {
	    		 System.out.println("Reading line: " + currentLine);
	    		 String[] observationData = currentLine.split(";");
	    		 createObservation(observationData[2],observationData[3],observationData[4],
	    				 observationData[5],observationData[6],observationData[7],
	    				 observationData[8],observationData[9]);
		    	 for(int i = 0;i<observationData.length;i++) {
		    		 //System.out.println(observationData[i]);
		    	 }
	    	 }	 
	    }
	    reader.close();
	}
	public Observation createObservation(String valueQuantity, String unit, String system, 
										String code, String subjectReference, String codeSystem, 
										String codeCode, String codeDisplay) throws IOException {
		Observation observation = new Observation();
		observation.setStatus(Observation.ObservationStatus.FINAL);
		observation.getCode().addCoding().setSystem(system).setCode(code).setDisplay(codeDisplay);
		try { //changes to bigDecimal
			BigDecimal vq = new BigDecimal(valueQuantity);
			observation.setValue(new Quantity().setValue(new BigDecimal(valueQuantity)).setUnit(unit).setSystem(system).setCode(code));
		}catch(Exception e) { //uses string values
			observation.setValue(new CodeableConcept().addCoding(new Coding().setCode(valueQuantity).setSystem(codeSystem)));
		}
		observation.setSubject(new Reference().setReference(subjectReference));
		observation.getEffectivePeriod().setStartElement(new DateTimeType().now());
		observation.addPerformer(new Reference().setReference("Practitioner/007").setDisplay("DataPreparer"));
		
		submitResourceToFHIRStore(observation);
		return observation;
	}
	public MethodOutcome submitResourceToFHIRStore(Observation observation) throws IOException {
		String server = getTmpFhirStore();
		this.ctx.newRestfulGenericClient(server);
		this.ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		String out = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(observation);
		System.out.println(out);
		MethodOutcome responseObservation = this.client.create().resource(observation).execute();
		return responseObservation;
	}
	public String getTmpFhirStore() throws IOException {
		Properties properties= new Properties();
		String file = new java.io.File(".").getCanonicalPath()+"/configuration/DMRE.properties";
		FileInputStream fileInput;
		try {
			fileInput = new FileInputStream(file);
			properties.load(fileInput);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//loading the properties to variables
		String serverName =  properties.getProperty("temporaryFHIRStore");
		System.out.println("ServerName from properties: "+ serverName);
		return serverName;
	}
	public static void main(String[] args) throws IOException {
		FHIRDataPreparer f = new FHIRDataPreparer();
		f.readInputFile("dataFile_run1.txt");
	}
}
