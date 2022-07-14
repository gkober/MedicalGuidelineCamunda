package com.spirit.DMRE.camunda;

import java.math.BigDecimal;
import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;

public class NewObservationGenerator implements JavaDelegate {
	FhirContext ctx = FhirContext.forR4();
	//IGenericClient client = ctx.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
	IGenericClient client = ctx.newRestfulGenericClient("http://localhost:8080/DMRE-TRUNK-SNAPSHOT/fhir/");
	ResultToFileWriter r = new ResultToFileWriter();
	@Override
	public void execute(DelegateExecution execution) throws Exception {
		System.out.println("Capturing new observations");
		r.writeResultTofile("Capturing new observations for the entire process - only for showcase");
		Map<String,String> FhirContentList = (java.util.TreeMap<String,String>) execution.getVariable("FhirContentList");
		for(String key:FhirContentList.keySet()) {
			String code = FhirContentList.get(key);
			System.out.println("FhirContentList_Hashmap-key_value: " + key +" " + code);
		}
		System.out.println((String) execution.getVariable("patientID").toString());
		this.generateNewObservation(FhirContentList.get("code"), FhirContentList.get("value") ,FhirContentList.get("unit"), "null", (String) execution.getVariable("patientID").toString());
	}
	public Patient generateNewPatient(String patientID, String familyName, String givenName, String gender) {
		Patient patient = new Patient();
		patient.addIdentifier().setSystem("http://acme.org/mrns").setValue(patientID);
		patient.addName().setFamily(familyName).addGiven(givenName);
		if(gender.equalsIgnoreCase("Male")) {
			patient.setGender(Enumerations.AdministrativeGender.MALE);
		}else if (gender.equalsIgnoreCase("Female")) {
			patient.setGender(Enumerations.AdministrativeGender.FEMALE);
		}else {
			patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
		}
		patient.setId(patientID);
		return patient;
	}
	public void generateNewObservation(String code, String value, String unit, String system, String patientID) {
		//Patient localPid = this.searchPatient(patientID);
		Observation observation = new Observation();
		observation.setStatus(Observation.ObservationStatus.FINAL);
		observation.getCode().addCoding().setSystem(system).setCode(code);
		boolean isBigdecimal;
		try {
			BigDecimal b = new BigDecimal(value);
			isBigdecimal = true;
		}catch(Exception e) {
			isBigdecimal = false;
		}
		if(!isBigdecimal) {
		observation.setValue(new CodeableConcept().addCoding(new Coding().setCode(value).setSystem("http://unitsofmeasure.org")));
		} else {//value = number
			observation.setValue(new Quantity().setValue(new BigDecimal(value)).setUnit(unit).setSystem("http://unitsofmeasure.org"));
		}
		//System.out.println(localPid.getIdElement().getValue());
		observation.setSubject(new Reference().setReference("Patient/"+patientID));
		observation.setEffective(new DateTimeType().now());
		MethodOutcome responseObservation = this.client.create().resource(observation).execute();
		System.out.println(this.ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(observation));
	}
	public Patient searchPatient(String patientID) {
		Bundle fhirPatient = (Bundle) client.search().forResource(Patient.class)
				.where(new TokenClientParam("identifier").exactly().systemAndCode("http://acme.org/mrns", patientID))
				.prettyPrint()
				.execute();
		Patient pat = (Patient) fhirPatient.getEntryFirstRep().getResource();
		System.out.println(this.ctx.newJsonParser().encodeResourceToString(pat));
		String familyName = pat.getNameFirstRep().getFamily();
		String givenName = pat.getNameFirstRep().getNameAsSingleString();
		String patID = pat.getIdElement().getIdPart();
		System.out.println("PatientID to Use: " + patID);
		return pat;
	}
	public static void main(String[] args) {
		NewObservationGenerator n = new NewObservationGenerator();
		Patient p = n.generateNewPatient("98765", "Maier", "Thomas", "Male");
		//MethodOutcome resp = n.client.create().resource(p).execute();
		//System.out.println(n.ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(resp.getResource()));
		String pid = n.searchPatient("98765").getIdElement().getIdPart();
		System.out.println(pid);
		n.generateNewObservation("46679-7", "120", "bpm", "http://loinc.org", "98765");
		
	}

}
