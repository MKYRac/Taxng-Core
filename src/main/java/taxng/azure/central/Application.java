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
import com.microsoft.azure.storage.file.CloudFile;
import com.microsoft.azure.storage.file.CloudFileClient;
import com.microsoft.azure.storage.file.CloudFileDirectory;
import com.microsoft.azure.storage.file.CloudFileShare;
import com.microsoft.azure.storage.file.ListFileItem;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;

import taxng.azure.central.service.ApplicationController;
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
	private ApplicationController appController;
	
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
		
		String inputMsgStr = "";
		
		while(true) {
			
			try {
				
				// Get storage account details and store them in CloudStorageAccount object
				CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
				
				CloudFileClient fileClient = storageAccount.createCloudFileClient();
				CloudFileShare share = fileClient.getShareReference("taxngfileshare");
				CloudFileDirectory rootDir = share.getRootDirectoryReference();

				for ( ListFileItem fileItem : rootDir.listFilesAndDirectories() ) {
				    CloudFile file = (CloudFile) fileItem;
				    inputMsgStr = file.downloadText();
				    appController.execute(inputMsgStr, 1);
				    file.delete();
				}
				
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

					// Get message from queue front
					CloudQueueMessage inputMsg = inputQueue.retrieveMessage();
					
					// Store message content in string variable
					inputMsgStr = inputMsg.getMessageContentAsString();
					
					appController.execute(inputMsgStr, 2);
					
					inputQueue.deleteMessage(inputMsg);
				
				}
				
				else {
					
					log.info("No messages currently available.");
					Thread.sleep(30000);
				
				}
					
			} catch (Exception e) {
				log.error(e.toString());
			}
			
		}	
	}
	
}
