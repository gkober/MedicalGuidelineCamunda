package com.spirit.DMRE.camunda;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.camunda.bpm.BpmPlatform;
import org.camunda.bpm.application.AbstractProcessApplication;
import org.camunda.bpm.application.PostDeploy;
import org.camunda.bpm.application.ProcessApplication;
import org.camunda.bpm.application.ProcessApplicationReference;
import org.camunda.bpm.application.impl.ServletProcessApplication;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.CaseService;
import org.camunda.bpm.engine.DecisionService;
import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.FilterService;
import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

@ProcessApplication(name="MedicalGuidelineAppCodePart") //, deploymentDescriptors= {"configuration/camunda/processes.xml"})
public class MedicalGuidelineApplication extends ServletProcessApplication {

	public MedicalGuidelineApplication() {
		System.out.println("Initializing MedicaliGuidelineApplicantion Camunda");
		System.out.println("java-classpath: " + System.getProperty("java.class.path"));
		System.out.println("deploymentDescriptors: " + MedicalGuidelineApplication.class.getAnnotation(ProcessApplication.class).deploymentDescriptors()[0]);
		System.out.println("--------------");

		File f = new File("/Users/gerhard/Documents/Dissertation/apache-tomcat-9/bin/configuration/camunda/processes.xml");
		if(f.exists()) {
			System.out.println("processes.xml exists");
		}else {
			System.out.println("Something happened ....");
		}
		
	}
	@Override
	public void createDeployment(String processArchiveName, DeploymentBuilder deploymentBuilder) {
		System.out.println("createDeployment");
	    ProcessEngine processEngine = BpmPlatform.getProcessEngineService().getProcessEngine("default");
	    createDeployment(processArchiveName, processEngine, getProcessApplicationClassloader());
	}
	public void createDeployment(String processArchiveName, ProcessEngine processEngine, ClassLoader classLoader) {
	    if (processEngine != null) {
	      RepositoryService repositoryService = processEngine.getRepositoryService();
	      System.out.println("loading the diagram to the repositoryService");
	      if (!isProcessDeployed(repositoryService, "MedicalGuideline")) {
	        repositoryService.createDeployment(this.getReference())
	          .addInputStream("diagram_initialTryCamunda.bpmn", classLoader.getResourceAsStream("diagram_initialTryCamunda.bpmn"))
	          .deploy();
	      }
	    }
	 }
	 protected boolean isProcessDeployed(RepositoryService repositoryService, String key) {
		    return repositoryService.createProcessDefinitionQuery().processDefinitionKey("MedicalGuideline").count() > 0;
	 }

	
	@PostDeploy
	public void startProcess(ProcessEngine processEngine) {
		 System.out.println("Starting the Process");
		 System.out.println("let's try to start the process initially on startup");		 
		 System.out.println("ProcessEngine-Name: " + processEngine.getName().toString());
		 try {
			boolean r = new ResultToFileWriter().writeResultTofile("startProcess triggered");
		 } catch (IOException e1) {
			e1.printStackTrace();
		}
		 try {
			 System.out.println("For now - do NOT start the instance by startup");
			 //processEngine.getRuntimeService().startProcessInstanceByKey("initialTryCamundaProcess");
		 }catch(Exception e) {
			 System.out.println("Error happened during loading....");
			 System.out.println("trying again");
			 try {
				boolean r = new ResultToFileWriter().writeResultTofile("startProcess triggered, but an error happened");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			 try {
				 System.out.println("For now - do NOT start the instance by startup - the 2nd time");
				 //processEngine.getRuntimeService().startProcessInstanceByKey("initialTryCamundaProcess");
			 }catch (Exception er) {
				 System.out.println("Error happened second time");
			 }
		 }
		
	}

}
