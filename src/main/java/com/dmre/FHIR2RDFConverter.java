package com.spirit.DMRE.camunda;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.hapi.ctx.FhirR4;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;

public class FHIR2RDFConverter implements JavaDelegate {

	FhirContext ctx = FhirContext.forR4();
	IGenericClient client = null;
	ResultToFileWriter r = new ResultToFileWriter();
	public FHIR2RDFConverter() {
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		System.out.println("\n\n\n Here in the execution of FHIR2RDFConverter \n\n\n");
		r.writeResultTofile("Starting the execution of FHIR2RDFConverter");
		List<String> availableFhirServers = new ArrayList<String>();
		List<String> FhirResourcesToGet = new ArrayList<String>();
		String patientID = (String) execution.getVariable("PatientID").toString();
		System.out.println(patientID);
		List<String> FHIRServers = (ArrayList) execution.getVariable("FHIRServers");
		List<String> FHIRResources = (ArrayList) execution.getVariable("FHIRResources");
		//Iterate over list
		for(int i = 0; i<FHIRServers.size();i++) {
			availableFhirServers.add(FHIRServers.get(i).toString());
			System.out.println(FHIRServers.get(i).toString());
		}
		for(int i = 0; i<FHIRResources.size();i++) {
			FhirResourcesToGet.add(FHIRResources.get(i).toString());
			System.out.println(FHIRResources.get(i).toString());
		}
		//try to connect to the FHIR-Servers
		//print out avaliable FHIR-Servers (configured in Camunda)
		for(String url : availableFhirServers) {
			System.out.println("potential servers to connect to: " + url);
			r.writeResultTofile("potential servers to connect to: " + url);
		}
		//printing out fhirResources to check for
		for(String resourceName : FhirResourcesToGet) {
			System.out.println("Potential resources to check for in the servers: " + resourceName);
		}
		//getAll available information from FHIR-Servers
		for(String url : availableFhirServers) {
			System.out.println("Connecting to URL: " + url);
			this.client = this.ctx.newRestfulGenericClient(url);
			for(String resourceName : FhirResourcesToGet) {
				System.out.println("Getting Resource " + resourceName + "from url " + url);
				try {
						Bundle b = this.client.search()
							.forResource(resourceName)
							.where(new ReferenceClientParam("subject").hasId("Patient/" + patientID))
							.returnBundle(Bundle.class)
							.execute();
						System.out.println("\n\n\nTotalSizeOfBundle: " + b.getTotal());
					
						if(b.getTotal()!= 0) {
							//convert and store to local RDF-Store, but only if Total is not 0
							r.writeResultTofile("BundleSize for Resource: " + url + "|" + resourceName + " is: " + b.getTotal());
							this.createOWLData(b);
						}else {
							r.writeResultTofile("BundleSize for Resource: " + url + "|" + resourceName + " is: " + b.getTotal());
						}
					}
					catch(ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException e) {
						r.writeResultTofile("ResourceNotFound: "+ e.getMessage() + " " + resourceName + "from url " + url);
					}
					catch (Exception e) {
						e.printStackTrace();
						continue;
					}
			}
		}
	}
	
	private void connectToFhirServer(String url) {
		this.client = this.ctx.newRestfulGenericClient(url);
		
	}
	private Bundle searchForObservations(String patientID) throws IOException {
		Bundle observationBundle = null;
		List<IBaseResource> observations = new ArrayList<>();
		
		try {
			observationBundle = this.client.search()
					.forResource("Observation")
					.where(new ReferenceClientParam("subject").hasId("Patient/" + patientID))
					.returnBundle(Bundle.class)
					.execute();
			
		}catch(ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException resourceNotFoundException) {
			System.out.println("resourceNotFoundException happened");
			r.writeResultTofile("ResoruceNotFoundException" + resourceNotFoundException.getResponseBody().toString());
		}
		
		System.out.println("ObservationBundle-size: " + observationBundle.getTotal());
		return observationBundle;
	}
	public void createOWLData(Bundle observationBundle) throws IOException {
		System.out.println("Converting and persisting RDF-Representation");
		r.writeResultTofile("creatingOWLData");
		String serialized = this.ctx.newRDFParser().encodeResourceToString(observationBundle);
		System.out.println(serialized);
		
		//temporarily write to disk...
		FileOutputStream outputStream = null;
		outputStream = new FileOutputStream("rdf_representation"+"_.rdf");
		outputStream.write(serialized.getBytes(Charset.forName("UTF-8")));
		outputStream.close();
		
		//pimp the TTL with the FHIR.ttl import
		org.apache.jena.rdf.model.Model model=ModelFactory.createDefaultModel();
		OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		Ontology ont = ontModel.createOntology("http://www.guideline.org/ABox");
	
		ontModel.read(new FileInputStream("rdf_representation"+"_.rdf"),null,"TTL");
		ont.addImport(ontModel.getResource("http://hl7.org/fhir/fhir.ttl"));
		OutputStream modelOut = new FileOutputStream("ttl_representation"+"_.ttl");
		RDFDataMgr.write(modelOut, ontModel, Lang.TTL);
	 
	    //transforming from TTL to RDF-XML
	    InputStream in = new FileInputStream("ttl_representation"+"_.ttl");
	    org.apache.jena.rdf.model.Model modelTransform = ModelFactory.createDefaultModel(); // creates an in-memory Jena Model
	    modelTransform.read(in, null, "TURTLE"); // parses an InputStream assuming RDF in Turtle format
	    OutputStream modelOutRDFXML;
		modelOutRDFXML = new FileOutputStream("rdf_representation"+"_.rdf.xml");
		modelTransform.write(modelOutRDFXML, "RDF/XML");
		System.out.println("trying to call the sendOWLDataToRDFStore-method");
		try {
			this.sendOWLDataToRDFStore();
		}catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("createOWLData completed");
		r.writeResultTofile("createOWLData completed");
		
	}
	private void sendOWLDataToRDFStore() throws IOException {
			System.out.println("now trying to send the OWL to the RDF-Store ...");
			r.writeResultTofile("trying to send the OWL to the RDF-store");
			URL url = new URL("http://localhost:8080/DMRE-TRUNK-SNAPSHOT/DMSE/SPARQL/FeedRDF");
			HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setRequestMethod("POST");
			httpConnection.setDoOutput(true);
			httpConnection.setRequestProperty("Content-Type", "application/json");
			
			
			//read file to byteArray
			byte[] dataToSend = Files.readAllBytes(Paths.get("rdf_representation_.rdf.xml"));
			//String data = "something";
			//byte[] out = data.getBytes(StandardCharsets.UTF_8);
			OutputStream stream = httpConnection.getOutputStream();
			stream.write(dataToSend);
			
			System.out.println("httpConnection-ResponseCode: " + httpConnection.getResponseCode());
			r.writeResultTofile("OWLDataToRDF response-code: " + httpConnection.getResponseCode() + httpConnection.getResponseMessage());
//			System.out.println(httpConnection.getResponseMessage());
			httpConnection.getResponseMessage();
			httpConnection.disconnect();
			System.out.println("Sent RDF to Store");	
		
	}
	
	public static void main (String[] args) throws IOException {
		FHIR2RDFConverter a = new FHIR2RDFConverter();
		//a.connectToFhirServer("http://hapi.fhir.org/baseR4");
		a.connectToFhirServer("http://wildfhir4.aegis.net/fhir4-0-1");
		//Bundle b = a.searchForObservations("123cef8f-c44b-4b9a-97f4-4c871870854a");
		//Bundle b = a.searchForObservations("2789998"); //Hapi Fhir r4-server
		Bundle b = a.searchForObservations("23"); //aegis-server
		a.createOWLData(b);
		//a.createOWLData(b);
		//a.clearTmpRDFStore();
		
	}
	
	

}
