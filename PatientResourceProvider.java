package com.spirit.DMRE.FHIR;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class PatientResourceProvider implements IResourceProvider {
	private Map<Long, Deque<Patient>> myIdToPatientVersions = new HashMap<Long, Deque<Patient>>();
	private long myNextId = 1;
		
	/**
	* Constructor, which pre-populates the provider with one resource instance.
	*/
	public PatientResourceProvider() {
	      long resourceId = myNextId++;

	      Patient patient = new Patient();
	      patient.setId(Long.toString(resourceId));
	      patient.addIdentifier();
	      patient.getIdentifier().get(0).setSystem("urn:hapitest:mrns");
	      patient.getIdentifier().get(0).setValue("00002");
	      patient.addName().setFamily("Test");
	      patient.getName().get(0).addGiven("PatientOne");
	      patient.setGender(AdministrativeGender.FEMALE);

	      LinkedList<Patient> list = new LinkedList<>();
	      list.add(patient);


	      myIdToPatientVersions.put(resourceId, list);

	   }
	private void addNewVersion(Patient thePatient, Long theId) {
	      if (!myIdToPatientVersions.containsKey(theId)) {
	         myIdToPatientVersions.put(theId, new LinkedList<>());
	      }

	      thePatient.getMeta().setLastUpdatedElement(InstantType.withCurrentTime());

	      Deque<Patient> existingVersions = myIdToPatientVersions.get(theId);

	      // We just use the current number of versions as the next version number
	      String newVersion = Integer.toString(existingVersions.size());

	      // Create an ID with the new version and assign it back to the resource
	      IdType newId = new IdType("Patient", Long.toString(theId), newVersion);
	      thePatient.setId(newId);

	      existingVersions.add(thePatient);
	   }
		/**
	    * The "@Create" annotation indicates that this method implements "create=type", which adds a
	    * new instance of a resource to the server.
	    */
	   @Create()
	   public MethodOutcome createPatient(@ResourceParam Patient thePatient) {
	      validateResource(thePatient);

	      // Here we are just generating IDs sequentially
	      long id = myNextId++;

	      addNewVersion(thePatient, id);

	      // Let the caller know the ID of the newly created resource
	      return new MethodOutcome(new IdType(id));
	   }
	   @Search()
	   public List<Patient> findPatientsByName(@RequiredParam(name = Patient.SP_FAMILY) StringType theFamilyName) {
	      LinkedList<Patient> retVal = new LinkedList<Patient>();

	      /*
	       * Look for all patients matching the name
	       */
	      for (Deque<Patient> nextPatientList : myIdToPatientVersions.values()) {
	         Patient nextPatient = nextPatientList.getLast();
	         NAMELOOP:
	         for (HumanName nextName : nextPatient.getName()) {
	            String nextFamily = nextName.getFamily();
	            if (theFamilyName.equals(nextFamily)) {
	               retVal.add(nextPatient);
	               break NAMELOOP;
	            }
	         }
	      }

	      return retVal;
	   }

	   @Search
	   public List<Patient> findPatientsUsingArbitraryCtriteria() {
	      LinkedList<Patient> retVal = new LinkedList<Patient>();

	      for (Deque<Patient> nextPatientList : myIdToPatientVersions.values()) {
	         Patient nextPatient = nextPatientList.getLast();
	         retVal.add(nextPatient);
	      }

	      return retVal;
	   }


	   /**
	    * The getResourceType method comes from IResourceProvider, and must be overridden to indicate what type of resource this provider supplies.
	    */
	   @Override
	   public Class<Patient> getResourceType() {
	      return Patient.class;
	   }

	   /**
	    * This is the "read" operation. The "@Read" annotation indicates that this method supports the read and/or vread operation.
	    * <p>
	    * Read operations take a single parameter annotated with the {@link IdParam} paramater, and should return a single resource instance.
	    * </p>
	    *
	    * @param theId The read operation takes one parameter, which must be of type IdDt and must be annotated with the "@Read.IdParam" annotation.
	    * @return Returns a resource matching this identifier, or null if none exists.
	    */
	   @Read(version = true)
	   public Patient readPatient(@IdParam IdType theId) {
	      Deque<Patient> retVal;
	      try {
	         retVal = myIdToPatientVersions.get(theId.getIdPartAsLong());
	      } catch (NumberFormatException e) {
	         /*
	          * If we can't parse the ID as a long, it's not valid so this is an unknown resource
	          */
	         throw new ResourceNotFoundException(theId);
	      }

	      if (theId.hasVersionIdPart() == false) {
	         return retVal.getLast();
	      } else {
	         for (Patient nextVersion : retVal) {
	            String nextVersionId = nextVersion.getIdElement().getVersionIdPart();
	            if (theId.getVersionIdPart().equals(nextVersionId)) {
	               return nextVersion;
	            }
	         }
	         // No matching version
	         throw new ResourceNotFoundException("Unknown version: " + theId.getValue());
	      }

	   }

	   /**
	    * The "@Update" annotation indicates that this method supports replacing an existing
	    * resource (by ID) with a new instance of that resource.
	    *
	    * @param theId      This is the ID of the patient to update
	    * @param thePatient This is the actual resource to save
	    * @return This method returns a "MethodOutcome"
	    */
	   @Update()
	   public MethodOutcome updatePatient(@IdParam IdType theId, @ResourceParam Patient thePatient) {
	      validateResource(thePatient);

	      Long id;
	      try {
	         id = theId.getIdPartAsLong();
	      } catch (DataFormatException e) {
	         throw new InvalidRequestException("Invalid ID " + theId.getValue() + " - Must be numeric");
	      }

	      /*
	       * Throw an exception (HTTP 404) if the ID is not known
	       */
	      if (!myIdToPatientVersions.containsKey(id)) {
	         throw new ResourceNotFoundException(theId);
	      }

	      addNewVersion(thePatient, id);

	      return new MethodOutcome();
	   }
	   
	   private void validateResource(Patient thePatient) {
		      /*
		       * Our server will have a rule that patients must have a family name or we will reject them
		       */
	      if (thePatient.getNameFirstRep().getFamily().isEmpty()) {
	         OperationOutcome outcome = new OperationOutcome();
	         outcome.addIssue().setSeverity(IssueSeverity.FATAL).setDiagnostics("No family name provided, Patient resources must have at least one family name.");
	         throw new UnprocessableEntityException(FhirContext.forR4(), outcome);
	      }
	   }
}
