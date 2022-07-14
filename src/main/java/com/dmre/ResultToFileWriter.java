package com.spirit.DMRE.camunda;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Properties;

public class ResultToFileWriter {
	private boolean keepCamundaResultFileContent = false;
	
	private String getResultFileNameFromConfiguration() {
		Properties properties= new Properties();
		String file = null;
		try {
			//System.out.println(new java.io.File(".").getCanonicalPath().toString());
			file = new java.io.File(".").getCanonicalPath()+"/configuration/DMRE.properties";
		} catch (IOException e2) {
			e2.printStackTrace();
		}
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
		String fileName =  properties.getProperty("CamundaResultFile");
		this.keepCamundaResultFileContent = Boolean.parseBoolean(properties.getProperty("keepCamundaResultFileContent"));
		System.out.println("Found the following Filename as resultFile: " + fileName);
		return fileName;	
	}

	public boolean writeResultTofile(String result) throws IOException {
		String fileName = this.getResultFileNameFromConfiguration();
		//Append timestamp
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        System.out.println(timestamp);
        
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(timestamp);
        stringBuilder.append(" - ");
        stringBuilder.append(result);
        stringBuilder.append("\n");
        String finalString = stringBuilder.toString();
		FileOutputStream outputStream = new FileOutputStream(fileName,true);
		//result = result + "\n";
	    byte[] strToBytes = finalString.getBytes();
	    outputStream.write(strToBytes);
	    outputStream.flush();
	    outputStream.close();  
		return true;
	}
	public String getResultFromFile() throws FileNotFoundException, IOException {
		String fileName = this.getResultFileNameFromConfiguration();
		try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    String everything = sb.toString();
		    //emty file before returning 
		    if(this.keepCamundaResultFileContent == false) {
		    	 PrintWriter pw = new PrintWriter(fileName);
				 pw.close();
		    }
		    return everything;
		}catch(Exception e) {
			return e.getMessage().toString();
		}
	}
	
	public static void main(String[]args) throws IOException {
		ResultToFileWriter r = new ResultToFileWriter();
		r.writeResultTofile("Test1");
		r.writeResultTofile("Test2");
		r.writeResultTofile("Test3");
		System.out.println(r.getResultFromFile());
	}
}
