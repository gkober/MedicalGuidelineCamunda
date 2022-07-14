package com.spirit.DMRE.camunda;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Map.Entry;

import org.apache.jena.rdf.model.ModelFactory;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.hl7.fhir.r4.formats.IParser;
import org.hl7.fhir.r4.model.BaseDateTimeType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.MedicationAdministration.MedicationAdministrationDosageComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Substance;
import org.hl7.fhir.r4.model.Type;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;

public class ApplyDrugs implements JavaDelegate {

	FhirContext ctx = FhirContext.forR4();
	ResultToFileWriter r = new ResultToFileWriter();
	//IGenericClient client = ctx.newRestfulGenericClient("http://wildfhir4.aegis.net/fhir4-0-1");
	//IGenericClient client = ctx.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
	IGenericClient client = ctx.newRestfulGenericClient("http://localhost:8080/DMRE-TRUNK-SNAPSHOT/fhir/");
	Medication med = null;
	MedicationAdministration medAdministration = null;
	@Override
	public void execute(DelegateExecution execution) throws Exception {
		r.writeResultTofile("starting ApplyDrugs-Task");
		System.out.println("apply drugs and create new observation");
		//Add a new medicationResource to the fhir-endpoint
		this.client.setEncoding(EncodingEnum.JSON);
		String patientID = (String) execution.getVariable("patientID");
		String medication = (String) execution.getVariable("medication");
		try {
			this.generateMedicationAdministration(patientID, medication);
		}catch(Exception e) {
			r.writeResultTofile("An error happened in generating the medicationAdministration submission");
			e.printStackTrace();
		}
		try {
			if(execution.getVariable("readyForTransport").equals("no")) {
				execution.setVariable("readyForTransport", "yes");
				r.writeResultTofile("set executionVariable: readyForTransport + yes");
			}else {
				execution.setVariable("readyForTransport", "no");
				r.writeResultTofile("set executionVariable: readyForTransport + no");
			}
		}catch(Exception e) {
			execution.setVariable("readyForTransport", "no");
			r.writeResultTofile("set executionVariable: readyForTransport + no");
		}
		
		System.out.println("trying to print the executionVariables ...");
		for(Entry<String, Object> entry : execution.getVariables().entrySet()) {
			System.out.println(entry.getKey() + " " + entry.getValue().toString());
		}
	}
	public void generateMedication() {
		Substance ingredient = new Substance();
		ingredient.getCode().addCoding().setSystem("http://hl7.org/fhir/sid/ndc").setCode("0409-6531-02").setDisplay("Testmedication");
		ingredient.setDescription("myTestIngredient");
		this.med = new Medication();
		this.med.setCode(ingredient.getCode());
		this.med.addContained(ingredient);
			
	}
	public String generateMedicationAdministration(String patientID, String medication) {
		System.out.println("ApplyDrugs.java -> generateMedicationAdministration");
		System.out.println("PatientID: " + patientID);
		System.out.println("medication: " + medication);
		Patient patient = new Patient();
		patient.setId(patientID);
		patient.addIdentifier().setValue(patientID);
		
		//search for patient with ID:patientID
//		Bundle fhirPatient = (Bundle) client.search().forResource(Patient.class)
//				.where(new TokenClientParam("identifier").exactly().code(patientID))
//				.prettyPrint()
//				.execute();
//		Patient pat = (Patient) fhirPatient.getEntryFirstRep().getResource();
//		String familyName = pat.getNameFirstRep().getFamily();
//		String givenName = pat.getNameFirstRep().getNameAsSingleString();
//		String patID = pat.getIdElement().getIdPart();
//		
//		patient.setId(patID);
//		patient.addIdentifier().setValue(patID);
//		patient.addName().setFamily(familyName).addGiven(givenName);
//		
		//System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient));
		/*MethodOutcome out1 = this.client.create()
				.resource(patient).prettyPrint().encodedJson()
				.execute();
		System.out.println("PatientOUTID: " + out1.getId());*/
		
		this.generateMedication();
		this.medAdministration = new MedicationAdministration();
		Period period = new Period();
		period.setStartElement(new DateTimeType(LocalDateTime.now().toString()));
		this.medAdministration.setSubject(new Reference().setReference("Patient/"+patientID));
		this.medAdministration.setEffective(period);
		this.medAdministration.setMedication(new Reference(this.med));
		this.medAdministration.setSubject(new Reference(patient).setDisplay(patient.getNameFirstRep().getFamily()));
		this.medAdministration.setDosage(new MedicationAdministrationDosageComponent()
				.setDose(new Quantity().setValue(500).setUnit("mg").setSystem("http://unitsofmeasure.org").setCode("mg"))
				.setText("500mg irgendwas")
				.setMethod(new CodeableConcept().setText("IV push")));
		
		
		System.out.println(this.ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(medAdministration));
		
		MethodOutcome out2 = this.client.create().resource(medAdministration).execute();
		System.out.println(out2.getId().toString()+"?_format=application/json");
		return out2.getId().toString();
	}
	public static void main(String[] args) throws IOException {
		ApplyDrugs ad = new ApplyDrugs();
		//System.out.println(ad.generateMedicationAdministration("4711","Aspirin"));
		
		//search for the bundle and convert to RDF ...
		
		Bundle b = ad.client.search().forResource(MedicationAdministration.class)
				//.where(Patient.FAMILY.matches().value("huber")).and(Patient.GIVEN.matches().value("jockl2"))
				.where(MedicationAdministration.PATIENT.hasChainedProperty(Patient.FAMILY.matches().value("Monti"))                 )
				.and(MedicationAdministration.PATIENT.hasChainedProperty(Patient.GIVEN.matches().value("Tom")))
				.returnBundle(Bundle.class)
				.execute();
		System.out.println(b.getTotal());
		System.out.println("\n\n\n\n========================\n");
		String p = ad.ctx.newRDFParser().encodeResourceToString(b);
		FileOutputStream outputStream = null;
		outputStream = new FileOutputStream("rdf_representation1"+"_.rdf");
		outputStream.write(p.getBytes(Charset.forName("UTF-8")));
		outputStream.close();
		
		 //transforming from TTL to RDF-XML
	    InputStream in = new FileInputStream("rdf_representation1"+"_.rdf");
	    org.apache.jena.rdf.model.Model modelTransform = ModelFactory.createDefaultModel(); // creates an in-memory Jena Model
	    modelTransform.read(in, null, "TURTLE"); // parses an InputStream assuming RDF in Turtle format
	    OutputStream modelOutRDFXML;
		modelOutRDFXML = new FileOutputStream("rdf_representation1"+"_.rdf.xml");
		modelTransform.write(modelOutRDFXML, "RDF/XML");
		System.out.println(p);
	}
}
