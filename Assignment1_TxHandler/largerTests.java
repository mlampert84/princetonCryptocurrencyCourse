import java.util.Arrays;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;



public class largerTests {

	public static void main(String[] args) 
			throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		UtxoTestSet utxoTestSet = UtxoTestSet.builder()
				.setPeopleSize(10)
				.setUtxoTxNumber(10)
				.setMaxUtxoTxOutput(10)
				.setMaxValue(200)
				.setTxPerTest(10)
				.setMaxInput(10)
				.setMaxOutput(10)
				.setNegativeOutputs(true)  // create transactions with negative values
				.setCorruptedPercentage(.10) // probability of 20% of invalid transactions
				.build();

		
		ValidationLists<Transaction> trxsValidation = utxoTestSet.getValidationLists();
		
		MaxFeeTxHandler txHandler = new MaxFeeTxHandler(utxoTestSet.getUtxoPool());
		Transaction [] transactions = trxsValidation.allElements().toArray(new Transaction[trxsValidation.allElements().size()]); 
		Transaction [] successTxs = txHandler.handleTxs(transactions);

}
}