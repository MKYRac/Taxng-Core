package taxng.azure.central.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileHandlerImpl implements FileHandler {

	private static final Logger log = LoggerFactory.getLogger(FileHandlerImpl.class);
	
	@Override
	public boolean isXMLFormat(String XMLMessage) {
		if(XMLMessage.substring(0, 5).equals("<xml>") && XMLMessage.substring(XMLMessage.length()-6, XMLMessage.length()).equals("</xml>")) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public String getDataFromXMLFormat(String XMLMessage, String tag) {
		String result = "";
		int startIndex = 0;
		int endIndex = 0;
		String startTag = "<" + tag + ">";
		String endTag = "</" + tag + ">";
		for(int i = 0; i <= XMLMessage.length()-endTag.length(); i++) {
			if(startTag.equals(XMLMessage.substring(i, i+startTag.length()))) {
				startIndex = i+startTag.length();
			}
			else if(endTag.equals(XMLMessage.substring(i, i+endTag.length()))) {
				endIndex = i;
			}
		}
		if(startIndex == 0 && endIndex == 0) {
			log.error("Tag not found.");
		}
		result = XMLMessage.substring(startIndex, endIndex);		
		log.info("StartTag: " + startTag + ", EndTag: " + endTag + ", Value: " + result);
		return result;
	}
	
	@Override
	public String generateXMLFormat(String inputMessage, double taxResult) {
		String startTag = "<taxAmount>";
		String endTag = "</taxAmount>";
		String firstPartOfXML = inputMessage.substring(0, inputMessage.length()-6);
		String secondPartOfXML = inputMessage.substring(inputMessage.length()-6, inputMessage.length());
		String result = firstPartOfXML + startTag + taxResult + endTag + secondPartOfXML;
		return result;
	}
	
}
