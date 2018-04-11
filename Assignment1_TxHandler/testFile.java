//import java.math.BigInteger;
import java.math.BigInteger;
import java.security.*;

public class testFile {

	public static void main(String[] args)
			throws NoSuchAlgorithmException,SignatureException {
		KeyPair pk_scrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		KeyPair pk_alice = KeyPairGenerator.getInstance("RSA").generateKeyPair();

//		Set up the root transaction	
		Tx tx = new Tx();
		
//		Scrooge mints money
		tx.addOutput(10, pk_scrooge.getPublic());
		byte[] initialHash = BigInteger.valueOf(0).toByteArray();
		tx.addInput(initialHash, 0);
		tx.signTx(pk_scrooge.getPrivate(),0);
		
		tx.addOutput(5, pk_scrooge.getPublic());
		byte[] initialHash1 = BigInteger.valueOf(0).toByteArray();
		tx.addInput(initialHash1, 1);
		tx.signTx(pk_scrooge.getPrivate(),1);

		
		
//		Set up the UTXOPool
		UTXOPool utxoPool = new UTXOPool();
		UTXO utxo = new UTXO(tx.getHash(),0);
		utxoPool.addUTXO(utxo,tx.getOutput(0));
		UTXO utxo1 = new UTXO(tx.getHash(),1);
		utxoPool.addUTXO(utxo1,tx.getOutput(1));
		
		
//		Set up a test Transaction
		Tx tx2 = new Tx();
		tx2.addInput(tx.getHash(), 0);
		tx2.addInput(tx.getHash(), 1);

		//		Double add input to make sure it fails validity test
//		tx2.addInput(tx.getHash(), 0);

		
		
		//Add fake input to transaction
//		tx2.addInput(BigInteger.valueOf(0).toByteArray(),0);

		
		tx2.addOutput(12, pk_alice.getPublic());
		tx2.addOutput(1, pk_alice.getPublic());
		tx2.addOutput(1, pk_alice.getPublic());
		
		tx2.signTx(pk_scrooge.getPrivate(), 0);
		tx2.signTx(pk_scrooge.getPrivate(), 1);
		
//		Start the txHandler
		TxHandler txHandler = new TxHandler(utxoPool);
		System.out.println("txHandler.isValidTx(tx2) returns: " + txHandler.isValidTx(tx2));		
		 System.out.println("txHandler.handleTxs(new Transaction[]{tx2}) returns: " +
		            txHandler.handleTxs(new Transaction[]{tx2}).length + " transaction(s)");
		
//		 txHandler.handleTxs(new Transaction[]{tx2});
	}

	//Copied from https://github.com/keskival/cryptocurrency-course-materials/blob/master/assignment1/Main.java
	public static class Tx extends Transaction { 
        public void signTx(PrivateKey sk, int input) throws SignatureException {
            Signature sig = null;
            try {
                sig = Signature.getInstance("SHA256withRSA");
                sig.initSign(sk);
                sig.update(this.getRawDataToSign(input));
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
            this.addSignature(sig.sign(),input);
            // Note that this method is incorrectly named, and should not in fact override the Java
            // object finalize garbage collection related method.
            this.finalize();
        }
    }
}
