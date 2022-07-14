package com.spirit.DMRE.FHIR;

import java.time.LocalDateTime;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Substance;
import org.hl7.fhir.r4.model.MedicationAdministration.MedicationAdministrationDosageComponent;

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

public class MedicationAdministrationResourceProvider implements IResourceProvider {
	private Map<Long, Deque<MedicationAdministration>> myIdToMedicationAdministrationVersions = new HashMap<Long, Deque<MedicationAdministration>>();
	private long myNextMedicationAdministrationId = 1;

	public MedicationAdministrationResourceProvider() {
		long resourceId = myNextMedicationAdministrationId++;
		Substance ingredient = new Substance();
		ingredient.getCode().addCoding().setSystem("http://hl7.org/fhir/sid/ndc").setCode("0409-6531-02").setDisplay("Testmedication");
		ingredient.setDescription("myTestIngredient");
		Medication med = new Medication();
		med.setCode(ingredient.getCode());
		med.addContained(ingredient);
		MedicationAdministration medAdmin1 = new MedicationAdministration();
		Period period = new Period();
		period.setStartElement(new DateTimeType(LocalDateTime.now().toString()));
		medAdmin1.setId(Long.toString(resourceId));
		medAdmin1.setEffective(period);
		medAdmin1.setMedication(new Reference(med));
		medAdmin1.setSubject(new Reference().setReference("Patient/00002"));
		medAdmin1.setDosage(new MedicationAdministrationDosageComponent()
				.setDose(new Quantity().setValue(500).setUnit("mg").setSystem("http://unitsofmeasure.org").setCode("mg"))
				.setText("500mg irgendwas")
				.setMethod(new CodeableConcept().setText("IV push")));
		
		LinkedList<MedicationAdministration> list = new LinkedList<>();
	    list.add(medAdmin1);
	    myIdToMedicationAdministrationVersions.put(resourceId, list);
	}
	
	@Override
	public Class<MedicationAdministration> getResourceType() {
		return MedicationAdministration.class;
	}
	@Search
	public List<MedicationAdministration> findBySubject(@RequiredParam(name=MedicationAdministration.SP_SUBJECT,chainWhitelist = {"",Patient.SP_IDENTIFIER,
			Patient.SP_BIRTHDATE}) ReferenceParam subject){
		System.out.println("now in searching the MedicationAdministration for a given identifier ... ");
		System.out.println(subject.toString());
		LinkedList<MedicationAdministration> retVal = new LinkedList<MedicationAdministration>();
		
		Iterator<Deque<MedicationAdministration>> iteratorVals = myIdToMedicationAdministrationVersions.values().iterator();
		while(iteratorVals.hasNext()){
			List medAdmins = (List) iteratorVals.next();
			for(int i=0; i<medAdmins.size();i++) {
				MedicationAdministration ma = (MedicationAdministration) medAdmins.get(i);
				System.out.println(ma.getSubject().getReference());
				if(ma.getSubject().getReference().equals(subject.getValue())) {
					retVal.add(ma);
				}
			}
		}
		
		return retVal;
	
	}
	@Search()
	public List<MedicationAdministration> findObservationsUsingArbitraryCtriteria() {
	      LinkedList<MedicationAdministration> retVal = new LinkedList<MedicationAdministration>();

	      for (Deque<MedicationAdministration> nextMedicationAdministrationList : myIdToMedicationAdministrationVersions.values()) {
	    	  MedicationAdministration nextMedicationAdministration = nextMedicationAdministrationList.getLast();
	         retVal.add(nextMedicationAdministration);
	      }
	      return retVal;
	}
	@Create
	public MethodOutcome creatMedicationAdministration(@ResourceParam MedicationAdministration theMedicationAdministration) {
		long id = myNextMedicationAdministrationId++;
		addNewVersion(theMedicationAdministration, id);
		return new MethodOutcome(new IdType(id));
	}
	
	@Read(version = true)
	public MedicationAdministration readObservation(@IdParam IdType theId) {
		Deque<MedicationAdministration> retVal;
		try {
			retVal = myIdToMedicationAdministrationVersions.get(theId.getIdPartAsLong());
		}catch (Exception e) {
			throw new ResourceNotFoundException(theId);
		}
		if (theId.hasVersionIdPart() == false) {
	         return retVal.getLast();
	      } else {
	    	  for (MedicationAdministration nextVersion : retVal) {
		            String nextVersionId = nextVersion.getIdElement().getVersionIdPart();
		            if (theId.getVersionIdPart().equals(nextVersionId)) {
		               return nextVersion;
		            }
		         }
		         // No matching version
		         throw new ResourceNotFoundException("Unknown version: " + theId.getValue());
	      }		
	}
	
	private void addNewVersion(MedicationAdministration theMedicationAdministration, Long theId) {
	      if (!myIdToMedicationAdministrationVersions.containsKey(theId)) {
	         myIdToMedicationAdministrationVersions.put(theId, new LinkedList<>());
	      }

	      theMedicationAdministration.getMeta().setLastUpdatedElement(InstantType.withCurrentTime());

	      Deque<MedicationAdministration> existingVersions = myIdToMedicationAdministrationVersions.get(theId);

	      // We just use the current number of versions as the next version number
	      String newVersion = Integer.toString(existingVersions.size());

	      // Create an ID with the new version and assign it back to the resource
	      IdType newId = new IdType("MedicationAdministration", Long.toString(theId), newVersion);
	      theMedicationAdministration.setId(newId);

	      existingVersions.add(theMedicationAdministration);
	   }


}
