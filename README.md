# MedicalGuidelineCamunda

This project is part of a bigger system. The entire system running in a distributed environment, that contains FHIR (Fast Healthcare Interoperability Resources) servers, RDF stores, medical execution environments. This project now covers the additional code to extend the functionality of Camunda (http://camunda.com) for retrieving FHIR data and allow conversion to RDF and subsequently provide SPARQL results to the camunda execution engine. 

The BPMN for describing the medical guideline can be created using the camunda modeller, and then be used in the code. For our purpose, we created a simple web interface to start the execution process, and after execution provide the logs about what happened. Following a screenshot of the web interface.
<img width="1163" alt="Bildschirmfoto 2022-07-14 um 21 29 21" src="https://user-images.githubusercontent.com/48680286/179067646-e5be2d87-9d1b-437e-919c-e4883ddade3e.png">

In this project the code for a simple in-memory FHIR-store is available (that allows storing, querying and receving the following resources:Patient, Observation, MedicationAdministration). This feature happened due the lack of availability of public FHIR test-servers. Of course the search implementation is limited to the needs to fulfill a query by patient id, since we grab all resources related to one patient, and we do not need more specialized queries, since our approach is to work with RDF-data, since it is more standard than FHIR ;)

So in the code you find:
1. Camunda extending Java code (to be called/defined in the BPMN)
2. FHIR-standalone server

additionally we provide a simple BPMN model, that is executable in Camunda.
