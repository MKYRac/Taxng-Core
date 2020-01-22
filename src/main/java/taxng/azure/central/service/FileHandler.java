package taxng.azure.central.service;

public interface FileHandler {

	boolean isXMLFormat(String XMLMessage);

	String getDataFromXMLFormat(String XMLMessage, String tag);

	String generateXMLFormat(String inputMessage, double taxResult);

}