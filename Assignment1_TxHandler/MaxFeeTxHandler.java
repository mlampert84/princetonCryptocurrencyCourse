import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class MaxFeeTxHandler {

	    private UTXOPool currentPool;
	    private double maxFee;
	    private Transaction [] acceptedTxsWithMaxFees;


	    /**
	     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
	     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
	     * constructor.
	     */
	    public MaxFeeTxHandler(UTXOPool utxoPool) {
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
	    
	    //Checks if a transaction doublespends
	    private boolean noDoubleSpends(Transaction tx) {
	    	ArrayList<UTXO> claimedOutputs = new ArrayList<UTXO>();   	
	    	for(int i = 0; i < tx.numInputs(); i++) {
	    	    Transaction.Input input = tx.getInput(i);
	    	    UTXO currentUTXO = new UTXO(input.prevTxHash,input.outputIndex);
	            if(claimedOutputs.contains(currentUTXO)) {
	        	   return false;
	            }
	            claimedOutputs.add(currentUTXO);
	    	}
	    	return true;
	    }
	    
	    private ArrayList<Integer> findDuplicate(ArrayList<Transaction> txs){
	    	ArrayList<Integer> conflictingTxs = new ArrayList<Integer>();
	    	HashSet<UTXO> allInputs = new HashSet<UTXO>();

	    	UTXO doubleInput = null;
	    	boolean doubleSpendFound = false;
	    	//Find an input that is double spent
	    	outerloop:
	    	for(int t = 0; t < txs.size(); t++) {
	    	  Transaction tx = txs.get(t);
	    	  for(Transaction.Input i : tx.getInputs()) {
	    		  doubleInput = new UTXO(i.prevTxHash,i.outputIndex);
	    		  if(!allInputs.add(doubleInput)){
	    			  doubleSpendFound = true;
	    			  break outerloop;	
	    		  }
	    	  }
	    	}
           //Get all the transactions that use that input
	      if( doubleSpendFound) {
	      	for(int t = 0; t < txs.size(); t++) {
		    	  Transaction tx = txs.get(t);
		    	  for(Transaction.Input i : tx.getInputs()) {
		    		  UTXO utxo = new UTXO(i.prevTxHash,i.outputIndex);
		    		  		if(doubleInput.equals(utxo)) {
		    		  			conflictingTxs.add(t);
		    		  		}
		    	  }
	      		}
	      	}
	      return conflictingTxs;
	    }
	    
	    private void splitTxs(ArrayList<Transaction> txs) {

	    	ArrayList<Integer> duplicates = findDuplicate(txs);
	    	if( duplicates.size()>0) {
	        for(int i : duplicates) {	        	
	        	ArrayList<Transaction> txsSubset = new ArrayList<Transaction>();
	        	txsSubset.add(txs.get(i));
	        	for(int j = 0; j < txs.size(); j++) {
	        		if(!duplicates.contains(j)) txsSubset.add(txs.get(j));
	        	}
	        	splitTxs(txsSubset);
	        }
	        
	    	}
	    	else {
	    		processTxs(txs);
	    	}
	    }
	    
	    private void processTxs(ArrayList<Transaction>txs) {

	   
	    	 UTXOPool resetPool = new UTXOPool(currentPool);
	    	 double totalFees = 0;
	    	 ArrayList<Transaction> acceptedTxs = new ArrayList<Transaction>();
	    	 Transaction txToProcess = null;

		        do {
		        	if(txToProcess != null) {
			    	    double totalInput = 0;
			    	    double totalOutput = 0;
			    	    
		        		//Delete the inputs from the current UTXOPool, tallying their value in the process
		    	        for(Transaction.Input i : txToProcess.getInputs()) {
		    	        	UTXO utxo = new UTXO(i.prevTxHash,i.outputIndex);
		    	        	totalInput += currentPool.getTxOutput(utxo).value;
		    	        	currentPool.removeUTXO(utxo);
		    	        }

		    	        //Add the new output to the current UTXOPool, tallying their value in the process
		    	   	   ArrayList<Transaction.Output> txOutputs = txToProcess.getOutputs();
		    	        for(int k = 0; k < txOutputs.size(); k++) {
		    	         totalOutput += txOutputs.get(k).value;
		    	   		  currentPool.addUTXO(new UTXO(txToProcess.getHash(),k), txOutputs.get(k));
		    	        }
		    	        
		    	        totalFees += (totalInput - totalOutput);
		        		acceptedTxs.add(txToProcess);
		    			txToProcess = null;
		    		}

		         	for(int i = 0; i < txs.size(); i++) {
		         		Transaction tx = txs.get(i);
		         		if(isValidTx(tx)) {
		          		txToProcess = tx;
		          		txs.remove(i);
		          		break;
		         		}
		         	}
		    	} while(txToProcess != null);

		        if(totalFees > maxFee) {
		        	maxFee = totalFees;
		        	acceptedTxsWithMaxFees = acceptedTxs.toArray(new Transaction[acceptedTxs.size()]);
		        }
		        currentPool = resetPool;
	     }
	    	    

	    /**
	     * Finds the set of txs that maximize fees by using the following strategy:
	     * 1) any txs are removed that double spend an input, for these will never be valid, and would
	     * interfere with the search for double spends in step 2. 
	     * 2) The array of txs is searched for any txs that try to spend the same input.  If such txs
	     * are found, then subsets of the transactions are made so that the double spend does not
	     * exist in each subset (for example, say for txs 1 through 5, there are three txs, 2,3,4, that
	     * all claim the same input.  Three subsets of txs are created: one with {1,2,5},{1,3,5},
	     * {1,4,5}.
	     * 3) Subsets are recursively put through Step 2 until no txs in the subset double-spend an input.
	     * 4) Once a subset exists where no transaction claims the same input, the subset of transactions is
	     * processed as in the non-extra credit version, simply looking for the next valid tx and processing it,
	     * and then repeating this until there are no more valid txs. The fee is calculated while the 
	     * txs are being processed, and if it exceeds the current max fee, that
	     * subset of processed transactions is set to be returned as the maximized set of accepted txs. 
	     */
	    
	    public Transaction[] handleTxs(Transaction[] possibleTxs) {

//	    	Initialize maxFee to zero
	    	maxFee = 0;
	    	
	    	ArrayList<Transaction> remainingTxs = new ArrayList<Transaction>(Arrays.asList(possibleTxs));

	    	//	    	Filter out txs that double spend 
	    	ArrayList<Transaction> doubleSpendTxs = new ArrayList<Transaction>();	        
	    	for(Transaction tx : remainingTxs) {
	    		if(!noDoubleSpends(tx)) {
	    			doubleSpendTxs.add(tx);
	    		}
	    	}
	    	remainingTxs.removeAll(doubleSpendTxs);
	    	
	    	
	    	//Find inputs claimed multiple times 	
	        splitTxs(remainingTxs); 	
	    	
	        return acceptedTxsWithMaxFees;
	    }

	    public static void main(String[] args) {


	    }	
	    
}
