package taxng.azure.central.service;

public interface TaxMessageService {

	double taxAmount(double bPrice, double sPrice, int nos, double taxRate);

}