package taxng.azure.central;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

import taxng.azure.central.service.DatabaseService;
import taxng.azure.central.service.FileHandler;
import taxng.azure.central.service.TaxMessageService;

@SpringBootApplication
public class Application implements ApplicationRunner {
	
	@Value("${input.queue}")
	private String inputQueueName;
	
	@Value("${output.queue}")
	private String outputQueueName;
	
	@Value("${error.queue}")
	private String errorQueueName;
	
	@Value("${account.name}")
	private String accountName;
	
	@Value("${protocol}")
	private String protocol;
	
	@Value("${account.key}")
	private String accountKey;
	
	@Autowired
	private FileHandler fileHandler;
	
	@Autowired
	private TaxMessageService tmsgService;
	
	@Autowired
	private DatabaseService dbService;
	
	private static final Logger log = LoggerFactory.getLogger(Application.class);

	
    public static void main( String[] args ) {
        SpringApplication.run(Application.class, args);
    }

	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		/*   
		 *  Compose string variable for connecting to Azure storage account
		 *  Details are retrieved from application.properties file under src/main/resources
		 */
		final String storageConnectionString =
			    "DefaultEndpointsProtocol=" + protocol + ";" +
			    "AccountName=" + accountName + ";" +
			    "AccountKey=" + accountKey;
		
		while(true) {
			
			try {
				
				// Get storage account details and store them in CloudStorageAccount object
				CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
				
				// Create CloudQueuClient object
				CloudQueueClient queueClient = storageAccount.createCloudQueueClient();
				
				// Get input queue and store it in CloudQueue object
				CloudQueue inputQueue = queueClient.getQueueReference(inputQueueName);
				
				// Get current number of messages waiting in queue to get processed
				inputQueue.downloadAttributes();
				long msgCount = inputQueue.getApproximateMessageCount();
				
				/*
				 *  If message(s) is/are available execute following code
				 *  If no messages available, pause thread for 30 seconds and try again 
				 *  (see else on line 162)
				 */
				
				if(msgCount > 0) {
					
					log.info("<<< LOGGING STARTS HERE >>>");
					
					// Get message from queue front
					CloudQueueMessage inputMsg = inputQueue.retrieveMessage();
					
					// Store message content in string variable
					String inputMsgStr = inputMsg.getMessageContentAsString();
					
					log.info("Received Message: -- " + inputMsgStr);
					log.info("<<< Message gets processed >>>");
					
					/*
					 *  Check if "<xml>"-tags are in message
					 *  If yes continue processing message
					 *  If no log error and put message into error queue
					 *  (see else on line 142)
					 */
					if(fileHandler.isXMLFormat(inputMsgStr)) {
						
						// Extract information from xml message string variable
						int txId = Integer.parseInt(fileHandler.getDataFromXMLFormat(inputMsgStr, "txId"));
						double bPrice = Double.parseDouble(fileHandler.getDataFromXMLFormat(inputMsgStr, "bPrice"));
						double sPrice = Double.parseDouble(fileHandler.getDataFromXMLFormat(inputMsgStr, "sPrice"));
						int nos = Integer.parseInt(fileHandler.getDataFromXMLFormat(inputMsgStr, "nos"));
						double taxRate = Double.parseDouble(fileHandler.getDataFromXMLFormat(inputMsgStr, "taxRate"));
						
						// Call tax message service implementation and calculate tax amount
						double result = tmsgService.taxAmount(bPrice, sPrice, nos, taxRate);
						
						log.info("Message processed -- Tax amount is: " + String.valueOf(result));
						
						// Call database service to store values in database
						dbService.saveDataToDB(txId, bPrice, sPrice, nos, taxRate, result);
						
						// Compose result string variable (initial XML message with added tax amount)
						String strResult = fileHandler.generateXMLFormat(inputMsgStr, result);
						
						// Get output queue and store it in CloudQueue object
						CloudQueue outputQueue = queueClient.getQueueReference(outputQueueName);
						
						// Create output message as CloudQueueMessage object
						CloudQueueMessage outputMsg = new CloudQueueMessage(strResult);
						
						// Add output message to output queue
						outputQueue.addMessage(outputMsg);
						
						log.info("<<< Processed Message >>> " + outputMsg.getMessageContentAsString());
						log.info("Message processed to queue " + outputQueueName);
					
					} else {
						
						log.info("Message is not in XML format.");
						log.info("Message processed to queue " + errorQueueName);
						
						// Get error queue and store it in CloudQueue object
						CloudQueue errorQueue = queueClient.getQueueReference(errorQueueName);
						
						// Create error message as CloudQueueMessage object
						CloudQueueMessage errorMsg = new CloudQueueMessage(inputMsgStr);
						
						// Add error message to error queue
						errorQueue.addMessage(errorMsg);
					}
					
					// Remove initial message from input queue
					inputQueue.deleteMessage(inputMsg);
					
					log.info("<<< LOGGING ENDS HERE >>>");
				
				} else {
					
					log.info("No messages currently available.");
					Thread.sleep(30000);
				
				}
				
			} catch (Exception e) {
				log.error(e.toString());
			}
			
		}	
	}
	
}
