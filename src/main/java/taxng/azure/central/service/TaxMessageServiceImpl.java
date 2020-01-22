package taxng.azure.central.service;

import org.springframework.stereotype.Service;

import taxng.azure.central.model.TaxMessage;

// This class calculates the tax amount for a specific tax message

@Service
public class TaxMessageServiceImpl implements TaxMessageService {

	// Method to calculate the tax amount for a specific tax message
	@Override
	public double taxAmount(double bPrice, double sPrice, int nos, double taxRate) {
		
		// Instantiate new TaxMessage object
		TaxMessage txmsg = new TaxMessage();
		
		// Call setters to set the values
		txmsg.setBuyPrice(bPrice);
		txmsg.setSellPrice(sPrice);
		txmsg.setNumberShares(nos);
		txmsg.setTaxRate(taxRate);
		
		int number = txmsg.getNumberShares();
		
		// Calculate earnings from trade
		final double earnings = (txmsg.getSellPrice() * number) - (txmsg.getBuyPrice() * number);
		
		
		// Return calculated tax amount only if earning are positive, otherwise return 0 for no taxes
		if(earnings > 0) {
			return (earnings * txmsg.getTaxRate() / 100);
		}
		else {
			return 0;
		}
	}
	
}
