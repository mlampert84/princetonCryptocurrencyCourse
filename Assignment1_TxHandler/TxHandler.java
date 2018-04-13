import java.util.ArrayList;
import java.util.Arrays;
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
//    	System.out.println("Transaction has inputs of size " + tx.getInputs().size());
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
        int inputIndex = 0;
        for(Transaction.Input i : txInputs) {
        	byte[] sigToTest = i.signature;
        	PublicKey keytoTest = currentPool.getTxOutput(new UTXO(i.prevTxHash, i.outputIndex)).address;
        	byte[] rawDataToSign = tx.getRawDataToSign(inputIndex);
        	//           System.out.println("Checking input with index " + i.outputIndex);
        	if(!Crypto.verifySignature(keytoTest, rawDataToSign,sigToTest)) {
              System.out.println("Signature didn't verify");
        		return false;
        	}
        	inputIndex++;
        }


//       	ArrayList<UTXO> theUTXOs = currentPool.getAllUTXO();
        for(UTXO ut : claimedOutputs) {
        	inputTotal += currentPool.getTxOutput(ut).value;
        }
//        System.out.println("Input total: " + inputTotal);

        ArrayList<Transaction.Output> txs = tx.getOutputs();
    	for(Transaction.Output o : txs) {
    		if(o.value < 0) {
    			System.out.println("Output value is negative");
    			return false;}
    		outputTotal += o.value;
    	}
//    	System.out.println("Output Total: " + outputTotal);
//    	System.out.println("Input Total: " + inputTotal);

    	if(outputTotal > inputTotal) {
    		System.out.println("Output total exceeds inputTotal");
    		return false;
    	}
    	System.out.println("Transaction with total output " + outputTotal + " and input total " + inputTotal + " is valid.");
    	return true;
    }

    private void showPoolContents(UTXOPool pool) {

    	if(currentPool.getAllUTXO().size() == 0)
    		System.out.println("Current pool is empty");

    	for(UTXO ut : currentPool.getAllUTXO()) {
    		Transaction.Output op = currentPool.getTxOutput(ut);
    		System.out.println("Current pool contains input of value: " + op.value + " belonging to "
    		  + testHelperFunctions.bytesToHex(op.address.getEncoded()));

    	}

    	if(pool.getAllUTXO().size() == 0)
    		System.out.println("Tx Pool is empty");

    	for(UTXO ut : pool.getAllUTXO())
    		System.out.println("Tx Pool contains input of value: " + pool.getTxOutput(ut).value);
    }

    private void processTx(Transaction tx) {
   	   ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
   	   ArrayList<Transaction.Input> txInputs = tx.getInputs();

   	   Transaction txInputsCopy = new Transaction();
   	   for(Transaction.Input i : tx.getInputs()) {
   		   txInputsCopy.addInput(i.prevTxHash, i.outputIndex);
   	   }

   	   ArrayList<Transaction.Input> remainingTxInputs = txInputsCopy.getInputs();


       //Make a pool of UTXOs containing all the inputs in the transaction.
       UTXOPool utxosInTx = new UTXOPool();
       for(Transaction.Input i : txInputs) {
    	   UTXO currentUTXO = new UTXO(i.prevTxHash,i.outputIndex);
    	   utxosInTx.addUTXO(currentUTXO, currentPool.getTxOutput(currentUTXO));
       }

//   	   	Process each output one by one, consuming inputs as necessary.

        for(int k = 0; k < txOutputs.size(); k++) {

            showPoolContents(utxosInTx);

        	Transaction.Output o = txOutputs.get(k);
        	System.out.println("Testing output with value " + o.value);
        	double remainingOutput = o.value;
        	while(remainingOutput > 0){

        	      showPoolContents(utxosInTx);

         		   System.out.println("Output's current value is " + remainingOutput);
         		   Transaction.Input i = remainingTxInputs.get(0);
         		   UTXO currentUTXO = new UTXO(i.prevTxHash,i.outputIndex);
         		   double value = utxosInTx.getTxOutput(currentUTXO).value;
         		   PublicKey address = utxosInTx.getTxOutput(currentUTXO).address;
         		   Transaction.Output sourceOutput = tx.new Output(value,address);
         		   System.out.println("Comparing to input with value " + sourceOutput.value);

         		   //Output is less than the input, meaning the output can now be added to the pool
         		   //and the input can still be used in this specific transaction for other outputs
         		      if(remainingOutput < sourceOutput.value) {
		          		   //revise the inputs value
         		    	   sourceOutput.value -= remainingOutput;
         		    	   //no output remains
		          		   remainingOutput = 0;
		          		   //Revise the input UTXO to reflect the new lower value
		          		   utxosInTx.removeUTXO(currentUTXO);
		          		   utxosInTx.addUTXO(currentUTXO, sourceOutput);
		          		   System.out.println("New input value: " + currentPool.getTxOutput(currentUTXO).value);

		          		   //Add the new output to the current UTXOPool
		          		   currentPool.addUTXO(new UTXO(tx.getHash(),i.outputIndex), o);
		          	   }

         		      //Output is greater than input, meaning the output is reduced, and the input
         		      //is no longer available.
         		      else{
		          	      remainingOutput -= sourceOutput.value;
		          	      System.out.println("Removing input with value: " + sourceOutput.value
		          	    		  + ". Output has value " + remainingOutput);

		          	      //Add new available output to UTXO
		          	      currentPool.addUTXO(new UTXO(tx.getHash(), k), o);

		          	      //Input is exhausted so remove it from the utxosInTx in the transaction
		          	      //And from the tx inputs
		          	      utxosInTx.removeUTXO(currentUTXO);
		          	      remainingTxInputs.remove(0);
		          	   	}
         	   }
        }

        //Remove all the tx inputs from the UTXO Pool, since a tx consumes all inputs.
        for(Transaction.Input i : txInputs) {
        	System.out.println("Cycling through tx inputs");
        	UTXO toRemove = new UTXO(i.prevTxHash,i.outputIndex);
        	System.out.println("Removing from UTXO pool input of value " + currentPool.getTxOutput(toRemove).value);
        	currentPool.removeUTXO(toRemove);

        }

        System.out.println("Finished processing transaction.");
        showPoolContents(utxosInTx);
    }

//    private double txTotalInput(Transaction tx) {
//       	double inputTotal = 0;
//    	ArrayList<UTXO> claimedOutputs = new ArrayList<UTXO>();
//        for(Transaction.Input i : tx.getInputs()) {
//          UTXO nextUTXO = new UTXO(i.prevTxHash,i.outputIndex);
//           claimedOutputs.add(nextUTXO);
//        }
//        for(UTXO ut : claimedOutputs) {
//        	inputTotal += currentPool.getTxOutput(ut).value;
//        }
//        return inputTotal;
//    }
//
    private double txTotalOutput(Transaction tx) {
    	double outputTotal = 0;
    	   ArrayList<Transaction.Output> txs = tx.getOutputs();
    	for(Transaction.Output o : txs) {
    		outputTotal += o.value;
    	}
    	return outputTotal;
    }
    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {


    	ArrayList<Transaction> remainingPosTxs = new ArrayList<Transaction>(Arrays.asList(possibleTxs));
        ArrayList<Transaction> acceptedTxs= new ArrayList<Transaction>();
       	Transaction txToProcess = null;

        do {
        	if(txToProcess != null) {
//      			System.out.println("Processing Transaction.");
                processTx(txToProcess);
    			acceptedTxs.add(txToProcess);
    			txToProcess = null;
    		}

         	for(int i = 0; i < remainingPosTxs.size(); i++) {
         		Transaction tx = remainingPosTxs.get(i);
         		if(isValidTx(tx)) {
          		System.out.println("Adding to processessing stream transaction with total input of " + txTotalOutput(tx));
          		txToProcess = tx;
          		remainingPosTxs.remove(i);
          		break;
         		}
         		else
         			System.out.println("Transaction with total output of " + txTotalOutput(tx) + " is not valid.");

         	}
    	} while(txToProcess != null);


    	return acceptedTxs.toArray(new Transaction[acceptedTxs.size()]);
    }

    public static void main(String[] args) {


    }

}
