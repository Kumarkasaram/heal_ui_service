package com.heal.dashboard.service;

import java.util.HashMap;
import java.util.Map;

public class Test {

	public static void main(String[] args) {
		
		
		
	
		
		
	     String str ="Active connections: 2 \n" +
                 "server accepts handled requests\n" +
                 " 25 25 54 \n" +
                 "Reading: 0\n"+
                 "Writing: 1\n"+
                 "Waiting: 1 ";

	     String[] keyValuePairs = str.split("\n");              //split the string to creat key-value pairs
	     String[] server = keyValuePairs[1].split(" ");
	     String[] serverValue = keyValuePairs[2].split(" ");
	     keyValuePairs[1] = server[0]+" "+server[1]+":"+serverValue[1];
	     keyValuePairs[2] = server[0]+" "+server[2]+":"+serverValue[2];
	     keyValuePairs[4] = server[0]+" "+server[3]+":"+serverValue[3];
	     
         System.out.println("keyValuePairs :"+keyValuePairs);

	     Map<String,String> map = new HashMap();               

	     for(String pair : keyValuePairs)                        //iterate over the pairs
	     {
	         String[] entry = pair.split(":");                   //split the pairs to get key and value 
	         map.put(entry[0].trim(), entry[1].trim());          //add them to the hashmap and trim whitespaces
	     }
         System.out.println("test"+map);

	}

}
