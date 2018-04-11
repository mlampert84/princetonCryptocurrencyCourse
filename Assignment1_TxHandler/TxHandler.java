import java.util.ArrayList;
import java.security.PublicKey;

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
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {

    	double inputTotal = 0;
    	double outputTotal = 0;
    	
//    	Make list of UTXOs from Transaction Inputs so as to test if inputs, i.e. claimed outputs, are in the UTXOPool
//    	Along the way, test that no input is used twice as a test of condition (3) listed above
    	ArrayList<UTXO> claimedOutputs = new ArrayList<UTXO>();
        for(Transaction.Input i : tx.getInputs()) {
//         System.out.println("Adding transaction at index " + i.outputIndex);
          UTXO nextUTXO = new UTXO(i.prevTxHash,i.outputIndex);
           if(claimedOutputs.contains(nextUTXO)) {
        	System.out.println("UTXO is claimed multiple times");
        	   return false;
           }
           claimedOutputs.add(nextUTXO);
        }
 
//     Return false if output claimed by {@code tx} is not in the current UTXO pool
        for(UTXO i : claimedOutputs) {
        	if(!currentPool.contains(i)) {
        	System.out.println("Output is not in the current UTXO Pool.");
        		return false;
        	
        	}
        }
         
//        Check the signatures on each input of {@code tx}
        ArrayList<Transaction.Input> txInputs = tx.getInputs();
        for(Transaction.Input i : txInputs) {
        	byte[] sigToTest = i.signature;
        	PublicKey keytoTest = currentPool.getTxOutput(new UTXO(i.prevTxHash, i.outputIndex)).address;
//           System.out.println("Checking input with index " + i.outputIndex);	
        	if(!Crypto.verifySignature(keytoTest, tx.getRawDataToSign(i.outputIndex),sigToTest)) {
              System.out.println("Signature didn't verify");	
        		return false;
                
        		
        	}
        }
    	
       	ArrayList<UTXO> theUTXOs = currentPool.getAllUTXO();
        for(UTXO ut : theUTXOs) {
        	inputTotal += currentPool.getTxOutput(ut).value;
        }
//        System.out.println("Input total: " + inputTotal);
    	
        ArrayList<Transaction.Output> txs = tx.getOutputs();
    	for(Transaction.Output o : txs) {
    		if(o.value < 0) return false;
    		outputTotal += o.value;
    	}

    	if(outputTotal > inputTotal) return false;
    	
    	return true;
    }
    
    //includes the input data as well as the output transaction from which it came.
    private class FullInputData{
      public UTXO utxo;
      public Transaction.Output output;
      
      public FullInputData(UTXO ut, Transaction.Output o) {
    	  utxo = ut;
    	  output = o;
      }
    }
    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        
    	ArrayList<Transaction> acceptedTxs = new ArrayList<Transaction>();
    	for(Transaction tx : possibleTxs) {
          	if(isValidTx(tx)) {
          		
          		acceptedTxs.add(tx);
//          	   System.out.println("The transaction is valid.  Proceeding...");
          	   ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
          	   ArrayList<Transaction.Input> txInputs = tx.getInputs();

               for(Transaction.Output o : txOutputs) {
	            	   do { 
//	            		   System.out.println("Testing output with value " + o.value);
	            		   Transaction.Input i = txInputs.get(0);
	            		   UTXO currentUTXO = new UTXO(i.prevTxHash,i.outputIndex);
	            		   Transaction.Output sourceOutput = currentPool.getTxOutput(currentUTXO);
//	            		   System.out.println("Comparing to input with value " + sourceOutput.value);
			          	   if(o.value < sourceOutput.value) {
			          		   sourceOutput.value -= o.value;
			          		   o.value = 0;
			          		   currentPool.removeUTXO(currentUTXO); 
			          		   currentPool.addUTXO(currentUTXO, sourceOutput);
//			          		   System.out.println("New input value: " + currentPool.getTxOutput(currentUTXO).value);
			          	   }
			          	   else{
			          	      o.value -= sourceOutput.value;
			          	      currentPool.removeUTXO(currentUTXO);
			          	      txInputs.remove(0);
			//          	      System.out.println("Removing input with value: " + sourceOutput.value + ". Output has value " + o.value);
			          	   
			          	   	}
	            	   }while(o.value > 0);
          	  
          	   	             }
          	}
      	
    	}
        
    	return acceptedTxs.toArray(new Transaction[acceptedTxs.size()]);
    }

    public static void main(String[] args) {
 
    
    }
    
}
