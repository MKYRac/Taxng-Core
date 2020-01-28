package taxng.azure.central.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

import taxng.azure.central.Application;

@Component
public class ApplicationControllerImpl implements ApplicationController {

	@Value("${output.queue}")
	private String outputQueueName;
	
	@Value("${error.queue}")
	private String errorQueueName;
	
	@Autowired
	private FileHandler fileHandler;
	
	@Autowired
	private TaxMessageService tmsgService;
	
	@Autowired
	private DatabaseService dbService;
	
	@Value("${account.name}")
	private String accountName;
	
	@Value("${protocol}")
	private String protocol;
	
	@Value("${account.key}")
	private String accountKey;
	
	private static final Logger log = LoggerFactory.getLogger(Application.class);
	
	@Override
	public void execute(String XMLInputMessage, int selection) {
	
		try {
			log.info("<<< LOGGING STARTS HERE >>>");
			
			if(selection == 1) {
				
				log.info("Processing File From Storage");
				
			}
			
			else if (selection == 2) {
				
				log.info("Processing File From Input Queue");
				
			}
			
			log.info("Received Message: -- " + XMLInputMessage);
			log.info("<<< Message gets processed >>>");
			
			/*
			 *  Check if "<xml>"-tags are in message
			 *  If yes continue processing message
			 *  If no log error and put message into error queue
			 *  (see else on line 142)
			 */
			
			if(fileHandler.isXMLFormat(XMLInputMessage)) {
				
				// Extract information from xml message string variable
				int txId = Integer.parseInt(fileHandler.getDataFromXMLFormat(XMLInputMessage, "txId"));
				double bPrice = Double.parseDouble(fileHandler.getDataFromXMLFormat(XMLInputMessage, "bPrice"));
				double sPrice = Double.parseDouble(fileHandler.getDataFromXMLFormat(XMLInputMessage, "sPrice"));
				int nos = Integer.parseInt(fileHandler.getDataFromXMLFormat(XMLInputMessage, "nos"));
				double taxRate = Double.parseDouble(fileHandler.getDataFromXMLFormat(XMLInputMessage, "taxRate"));
				
				// Call tax message service implementation and calculate tax amount
				double result = tmsgService.taxAmount(bPrice, sPrice, nos, taxRate);
				
				log.info("Message processed -- Tax amount is: " + String.valueOf(result));
				
				// Call database service to store values in database
				dbService.saveDataToDB(txId, bPrice, sPrice, nos, taxRate, result);
				
				// Compose result string variable (initial XML message with added tax amount)
				String strResult = fileHandler.generateXMLFormat(XMLInputMessage, result);
				
				sendToOutputQueue(strResult, outputQueueName);
				
			}
			
			else {
				
				log.info("Message is not in XML format.");
				sendToOutputQueue(XMLInputMessage, errorQueueName);
			
			}
			
		} catch(Exception e) {
			log.error(e.toString());
		}
		
		
	}
	
	@Override
	public void sendToOutputQueue(String strMessage, String queueName) {

		
		final String storageConnectionString =
			    "DefaultEndpointsProtocol=" + protocol + ";" +
			    "AccountName=" + accountName + ";" +
			    "AccountKey=" + accountKey;
		
		try {
			
			CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
			
			CloudQueueClient queueClient = storageAccount.createCloudQueueClient();
				
			// Get queue and store it in CloudQueue object
			CloudQueue queue = queueClient.getQueueReference(queueName);
				
			// Create message as CloudQueueMessage object
			CloudQueueMessage message = new CloudQueueMessage(strMessage);
				
			// Add message to queue
			queue.addMessage(message);
			
			log.info("<<< Processed Message >>> " + message.getMessageContentAsString());
			log.info("Message processed to queue " + queueName);
				
			log.info("<<< LOGGING ENDS HERE >>>");
		
		} catch(Exception e) {
			log.error(e.toString());
		}
		
	}
	
}
