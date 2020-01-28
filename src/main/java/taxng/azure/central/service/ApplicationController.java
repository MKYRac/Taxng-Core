package taxng.azure.central.service;

public interface ApplicationController {

	void execute(String XMLInputMessage, int selection);
	
	void sendToOutputQueue(String strMessage, String queueName);

}