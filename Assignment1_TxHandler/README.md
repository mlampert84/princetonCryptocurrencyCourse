# See 'Assignment1.pdf' for a description of the Assignment.

# Outline of my solution to MaxFeeTxHandler

MaxFeeTxHandler finds the set of txs that maximize fees by using the following strategy:

1.  any txs are removed that double spend an input, for these will never be valid, and would interfere with the search for double spends in step 2.
2.  The array of txs is searched for any txs that try to spend the same input. If such txs are found, then subsets of the transactions are made so that the double spend does not exist in each subset (for example, say for txs 1 through 5, there are three txs, 2,3,4, that all claim the same input. Three subsets of txs are created: {1,2,5},{1,3,5}, {1,4,5}.
3.  Subsets are recursively put through Step 2 until no txs in the subset double-spend an input.
4.  Once a subset exists where no transaction claims the same input, the subset of transactions is processed as in the non-extra credit version, simply looking for the next valid tx and processing it, and then repeating this until there are no more valid txs. The fee is calculated while the txs are being processed, and if it exceeds the current max fee, that subset of processed transactions is set to be returned as the maximized set of accepted txs.
