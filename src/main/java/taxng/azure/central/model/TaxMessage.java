package taxng.azure.central.model;

// This class represents the attributes of an XML Tax Message

public class TaxMessage {

	private double buyPrice;
	private double sellPrice;
	private int numberShares;
	private double taxRate;
	
	public double getBuyPrice() {
		return buyPrice;
	}
	public void setBuyPrice(double buyPrice) {
		this.buyPrice = buyPrice;
	}
	public double getSellPrice() {
		return sellPrice;
	}
	public void setSellPrice(double sellPrice) {
		this.sellPrice = sellPrice;
	}
	public int getNumberShares() {
		return numberShares;
	}
	public void setNumberShares(int numberShares) {
		this.numberShares = numberShares;
	}
	public double getTaxRate() {
		return taxRate;
	}
	public void setTaxRate(double taxRate) {
		this.taxRate = taxRate;
	}
	
}
