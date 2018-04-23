import java.util.ArrayList;
import java.util.Arrays;

public class TxHandler {

    private UTXOPool currentPool;

	/**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        currentPool = new UTXOPool(utxoPool);
    }


    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
    	
    	double inputTotal = 0;
    	double outputTotal = 0;
    	ArrayList<UTXO> claimedOutputs = new ArrayList<UTXO>();   	

    	for(int i = 0; i < tx.numInputs(); i++) {
    	    Transaction.Input input = tx.getInput(i);
    	    UTXO currentUTXO = new UTXO(input.prevTxHash,input.outputIndex);
            if(claimedOutputs.contains(currentUTXO)) {
        	   return false;
            }
          	if(!currentPool.contains(currentUTXO)) {
            		return false;
           }
          Transaction.Output outputIn = currentPool.getTxOutput(currentUTXO);
          if(!Crypto.verifySignature(outputIn.address, tx.getRawDataToSign(i),input.signature)) {
        		return false;
          }
          inputTotal += outputIn.value;  
          claimedOutputs.add(currentUTXO);
        }

    	for(Transaction.Output o : tx.getOutputs()) {
    		if(o.value < 0) return false;
    		outputTotal += o.value;
    	}
    	return (inputTotal >= outputTotal);
    }

    private void processTx(Transaction tx) {

  
       //Delete the inputs from the current UTXOPool
       for(Transaction.Input i : tx.getInputs()) {
    	   currentPool.removeUTXO(new UTXO(i.prevTxHash,i.outputIndex));
       }

       //Add the new output to the current UTXOPool
   	   ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
       for(int k = 0; k < txOutputs.size(); k++) {
  		  currentPool.addUTXO(new UTXO(tx.getHash(),k), txOutputs.get(k));
       }
       
    }

    
    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {


 	    	ArrayList<Transaction> remainingPosTxs = new ArrayList<Transaction>(Arrays.asList(possibleTxs));
 	        ArrayList<Transaction> acceptedTxs = new ArrayList<Transaction>();
 	       	Transaction txToProcess = null;

 	        do {
 	        	if(txToProcess != null) {
 	                processTx(txToProcess);
 	    			acceptedTxs.add(txToProcess);
 	    			txToProcess = null;
 	    		}

 	         	for(int i = 0; i < remainingPosTxs.size(); i++) {
 	         		Transaction tx = remainingPosTxs.get(i);
 	         		if(isValidTx(tx)) {
 	          		txToProcess = tx;
 	          		remainingPosTxs.remove(i);
 	          		break;
 	         		}

 	         	}
 	    	} while(txToProcess != null);



    	return acceptedTxs.toArray(new Transaction[acceptedTxs.size()]);
    }

    public static void main(String[] args) {


    }

}
