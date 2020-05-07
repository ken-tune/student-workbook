package com.aerospike;


import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Record;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Value;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.ScanCallback;
import com.aerospike.client.query.PartitionFilter;
import com.aerospike.client.Language;

public class CorrectScans {

        PartitionFilter pf = PartitionFilter.id(0);   //Initialize to start at partition 0.
        //We can update it in the asynchronous call back with the last digest value.
       
        //Tracking counters, can be updated in the call back. 
        int num_this_scan = 0;
	int num_total = 0;
	int num_total_tweets = 0;

	public static void main(String[] args) {

                CorrectScans cs = new CorrectScans();
                cs.work();
        }

        public void work() {
		//AerospikeClient client = new AerospikeClient("172.28.128.3", 3000); //Update IP Address 
		AerospikeClient client = new AerospikeClient("127.0.0.1", 3000); //Update IP Address 

                num_total_tweets=0;
	        //Scan all tweets  (no PartitionFilter - regular scan for count validation.)
                scanAllTweetsForAllUsers(client);

                System.out.println("Total: ***** "+num_total_tweets+" *****");

                // Scan partition by partition, with pagination.
                Key startKey=null;
                int count = 3;
                num_total = 0;

                for(int pid=0; pid<4096; pid++){
                 pf = PartitionFilter.id(pid);  
                 num_this_scan = count;
                 if(client!=null){
                   while(num_this_scan >= count ){
                        scanByPartitionWithPagination(client, count);
                        System.out.println("["+pid+"]: -- partial, up to "+count+" --");
                   }
                   System.out.println("["+pid+"]: ********************");
                 }
                 else{
                   System.out.println("NULL client?");
                 }
                }
                System.out.println("Total by 1 partition, with pagination: ***** "+num_total+" *****");
                System.out.println("Total actual tweets: ***** "+num_total_tweets+" *****");

                // Scan 4 partitions at a time, with pagination.
                
                num_total = 0;
                int pRange = 4;

                for(int pid=0; pid<4096; pid+=pRange){  //Process 4 partitions at a time.
                 pf = PartitionFilter.range(pid,pRange);  
                 if(client!=null){
                   scanByPartitionsRange(client);
                   System.out.println("["+pid+"]: ********************");
                 }
                 else{
                   System.out.println("NULL client?");
                 }
                }
                System.out.println("Total by "+pRange+" partitions at a time, with pagination: ***** "+num_total+" *****");
                System.out.println("Total actual tweets: ***** "+num_total_tweets+" *****");

                client.close();
	}


      public void scanAllTweetsForAllUsers(AerospikeClient client) {
	        //Scan all tweets
                ScanPolicy policy = new ScanPolicy();
                policy.concurrentNodes = false;
                policy.includeBinData = true;
                try {
                  client.scanAll(policy, "test", "tweets", new ScanCallback() {

                                public void scanCallback(Key key, Record record)
                                                throws AerospikeException {
                                        //System.out.println(record.getValue("tweet") + "\n");
                                        //Not printing the tweet, just taking the count to crosscheck.
                                        num_total_tweets += 1;
                                }
                        }, "tweet");
                } catch (AerospikeException e) {
                        System.out.println("EXCEPTION - Message: " + e.getMessage());
                }
      } 

      public void scanByPartitionWithPagination(AerospikeClient client, int count) {
	        //Scan all tweets starting with partion id, limiting to count and 
	        //resuming the same partition till end by using start digest
	        //for subsequent PartitionFilter.
                ScanPolicy policy = new ScanPolicy();
                policy.concurrentNodes = false;
                policy.includeBinData = true;
                policy.maxRecords = count; 
                num_this_scan = 0;
                
                try {
                  client.scanPartitions(policy, pf,  "test", "tweets", new ScanCallback() {
                                public void scanCallback(Key key, Record record)
                                                throws AerospikeException {
                                        System.out.println(record.getValue("tweet") + "\n");
                                        pf = PartitionFilter.after(key);
                                        num_this_scan += 1;
                                        num_total += 1;
                                }
                        }, "tweet");
                  
                } catch (AerospikeException e) {
                        System.out.println("EXCEPTION - Message: " + e.getMessage());
                }
     }
     public void scanByPartitionsRange(AerospikeClient client) {
	        //Scan all tweets
                ScanPolicy policy = new ScanPolicy();
                policy.concurrentNodes = false;
                policy.includeBinData = true;
                policy.maxRecords = 0; //count; 
                num_this_scan = 0;
                
                try {
                  client.scanPartitions(policy, pf,  "test", "tweets", new ScanCallback() {
                                public void scanCallback(Key key, Record record)
                                                throws AerospikeException {
                                        System.out.println(record.getValue("tweet") + "\n");
                                        num_this_scan += 1;
                                        num_total += 1;
                                }
                        }, "tweet");
                  
                } catch (AerospikeException e) {
                        System.out.println("EXCEPTION - Message: " + e.getMessage());
                }
     }
}
