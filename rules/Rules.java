package rules;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;


public class Rules {

	String [] ruleset = { };
	public static void main(String[] args){
		String path = System.getProperty("user.dir");//build path to training corpus from cwd and args[0]
		if(path.contains("\\"))
			path+="\\";
		else
			path+="/";
		path+=args[0];
		
		File training = new File (path);//get file and attempt to scan it.
		Scanner scan;
		try {
			scan = new Scanner (training);
		} catch (FileNotFoundException e) {//if file is not found, terminate the program
			System.out.println("FATAL ERROR: file "+args[0]+" not found at "+path);
			e.printStackTrace();
			return;
		}
		
		ArrayList<String> out = new ArrayList<String>();
		out.add("");
		while(scan.hasNext()){
			ArrayList<String> nonTaggedSentence= new ArrayList<String>();//builds next non-tagged sentence from the test corpus
			
			nonTaggedSentence.add("stopSentence\tstopSentence");//add the sentence's start state 
			String tmp=null;
			while(!(tmp = scan.nextLine()).equals("")){//add tokens until the end of the sentence
				nonTaggedSentence.add(tmp);
			}
			nonTaggedSentence.add("stopSentence\tstopSentence");//add the sentence's stop state
			
			if(nonTaggedSentence.size()>2){
				ArrayList<String> taggedSentence = tag(nonTaggedSentence);//tag the sentence
			
				for(int i = 0; i<taggedSentence.size();i++)//adds all lines of the newly tagged sentence to the output list
					out.add(taggedSentence.get(i));
			}
		}
		
		scan.close();
		
		
		String path2 = System.getProperty("user.dir");//builds path to output file (ALWAYS ouput.pos in the cwd)
		if(path2.contains("\\"))
			path2+="\\";
		else
			path2+="/";
		path2+="ruleOutput.chunk";
		
		try {//writes to the output file
			File chunk = new File(path2);
			Files.write(Paths.get(path2), out, StandardCharsets.UTF_8);
		} catch (IOException e) {
			
			e.printStackTrace();
			System.out.println("FATAL ERROR: Can't write to output.pos at location "+path2);
			//scan.close();
			return;
		}
		//scan.close();
	}
	

	static ArrayList<String> tag(ArrayList<String> rawWords){
		//String lastTag = new String();
		ArrayList<String> outSentence = new ArrayList<String>();
		//String[][] chart = new String[rawWords.size()][rawWords.size()];
		for(int i = 0; i<rawWords.size(); i++){
			String[] entry = rawWords.get(i).split("\\s+");
			String word = entry[0];
			String pos = entry[1];
			if((pos.equals("NNP")||word.equals("tomorrow"))&&rawWords.get(i-1).split("\\s+")[0].equals("due")){
				int beginning = i;
				int ending = i;
				int j = i+1;
				boolean endingFound = false;
				while(!endingFound){
					String next=rawWords.get(j).split("\\s")[1];
					if(next.equals("NN")||next.equals("NNP")||next.equals("NNS")||next.equals("NNPS")||next.equals("CD")){
						ending++;
						j++;
					}
					else
						endingFound = true;
				}
				while(beginning<outSentence.size()){
					outSentence.remove(outSentence.size()-1);
				}
				String nounGroupLine = rawWords.get(beginning).split("\\s+")[0]+"\tB-NP";
				outSentence.add(nounGroupLine);
				for(int index = beginning+1; index<=ending; index++){
					nounGroupLine = rawWords.get(index).split("\\s+")[0]+"\tI-NP";
					outSentence.add(nounGroupLine);
				}
				i=ending;
			}
			/*else if(pos.equals("CD")&&rawWords.get(i-1).split("\\s+")[0].equals("$")&&rawWords.get(i-2).split("\\s+")[1].equals("IN")){
				int beginning=i;
				int ending = i;
				int j=i+1;
				boolean endingFound = false;
				while(!endingFound){
					String next=rawWords.get(j).split("\\s")[1];
					if(next.equals("CD")){
						ending++;
						j++;
					}
					else
						endingFound = true;
				}
				boolean beginningFound = false;
				j = i-1;
				while(!beginningFound){
					String prior=rawWords.get(j).split("\\s")[1];
					String priorWord = rawWords.get(j).split("\\s")[0];
					if(prior.equals("$")){
						beginning--;
						j--;
					}
					else if (rawWords.get(j).split("\\s+")[0].equals("as")&&rawWords.get(j-1).split("\\s+")[0].equals("much")&&rawWords.get(j-2).split("\\s+")[0].equals("as")){
						beginning -=3;
						beginningFound = true;
						j -=3;
					}
					else if (prior.equals("IN")){
						beginning --;
						j--;
						beginningFound = true;
					}
						
					else
						beginningFound = true;
				}
				while(beginning<outSentence.size()){
					outSentence.remove(outSentence.size()-1);
				}
				String nounGroupLine = rawWords.get(beginning).split("\\s+")[0]+"\tB-NP";
				outSentence.add(nounGroupLine);
				for(int index = beginning+1; index<=ending; index++){
					nounGroupLine = rawWords.get(index).split("\\s+")[0]+"\tI-NP";
					outSentence.add(nounGroupLine);
				}
				i=ending;
			}*/
			else if(pos.equals("PRP")||pos.equals("WP")||word.equals("those")||pos.equals("WDT")){
				int beginning = i;
				int ending = i;
				String nounGroupLine = rawWords.get(beginning).split("\\s+")[0]+"\tB-NP";
				outSentence.add(nounGroupLine);
			}
			/*else if (word.equals("same")&&rawWords.get(i-1).split("\\s")[0].equals("the")){
				outSentence.remove(outSentence.size()-1);
				String nounGroupLine = "the\tB-NP";
				outSentence.add(nounGroupLine);
				nounGroupLine = "same\tI-NP";
				outSentence.add(nounGroupLine);
			}*/
			else if(pos.equals("NN")||pos.equals("NNS")||pos.equals("NNP")||pos.equals("NNPS")||pos.equals("CD")/*||pos.equals("PRP")*/||pos.equals("EX")){
				int beginning = i;
				int ending = i;
				boolean endFound = false;
				int j = i+1;
				while(!endFound){
					String next=rawWords.get(j).split("\\s")[1];
					if(next.equals("NN")||next.equals("NNS")||next.equals("NNP")||next.equals("NNPS")||next.equals("CD")||(next.equals("CC")&&rawWords.get(j-1).split("\\s")[1].equals("CD")&&rawWords.get(j+1).split("\\s")[1].equals("CD"))){
						ending++;
						j++;
					}
					else
						endFound = true;
				}
				boolean beginningFound = false;
				j = i-1;
				while(!beginningFound){
					String[] pArray = rawWords.get(j).split("\\s+");
					String prior = pArray[1];
					if(prior.equals("JJ")||prior.equals("JJR")||prior.equals("JJS")||prior.equals("PRP$")||prior.equals("RBS")||prior.equals("$")||prior.equals("RB")||(prior.equals("VBG")&&rawWords.get(j-1).split("\\s+")[1].equals("JJR"))||(pArray[0].equals("more")&&rawWords.get(j+1).split("\\s+")[1].equals("JJ"))){
						beginning--;
						j--;
					}
					else if (prior.equals("DT")||prior.equals("POS")){
						beginning--;
						beginningFound = true;
					}
					else
						beginningFound = true;
				}
				while(beginning<outSentence.size()){
					outSentence.remove(outSentence.size()-1);
				}
				String nounGroupLine = rawWords.get(beginning).split("\\s+")[0]+"\tB-NP";
				outSentence.add(nounGroupLine);
				for(int index = beginning+1; index<=ending; index++){
					nounGroupLine = rawWords.get(index).split("\\s+")[0]+"\tI-NP";
					outSentence.add(nounGroupLine);
				}
				i=ending;
			}
			else{
				String outLine = word+"\tO";
				outSentence.add(outLine);
			}
		}
		outSentence.remove(0);
		outSentence.remove(outSentence.size()-1);
		outSentence.add("");
		return outSentence;
	}

}
