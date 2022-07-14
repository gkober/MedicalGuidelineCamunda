package com.spirit.DMRE.FHIR;

import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
/**
 * 
 * @author gerhard
 * This class is the ResfulServer, that is the initiating endpoint; and adds resources to the Server ... 
 * The resources then, are capable of storing/retrieving the created resources
 */
public class FHIR_DMRERestfulServlet extends RestfulServer {

	public FHIR_DMRERestfulServlet() {
		super(FhirContext.forR4());
		
		
	}
	@Override
	public void initialize() {
		System.out.println("Initializing the FHIR DMRERestfulServlet");
		List<IResourceProvider> providers = new ArrayList<IResourceProvider>();
		providers.add(new PatientResourceProvider());
		providers.add(new ObservationResourceProvider());
		providers.add(new MedicationAdministrationResourceProvider());
		setResourceProviders(providers);
		
	}
}
