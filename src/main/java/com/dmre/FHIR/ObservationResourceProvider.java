package com.spirit.DMRE.FHIR;

import java.math.BigDecimal;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

public class ObservationResourceProvider implements IResourceProvider {
	
	private Map<Long, Deque<Observation>> myIdToObservationVersions = new HashMap<Long, Deque<Observation>>();
	private long myNextObservationId = 1;
	
	@Override
	public Class<Observation> getResourceType() {
		return Observation.class;
	}
	/**
	 * Create initial Observation ...
	 */
	public ObservationResourceProvider () {
		long resourceId = myNextObservationId++;
		Observation observation = new Observation();
		observation.setId(Long.toString(resourceId));
		observation.setStatus(Observation.ObservationStatus.FINAL);
		observation
		   .getCode()
		      .addCoding()
		         .setSystem("http://loinc.org")
		         .setCode("789-8")
		         .setDisplay("Erythrocytes [#/volume] in Blood by Automated count");
		observation.setValue(
		   new Quantity()
		      .setValue(4.12)
		      .setUnit("10 trillion/L")
		      .setSystem("http://unitsofmeasure.org")
		      .setCode("10*12/L"));
		
		observation.setSubject(new Reference().setReference("Patient/00002"));
		observation.setEffective(new DateTimeType().now());
		LinkedList<Observation> list = new LinkedList<>();
	    list.add(observation);
	    myIdToObservationVersions.put(resourceId, list);
	}
	@Create()
	public MethodOutcome creatObservation(@ResourceParam Observation theObservation) {
		long id = myNextObservationId++;
		addNewVersion(theObservation, id);
		return new MethodOutcome(new IdType(id));
	}
	@Search()
	public List<Observation> findBySubject(@RequiredParam(name=Observation.SP_SUBJECT,chainWhitelist = {"",Patient.SP_IDENTIFIER,
			Patient.SP_BIRTHDATE}) ReferenceParam subject){
			System.out.println("now in searching the observations for a given identifier ... ");
			System.out.println(subject.toString());
			LinkedList<Observation> retVal = new LinkedList<Observation>();
			
			Iterator<Deque<Observation>> iteratorVals = myIdToObservationVersions.values().iterator();
			while(iteratorVals.hasNext()){
				List obs = (List) iteratorVals.next();
				for(int i=0; i<obs.size();i++) {
					Observation o = (Observation) obs.get(i);
					System.out.println(o.getSubject().getReference());
					if(o.getSubject().getReference().equals(subject.getValue())) {
						retVal.add(o);
					}
				}
			}
			
			return retVal;
		
	}
	
	@Search()
	public List<Observation> findObservationsUsingArbitraryCtriteria() {
	      LinkedList<Observation> retVal = new LinkedList<Observation>();

	      for (Deque<Observation> nextObservationList : myIdToObservationVersions.values()) {
	         Observation nextObservation = nextObservationList.getLast();
	         retVal.add(nextObservation);
	      }
	      return retVal;
	}
	
	@Read(version = true)
	public Observation readObservation(@IdParam IdType theId) {
		Deque<Observation> retVal;
		try {
			retVal = myIdToObservationVersions.get(theId.getIdPartAsLong());
		}catch (Exception e) {
			throw new ResourceNotFoundException(theId);
		}
		if (theId.hasVersionIdPart() == false) {
	         return retVal.getLast();
	      } else {
	    	  for (Observation nextVersion : retVal) {
		            String nextVersionId = nextVersion.getIdElement().getVersionIdPart();
		            if (theId.getVersionIdPart().equals(nextVersionId)) {
		               return nextVersion;
		            }
		         }
		         // No matching version
		         throw new ResourceNotFoundException("Unknown version: " + theId.getValue());
	      }		
	}
	private void addNewVersion(Observation theObservation, Long theId) {
	      if (!myIdToObservationVersions.containsKey(theId)) {
	         myIdToObservationVersions.put(theId, new LinkedList<>());
	      }

	      theObservation.getMeta().setLastUpdatedElement(InstantType.withCurrentTime());

	      Deque<Observation> existingVersions = myIdToObservationVersions.get(theId);

	      // We just use the current number of versions as the next version number
	      String newVersion = Integer.toString(existingVersions.size());

	      // Create an ID with the new version and assign it back to the resource
	      IdType newId = new IdType("Observation", Long.toString(theId), newVersion);
	      theObservation.setId(newId);

	      existingVersions.add(theObservation);
	   }

}
