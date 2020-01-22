package taxng.azure.central.service;

public interface DatabaseService {

	void saveDataToDB(int txId, double bPrice, double sPrice, int nos, double taxRate, double result) throws Exception;

}