//import java.math.BigInteger;
import java.math.BigInteger;
import java.security.*;

public class testFile {

	public static void main(String[] args)
			throws NoSuchAlgorithmException,SignatureException {

		KeyPair pk_scrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		KeyPair pk_alice = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		KeyPair pk_bob = KeyPairGenerator.getInstance("RSA").generateKeyPair();

//		Set up the root transaction	
		Tx tx = new Tx();
		
//		Scrooge mints money
		tx.addOutput(10, pk_scrooge.getPublic());
		byte[] initialHash = BigInteger.valueOf(0).toByteArray();
		tx.addInput(initialHash, 0);
		tx.signTx(pk_scrooge.getPrivate(),0);
		tx.addOutput(9, pk_scrooge.getPublic());
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
		tx2.addOutput(8, pk_alice.getPublic());
		tx2.addOutput(7, pk_bob.getPublic());
		tx2.signTx(pk_scrooge.getPrivate(), 0);
		tx2.signTx(pk_scrooge.getPrivate(), 1);

		
//		Set up another test Transaction
		Tx tx3 = new Tx();
		tx3.addInput(tx2.getHash(),	0);
		tx3.addOutput(6, pk_bob.getPublic());		
		tx3.signTx(pk_alice.getPrivate(), 0);

		
		Tx tx4 = new Tx();
		tx4.addInput(tx2.getHash(),2);
        tx4.addOutput(4, pk_scrooge.getPublic());
        tx4.signTx(pk_bob.getPrivate(), 0);
		
//		Start the txHandler
		TxHandler txHandler = new TxHandler(utxoPool);
//		System.out.println("txHandler.isValidTx(tx2) returns: " + txHandler.isValidTx(tx2));		
//	    System.out.println("txHandler.handleTxs(new Transaction[]{tx2, tx3}) returns: " +
//		            txHandler.handleTxs(new Transaction[]{tx2}).length + " transaction(s)");
		


				 txHandler.handleTxs(new Transaction[]{tx2,tx3});
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
