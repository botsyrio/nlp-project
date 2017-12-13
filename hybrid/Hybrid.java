package hybrid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;



public class Hybrid {

		static HashMap<String,HashMap<String, HashMap<String, hashNode>>> posToWordToTag  = new HashMap<String, HashMap<String, HashMap<String, hashNode>>>();
		static HashMap<String, HashMap<String,HashMap<String,HashMap<String, hashNode>>>> posToTagToPosToTag = new HashMap<String, HashMap<String,HashMap<String,HashMap<String, hashNode>>>>();
		
		static HashMap<String,HashMap<String, HashMap<String, hashNode>>> prevPosToPosToTag = new HashMap<String, HashMap<String, HashMap<String, hashNode>>>();
		static HashMap<String,HashMap<String,HashMap<String, hashNode>>> tagToPosToTag = new HashMap<String,HashMap<String,HashMap<String, hashNode>>>();
		static HashMap<String,HashMap<String,HashMap<String, hashNode>>> nextPosToPosToTag = new HashMap<String,HashMap<String,HashMap<String, hashNode>>>();
		

		
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
			
			String lastPos = "the thing";//String which stores the part of speech from the previous line.
			//Used when updating posToTagToPosToTag. The initial value is a code which tells the program that it's on
			//the first word and therefore cannot yet make a posToTagToPosToTag entry
			String lastTag = "the thing";
			
			while(scan.hasNextLine()){//fill posToWordToTag and posToTagToPosToTag with counts
				String currPos;
				String word;
				String tag;
				String str = scan.nextLine();
				if(str.equals("")){//case where the current line represents the start/end state
					word = "stopSentence";//manually set word and currPos
					currPos = "stopSentence";
					tag = "O";
				}
				else{//set word and currPos to the tokens given on the current line of the training corpus
					String [] line = str.split("\\s+");
					currPos = line[1];
					word = line[0];
					tag = line[2];
				}
				//update posToWordToTag
				if(!posToWordToTag.containsKey(currPos))//case where the currPos has not been encountered yet
					posToWordToTag.put(currPos, new HashMap<String, HashMap<String, hashNode>>());
				
				if(!posToWordToTag.get(currPos).containsKey(word))//case where the current key has not been associated with the current Pos
					posToWordToTag.get(currPos).put(word, new HashMap<String, hashNode>());
				
				if(!posToWordToTag.get(currPos).get(word).containsKey(tag))//case where the current tag has not been associated with the current word and pos
					posToWordToTag.get(currPos).get(word).put(tag, new hashNode());
					
				
				posToWordToTag.get(currPos).get(word).get(tag).value+=1.0;//increment count 
				//end update posToWordToTag
				
				//update posToTagToPosToTag
				if(!lastPos.equals("the thing")){//returns true in all cases except when the current line is the first line
					if(!posToTagToPosToTag.containsKey(lastPos))//case where lastPos has not been encountered as a previous pos
						posToTagToPosToTag.put(lastPos, new HashMap<String, HashMap<String, HashMap<String, hashNode>>>());
						
					if(!posToTagToPosToTag.get(lastPos).containsKey(lastTag))//case where currPos has not followed lastPos
						posToTagToPosToTag.get(lastPos).put(lastTag, new HashMap<String, HashMap<String, hashNode>>());
					
					
					if(!posToTagToPosToTag.get(lastPos).get(lastTag).containsKey(currPos))//case where currPos has not followed lastPos
						posToTagToPosToTag.get(lastPos).get(lastTag).put(currPos, new HashMap<String, hashNode>());
					
					if(!posToTagToPosToTag.get(lastPos).get(lastTag).get(currPos).containsKey(tag))//case where currPos has not followed lastPos
						posToTagToPosToTag.get(lastPos).get(lastTag).get(currPos).put(tag, new hashNode());
					
					posToTagToPosToTag.get(lastPos).get(lastTag).get(currPos).get(tag).value+=1.0;//increment count
					
					if(!tagToPosToTag.containsKey(lastTag))//case where currPos has not followed lastPos
						tagToPosToTag.put(lastTag, new HashMap<String, HashMap<String, hashNode>>());
					
					if(!tagToPosToTag.get(lastTag).containsKey(currPos))//case where currPos has not followed lastPos
						tagToPosToTag.get(lastTag).put(currPos, new HashMap<String, hashNode>());
					
					if(!tagToPosToTag.get(lastTag).get(currPos).containsKey(tag))//case where currPos has not followed lastPos
						tagToPosToTag.get(lastTag).get(currPos).put(tag, new hashNode());
					
					tagToPosToTag.get(lastTag).get(currPos).get(tag).value+=1.0;//increment count
					
					if(!nextPosToPosToTag.containsKey(currPos))//case where lastPos has not followed lastPos
						nextPosToPosToTag.put(currPos, new HashMap<String, HashMap<String, hashNode>>());
					
					if(!nextPosToPosToTag.get(currPos).containsKey(lastPos))//case where lastPos has not followed lastPos
						nextPosToPosToTag.get(currPos).put(lastPos, new HashMap<String, hashNode>());
					
					if(!nextPosToPosToTag.get(currPos).get(lastPos).containsKey(lastTag))//case where lastPos has not followed lastPos
						nextPosToPosToTag.get(currPos).get(lastPos).put(lastTag, new hashNode());
					
					nextPosToPosToTag.get(currPos).get(lastPos).get(lastTag).value+=1.0;//increment count
				}
				lastPos = currPos;//lastPos updated for next run
				lastTag = tag;
			}
			
			scan.close();
			
			//the gist of the following 2 code blocks (namely, the basic idea of how to update 
			//all elements of a HashMap in a single loop) which convert the counts in posToTagToPosToTag and posToWordToTag
			//to probability ratios, was taken from stackoverflow user Kevin Meredith's answer here:
			//https://stackoverflow.com/questions/16588492/update-all-values-at-a-time-in-hashmap
			
			//update the values in each hashNode in posToTagToPosToTag so that they're probability ratios, not counts
			for(HashMap.Entry<String, HashMap<String, HashMap<String, HashMap<String, hashNode>>>>prevPos:posToTagToPosToTag.entrySet()){
				for(HashMap.Entry<String, HashMap<String, HashMap <String, hashNode>>> prevTag:prevPos.getValue().entrySet()){
					for(HashMap.Entry<String, HashMap<String, hashNode>> pos: prevTag.getValue().entrySet()){
						double total = 0;//total number of this pos given the previous pos with its tag
						for(HashMap.Entry<String, hashNode> yaBoi: pos.getValue().entrySet()){
							total+=yaBoi.getValue().value;
						}
						for(HashMap.Entry<String, hashNode> yaBoi:pos.getValue().entrySet()){
							yaBoi.getValue().value=yaBoi.getValue().value/total;//converts count of prev pos -> current pos to probability of the current pos occurring given the prev pos
						}
						if(!pos.getValue().containsKey("B-NP"))
							pos.getValue().put("B-NP", new hashNode());
						
						if(!pos.getValue().containsKey("I-NP"))
							pos.getValue().put("I-NP", new hashNode());
						
						if(!pos.getValue().containsKey("O"))
							pos.getValue().put("O", new hashNode());
					}
				}
			}
			
			//same as the above code block, but in posToWordToTag
			for(HashMap.Entry<String, HashMap<String, HashMap<String,hashNode>>>pos:posToWordToTag.entrySet()){
				if(!pos.getValue().containsKey("OOV")){//makes sure there's an OOV entry for each POS so lookup does not result in a null pointer
					pos.getValue().put("OOV", new HashMap<String, hashNode>());
				}
				if(!pos.getValue().get("OOV").containsKey("B-NP"))
					pos.getValue().get("OOV").put("B-NP", new hashNode());

				if(!pos.getValue().get("OOV").containsKey("I-NP"))
					pos.getValue().get("OOV").put("I-NP", new hashNode());
				
				if(!pos.getValue().get("OOV").containsKey("O"))
					pos.getValue().get("OOV").put("O", new hashNode());
				double numOovB =0;//number of OOV words tagged B-NP
				double numOovI =0;//number of OOV words tagged I-NP
				double numOovO =0;//number of OOV words tagged O
				double numOOV=0;
				for(HashMap.Entry<String, HashMap<String, hashNode>> word:pos.getValue().entrySet()){
					double total = 0;//total number of instances of words of a given part of speech
					for(HashMap.Entry<String, hashNode> yaBoi:word.getValue().entrySet()){
						total+=yaBoi.getValue().value;//increment total
					}
					if(total<=2){//updates OOV count for the given pos whenever there's a word of given pos which only occurs once in the training corpus
						if(word.getValue().containsKey("B-NP")&&word.getValue().get("B-NP").value<=2){
							numOovB++;
							numOOV++;
						}
						if(word.getValue().containsKey("I-NP")&&word.getValue().get("I-NP").value<=2){
							numOovI++;
						}
						if(word.getValue().containsKey("O")&&word.getValue().get("O").value<=2){
							numOovO++;
							numOOV++;
						}
					}

					for(HashMap.Entry<String, hashNode> yaBoi:word.getValue().entrySet()){
						yaBoi.getValue().value=yaBoi.getValue().value/total;//increment total
					}

					if(!word.getValue().containsKey("B-NP"))
						word.getValue().put("B-NP", new hashNode());
					
					if(!word.getValue().containsKey("I-NP"))
						word.getValue().put("I-NP", new hashNode());
					
					if(!word.getValue().containsKey("O"))
						word.getValue().put("O", new hashNode());

				}
				pos.getValue().get("OOV").get("B-NP").value=numOovB;//sets OOV count to value explained above
				pos.getValue().get("OOV").get("I-NP").value=numOovI;
				pos.getValue().get("OOV").get("O").value=numOovO;
				
				for(HashMap.Entry<String, hashNode> yaBoi:pos.getValue().get("OOV").entrySet()){
					yaBoi.getValue().value=yaBoi.getValue().value/numOOV;//converts count to probability of the tag given the pos and the word
				}
			}
			
			for(HashMap.Entry<String, HashMap<String, HashMap <String, hashNode>>> prevTag:tagToPosToTag.entrySet()){
				for(HashMap.Entry<String, HashMap<String, hashNode>> pos: prevTag.getValue().entrySet()){
					double total = 0;//total number of this pos given the previous pos with its tag
					for(HashMap.Entry<String, hashNode> yaBoi: pos.getValue().entrySet()){
						total+=yaBoi.getValue().value;
					}
					for(HashMap.Entry<String, hashNode> yaBoi:pos.getValue().entrySet()){
						yaBoi.getValue().value=yaBoi.getValue().value/total;//converts count of prev pos -> current pos to probability of the current pos occurring given the prev pos
					}

					if(!pos.getValue().containsKey("B-NP"))
						pos.getValue().put("B-NP", new hashNode());
					
					if(!pos.getValue().containsKey("I-NP"))
						pos.getValue().put("I-NP", new hashNode());
					
					if(!pos.getValue().containsKey("O"))
						pos.getValue().put("O", new hashNode());
				}
			//}
		}
			/*for(HashMap.Entry<String, HashMap<String, HashMap <String, hashNode>>> nextPos:nextPosToPosToTag.entrySet()){
				nextPosToPosToTag.get(nextPos.getKey()).put("OOV", new HashMap<String, hashNode>());
				HashMap.Entry<String, HashMap<String, hashNode>> min = null;
				double minTotal = 0;
				for(HashMap.Entry<String, HashMap<String, hashNode>> pos: nextPos.getValue().entrySet()){
					double total = 0;//total number of this pos given the previous pos with its tag
					for(HashMap.Entry<String, hashNode> yaBoi: pos.getValue().entrySet()){
						total+=yaBoi.getValue().value;
					}
					for(HashMap.Entry<String, hashNode> yaBoi:pos.getValue().entrySet()){
						yaBoi.getValue().value=yaBoi.getValue().value/total;//converts count of prev pos -> current pos to probability of the current pos occurring given the prev pos
					}
					if(min ==null){
						min = pos;
						minTotal = total;
					}
					else if(total<minTotal){
						min = pos;
						minTotal = total;
					}				
				}
				for(HashMap.Entry<String, hashNode> yaBoi:min.getValue().entrySet()){
					nextPosToPosToTag.get(nextPos.getKey()).get("OOV").put(yaBoi.getKey(), yaBoi.getValue());
				}
			
			}*/
			for(HashMap.Entry<String, HashMap<String, HashMap<String,hashNode>>>nextPos:nextPosToPosToTag.entrySet()){
				double numOovB =0;//number of OOV words tagged B-NP
				double numOovI =0;//number of OOV words tagged I-NP
				double numOovO =0;//number of OOV words tagged O
				double numOOV=0;
				HashMap.Entry<String, HashMap<String, hashNode>> min = null;
				double minTotal=0;
				for(HashMap.Entry<String, HashMap<String, hashNode>> currPos:nextPos.getValue().entrySet()){
					double total = 0;//total number of instances of words of a given part of speech
					for(HashMap.Entry<String, hashNode> yaBoi:currPos.getValue().entrySet()){
						total+=yaBoi.getValue().value;//increment total
					}
					if(!currPos.getValue().containsKey("B-NP"))
						currPos.getValue().put("B-NP", new hashNode());
					
					if(!currPos.getValue().containsKey("I-NP"))
						currPos.getValue().put("I-NP", new hashNode());
					
					if(!currPos.getValue().containsKey("O"))
						currPos.getValue().put("O", new hashNode());
					
					if(min == null){
						min = currPos;
						minTotal = total;
					}
					else if(total<minTotal){
						min = currPos;
						minTotal = total;					
					}
					
					for(HashMap.Entry<String, hashNode> yaBoi:currPos.getValue().entrySet()){
						yaBoi.getValue().value=yaBoi.getValue().value/total;//increment total
					}

				}
				nextPos.getValue().put("OOV", new HashMap<String, hashNode>());
				for(HashMap.Entry<String, hashNode> oov:min.getValue().entrySet()){
					nextPos.getValue().get("OOV").put(oov.getKey(), new hashNode());
					nextPos.getValue().get("OOV").get(oov.getKey()).value = oov.getValue().value;
				}
			}
			
			
			String toTagged = System.getProperty("user.dir");//build path to training corpus from cwd and args[0]
			if(toTagged.contains("\\"))
				toTagged+="\\";
			else
				toTagged+="/";
			toTagged+=args[1];
			
			File tagged = new File (toTagged);//get file and attempt to scan it.
			try {
				scan = new Scanner (tagged);
			} catch (FileNotFoundException e) {//if file is not found, terminate the program
				System.out.println("FATAL ERROR: file "+args[0]+" not found at "+toTagged);
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
			path2+="hybridOutput.chunk";
			
			try {//writes to the output file
				//File chunk = new File(path2);
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
				System.out.println(word);
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
				else if (pos.equals("CD")){
					int beginning = i;
					int ending = i;
					boolean beginningFound = false;
					boolean endingFound = false;
					int j = i+1;
					while(!endingFound){
						if((rawWords.get(j).split("\\s")[1].equals("CC")&&rawWords.get(j-1).split("\\s")[1].equals("CD")&&rawWords.get(j+1).split("\\s")[1].equals("CD"))||rawWords.get(j).split("\\s")[1].equals("NN")||rawWords.get(j).split("\\s")[1].equals("NNS")||rawWords.get(j).split("\\s")[1].equals("NNP")||rawWords.get(j).split("\\s")[1].equals("NNPS")||rawWords.get(j).split("\\s")[1].equals("CD")||rawWords.get(j).split("\\s")[1].equals("CD")||rawWords.get(j).split("\\s")[0].equals("%")){
							ending++;
							j++;
						}
						else
							endingFound = true;
					}
					j = i-1;
					while(!beginningFound){
						if(rawWords.get(j).split("\\s")[0].equals("to")&&rawWords.get(j-1).split("\\s")[0].equals("up")){
							beginning-=2;
							beginningFound = true;
						}
						else if(rawWords.get(j).split("\\s")[1].equals("#")||rawWords.get(j).split("\\s")[1].equals("PRP$")||rawWords.get(j).split("\\s")[1].equals("JJ")||rawWords.get(j).split("\\s")[1].equals("JJR")||rawWords.get(j).split("\\s")[1].equals("JJS")||rawWords.get(j).split("\\s")[1].equals("$")){
							j--;
							beginning--;
						}
						else if(rawWords.get(j).split("\\s")[1].equals("POS")||rawWords.get(j).split("\\s")[1].equals("DT")||rawWords.get(j).split("\\s")[0].equals("approximately")||rawWords.get(j).split("\\s")[0].equals("around")||rawWords.get(j).split("\\s")[0].equals("almost")||rawWords.get(j).split("\\s")[0].equals("about")){
							beginning--;
							beginningFound = true;
						}
						else if (rawWords.get(j).split("\\s")[0].equals("as")&&(rawWords.get(j-1).split("\\s")[0].equals("many")||rawWords.get(j-1).split("\\s")[0].equals("much"))&&rawWords.get(j-2).split("\\s")[0].equals("as")){
							beginning-=3;
							beginningFound = true;
						}

						else if (rawWords.get(j).split("\\s")[1].equals("VBN")&&j>0&&(rawWords.get(j-1).split("\\s+")[1].equals("JJR")||rawWords.get(j-1).split("\\s+")[1].equals("PRP$")||rawWords.get(j-1).split("\\s+")[1].equals("JJ")||rawWords.get(j-1).split("\\s+")[1].equals("DT")||rawWords.get(j-1).split("\\s+")[1].equals("RB"))){
							beginning--;
							j--;
						}
						else if (rawWords.get(j).split("\\s")[1].equals("VBN")&&j>1&&(rawWords.get(j-1).split("\\s+")[1].equals(",")&&(rawWords.get(j-1).split("\\s+")[1].equals("JJR")||rawWords.get(j-2).split("\\s+")[1].equals("PRP$")||rawWords.get(j-2).split("\\s+")[1].equals("JJ")||rawWords.get(j-2).split("\\s+")[1].equals("DT")||rawWords.get(j-2).split("\\s+")[1].equals("RB")))){
							beginning-=2;
							j-=2;
						}
						else if((rawWords.get(j).split("\\s")[1].equals("RBR")||rawWords.get(j).split("\\s")[0].equals("not"))&&rawWords.get(j+1).split("\\s+")[1].equals("JJ")){
							beginning--;
							j--;
						}
						
						else{
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
				//else if(pos.equals(arg0))
				else if(pos.equals("PRP")||pos.equals("WP")||word.equals("those")||pos.equals("WDT")){
					int beginning = i;
					//int ending = i;
					String nounGroupLine = rawWords.get(beginning).split("\\s+")[0]+"\tB-NP";
					outSentence.add(nounGroupLine);
				}
				else if(word.equals("anywhere")&&rawWords.get(i+1).split("\\s+")[0].equals("else")){
					outSentence.add("anywhere\tB-NP");
					outSentence.add("else\tI-NP");
					i++;
				}
				/*else if (word.equals("same")&&rawWords.get(i-1).split("\\s")[0].equals("the")){
					outSentence.remove(outSentence.size()-1);
					String nounGroupLine = "the\tB-NP";
					outSentence.add(nounGroupLine);
					nounGroupLine = "same\tI-NP";
					outSentence.add(nounGroupLine);
				}*/
				else if(pos.equals("NN")||pos.equals("NNS")||pos.equals("NNP")||pos.equals("NNPS")||pos.equals("CD")/*||pos.equals("PRP")*/||pos.equals("EX")||word.equals("counseling")||word.equals("Counseling")){
					int beginning = i;
					int ending = i;
					boolean endFound = false;
					int j = i+1;
					while(!endFound){
						String next=rawWords.get(j).split("\\s")[1];
						if(next.equals("NN")||next.equals("NNS")||next.equals("NNP")||next.equals("NNPS")||next.equals("CD")||(next.equals("CC")&&rawWords.get(j-1).split("\\s")[1].equals(rawWords.get(j+1).split("\\s")[1]))){
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
						if(j>2 &&prior.equals(rawWords.get(j-2).split("\\s+")[1])&&(rawWords.get(j-1).split("\\s+")[1].equals("CC")||rawWords.get(j-1).split("\\s+")[1].equals(","))){
							beginning-=2;
							j-=2;
						}
						else if(prior.equals("JJ")||prior.equals("JJR")||prior.equals("JJS")||prior.equals("PRP$")||prior.equals("RBS")||prior.equals("$")||prior.equals("RB")||prior.equals("NNP")||prior.equals("NNPS")){
							beginning--;
							j--;
						}
						else if (prior.equals("VBN")){//&&j>1&&(rawWords.get(j-1).split("\\s+")[1].equals(",")&&(rawWords.get(j-2).split("\\s+")[1].equals("JJR")||rawWords.get(j-2).split("\\s+")[1].equals("PRP$")||rawWords.get(j-2).split("\\s+")[1].equals("JJ")||rawWords.get(j-2).split("\\s+")[1].equals("DT")||rawWords.get(j-2).split("\\s+")[1].equals("RB")))){
							String t = statGuess(rawWords, j);
							if(t.equals("I-NP")){
								beginning--;
								j--;
							}
							else if(t.equals("B-NP")){
								beginning--;
								beginningFound = true;
							}
							else
								beginningFound = true;
							//beginning-=2;
							//j-=2;
						}
						else if (prior.equals("VBN")&&j>0&&(rawWords.get(j-1).split("\\s+")[1].equals("JJR")||rawWords.get(j-1).split("\\s+")[1].equals("PRP$")||rawWords.get(j-1).split("\\s+")[1].equals("JJ")||rawWords.get(j-1).split("\\s+")[1].equals("DT")||rawWords.get(j-1).split("\\s+")[1].equals("RB"))){
							beginning--;
							j--;
						}
						else if (prior.equals("VBG")){//&&j>0&&(rawWords.get(j-1).split("\\s+")[1].equals("DT")||rawWords.get(j-1).split("\\s+")[1].equals("JJR")||rawWords.get(j-1).split("\\s+")[1].equals("JJ")||rawWords.get(j-1).split("\\s+")[1].equals("PRP$"))){
							String t = statGuess(rawWords, j);
							if(t.equals("I-NP")){
								beginning--;
								j--;
							}
							else if(t.equals("B-NP")){
								beginning--;
								beginningFound = true;
							}
							else
								beginningFound = true;
						}
						else if((prior.equals("RBR")||pArray[0].equals("not"))&&rawWords.get(j+1).split("\\s+")[1].equals("JJ")){
							beginning--;
							j--;
						}
						else if((pArray[0].equals("Buying")||pArray[0].equals("buying"))&&rawWords.get(j+1).split("\\s+")[0].equals("income")){
							beginning--;
							j--;
						}
						else if(pArray[0].equals("counseling")||pArray[0].equals("Counseling")){
							beginning--;
							j--;
							if(rawWords.get(j).split("\\s+")[1].equals("NN")){
								beginning--;
								j--;
							}
						}
						
						else if (prior.equals("DT")||prior.equals("POS")){
							//j--;
							beginning--;
							if(!rawWords.get(j-1).split("\\s+")[1].equals("DT"))
								beginningFound = true;
							else
								j--;
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
		
		public static String statGuess(ArrayList<String> rawWords, int index){
			ArrayList<tagNode> possibleTags	= new ArrayList<tagNode>();
			tagNode bTag = new tagNode();
			bTag.tag = "B-NP";
			bTag.pos = rawWords.get(index).split("\\s+")[1];
			bTag.prevPos = rawWords.get(index-1).split("\\s+")[1];
			bTag.nextPos = rawWords.get(index+1).split("\\s+")[1];
			bTag.nextTag = "I-NP";
			bTag.prevTag = null;
			double bChance1 = posToWordToTag.get(bTag.pos).get("OOV").get("B-NP").value;
			if(posToWordToTag.get(bTag.pos).containsKey(rawWords.get(index).split("\\s+")[0]))
				bChance1 = posToWordToTag.get(bTag.pos).get(rawWords.get(index).split("\\s+")[0]).get("B-NP").value;
			
			double bChance2;
			if(nextPosToPosToTag.get(rawWords.get(index+1).split("\\s+")[1]).containsKey(bTag.pos))
				bChance2 = nextPosToPosToTag.get(rawWords.get(index+1).split("\\s+")[1]).get(bTag.pos).get("B-NP").value;
			else
				bChance2 = nextPosToPosToTag.get(rawWords.get(index+1).split("\\s+")[1]).get("OOV").get("B-NP").value;
			
			double bMaxGivenPossiblePrevTags=0;
			if(posToTagToPosToTag.get(bTag.prevPos).containsKey("O")&&posToTagToPosToTag.get(bTag.prevPos).get("O").containsKey(bTag.pos)){
				bMaxGivenPossiblePrevTags=posToTagToPosToTag.get(bTag.prevPos).get("O").get(bTag.pos).get("B-NP").value;
				bTag.prevTag = "O";
			}

			if(posToTagToPosToTag.get(bTag.prevPos).containsKey("I-NP")&&posToTagToPosToTag.get(bTag.prevPos).get("I-NP").containsKey(bTag.pos)&&posToTagToPosToTag.get(bTag.prevPos).get("I-NP").get(bTag.pos).get("B-NP").value>bMaxGivenPossiblePrevTags){
				bMaxGivenPossiblePrevTags=posToTagToPosToTag.get(bTag.prevPos).get("I-NP").get(bTag.pos).get("B-NP").value;
				bTag.prevTag = "I-NP";
			}
			
			if(posToTagToPosToTag.get(bTag.prevPos).containsKey("B-NP")&&posToTagToPosToTag.get(bTag.prevPos).get("B-NP").containsKey(bTag.pos)&&posToTagToPosToTag.get(bTag.prevPos).get("B-NP").get(bTag.pos).get("B-NP").value>bMaxGivenPossiblePrevTags){
				bMaxGivenPossiblePrevTags=posToTagToPosToTag.get(bTag.prevPos).get("B-NP").get(bTag.pos).get("B-NP").value;
				bTag.prevTag = "B-NP";
			}
			
			if(bTag.prevTag==null){
				bMaxGivenPossiblePrevTags=tagToPosToTag.get("O").get(bTag.pos).get("B-NP").value;
				if(tagToPosToTag.get("I-NP").get(bTag.pos).get("B-NP").value>bMaxGivenPossiblePrevTags){
					bMaxGivenPossiblePrevTags=tagToPosToTag.get("I-NP").get(bTag.pos).get("B-NP").value;
					bTag.prevTag = "I-NP";
				}
				if(tagToPosToTag.get("B-NP").get(bTag.pos).get("B-NP").value>bMaxGivenPossiblePrevTags){
					bMaxGivenPossiblePrevTags=tagToPosToTag.get("B-NP").get(bTag.pos).get("B-NP").value;
					bTag.prevTag = "B-NP";
				}
			}
			bTag.probability = bChance1*bChance2*bMaxGivenPossiblePrevTags;
			
			tagNode iTag = new tagNode();
			iTag.tag = "I-NP";
			iTag.pos = rawWords.get(index).split("\\s+")[1];
			iTag.prevPos = rawWords.get(index-1).split("\\s+")[1];
			iTag.nextPos = rawWords.get(index+1).split("\\s+")[1];
			iTag.nextTag = "I-NP";

			double iChance1 = posToWordToTag.get(iTag.pos).get("OOV").get("I-NP").value;
			if(posToWordToTag.get(iTag.pos).containsKey(rawWords.get(index).split("\\s+")[0]))
				iChance1 = posToWordToTag.get(iTag.pos).get(rawWords.get(index).split("\\s+")[0]).get("I-NP").value;
			
			double iChance2;
			if(nextPosToPosToTag.get(rawWords.get(index+1).split("\\s+")[1]).containsKey(iTag.pos))
				iChance2 = nextPosToPosToTag.get(rawWords.get(index+1).split("\\s+")[1]).get(iTag.pos).get("I-NP").value;
			else
				iChance2 = nextPosToPosToTag.get(rawWords.get(index+1).split("\\s+")[1]).get("OOV").get("I-NP").value;
			
			double iMaxGivenPossiblePrevTags=0;
			if(posToTagToPosToTag.get(iTag.prevPos).containsKey("O")&&posToTagToPosToTag.get(iTag.prevPos).get("O").containsKey(iTag.pos)){
				iMaxGivenPossiblePrevTags=posToTagToPosToTag.get(iTag.prevPos).get("O").get(iTag.pos).get("I-NP").value;
				iTag.prevTag = "O";
			}

			if(posToTagToPosToTag.get(iTag.prevPos).containsKey("I-NP")&&posToTagToPosToTag.get(iTag.prevPos).get("I-NP").containsKey(iTag.pos)&&posToTagToPosToTag.get(iTag.prevPos).get("I-NP").get(iTag.pos).get("I-NP").value>iMaxGivenPossiblePrevTags){
				iMaxGivenPossiblePrevTags=posToTagToPosToTag.get(iTag.prevPos).get("I-NP").get(iTag.pos).get("I-NP").value;
				iTag.prevTag = "I-NP";
			}
			
			if(posToTagToPosToTag.get(iTag.prevPos).containsKey("B-NP")&&posToTagToPosToTag.get(iTag.prevPos).get("B-NP").containsKey(iTag.pos)&&posToTagToPosToTag.get(iTag.prevPos).get("B-NP").get(iTag.pos).get("I-NP").value>iMaxGivenPossiblePrevTags){
				iMaxGivenPossiblePrevTags=posToTagToPosToTag.get(iTag.prevPos).get("B-NP").get(iTag.pos).get("I-NP").value;
				iTag.prevTag = "B-NP";
			}
			
			if(iTag.prevTag==null){
				iMaxGivenPossiblePrevTags=tagToPosToTag.get("O").get(iTag.pos).get("I-NP").value;
				if(tagToPosToTag.get("I-NP").get(iTag.pos).get("I-NP").value>iMaxGivenPossiblePrevTags){
					iMaxGivenPossiblePrevTags=tagToPosToTag.get("I-NP").get(iTag.pos).get("I-NP").value;
					iTag.prevTag = "I-NP";
				}
				if(tagToPosToTag.get("B-NP").get(iTag.pos).get("I-NP").value>iMaxGivenPossiblePrevTags){
					iMaxGivenPossiblePrevTags=tagToPosToTag.get("B-NP").get(iTag.pos).get("I-NP").value;
					iTag.prevTag = "B-NP";
				}
			}
			iTag.probability = iChance1*iChance2*iMaxGivenPossiblePrevTags;
			

			tagNode oTag = new tagNode();
			oTag.tag = "O";
			oTag.pos = rawWords.get(index).split("\\s+")[1];
			oTag.prevPos = rawWords.get(index-1).split("\\s+")[1];
			oTag.nextPos = rawWords.get(index+1).split("\\s+")[1];
			oTag.nextTag = "B-NP";
			oTag.prevTag = null;
			double oChance1 = posToWordToTag.get(oTag.pos).get("OOV").get("B-NP").value;
			if(posToWordToTag.get(oTag.pos).containsKey(rawWords.get(index).split("\\s+")[0]))
				bChance1 = posToWordToTag.get(oTag.pos).get(rawWords.get(index).split("\\s+")[0]).get("B-NP").value;
			
			double oChance2;
			if(nextPosToPosToTag.get(rawWords.get(index+1).split("\\s+")[1]).containsKey(oTag.pos))
				oChance2 = nextPosToPosToTag.get(rawWords.get(index+1).split("\\s+")[1]).get(oTag.pos).get("O").value;
			else
				oChance2 = nextPosToPosToTag.get(rawWords.get(index+1).split("\\s+")[1]).get("OOV").get("O").value;
			
			double oMaxGivenPossiblePrevTags=0;
			if(posToTagToPosToTag.get(oTag.prevPos).containsKey("O")&&posToTagToPosToTag.get(oTag.prevPos).get("O").containsKey(oTag.pos)){
				oMaxGivenPossiblePrevTags=posToTagToPosToTag.get(oTag.prevPos).get("O").get(oTag.pos).get("O").value;
				oTag.prevTag = "O";
			}

			if(posToTagToPosToTag.get(oTag.prevPos).containsKey("I-NP")&&posToTagToPosToTag.get(oTag.prevPos).get("I-NP").containsKey(oTag.pos)&&posToTagToPosToTag.get(oTag.prevPos).get("I-NP").get(oTag.pos).get("O").value>oMaxGivenPossiblePrevTags){
				oMaxGivenPossiblePrevTags=posToTagToPosToTag.get(oTag.prevPos).get("I-NP").get(oTag.pos).get("O").value;
				oTag.prevTag = "I-NP";
			}
			
			if(posToTagToPosToTag.get(oTag.prevPos).containsKey("B-NP")&&posToTagToPosToTag.get(oTag.prevPos).get("B-NP").containsKey(oTag.pos)&&posToTagToPosToTag.get(oTag.prevPos).get("B-NP").get(oTag.pos).get("O").value>oMaxGivenPossiblePrevTags){
				oMaxGivenPossiblePrevTags=posToTagToPosToTag.get(oTag.prevPos).get("B-NP").get(oTag.pos).get("O").value;
				oTag.prevTag = "B-NP";
			}
			if(oTag.prevTag==null){
				oMaxGivenPossiblePrevTags=tagToPosToTag.get("O").get(iTag.pos).get("O").value;
				if(tagToPosToTag.get("I-NP").get(iTag.pos).get("O").value>iMaxGivenPossiblePrevTags){
					oMaxGivenPossiblePrevTags=tagToPosToTag.get("I-NP").get(iTag.pos).get("O").value;
					oTag.prevTag = "I-NP";
				}
				if(tagToPosToTag.get("B-NP").get(iTag.pos).get("O").value>iMaxGivenPossiblePrevTags){
					oMaxGivenPossiblePrevTags=tagToPosToTag.get("B-NP").get(iTag.pos).get("O").value;
					oTag.prevTag = "B-NP";
				}
			}
			oTag.probability = oChance1*oChance2*oMaxGivenPossiblePrevTags;
			
			String maxTag = "B-NP";
			double maxProb = bTag.probability;
			if(iTag.probability>maxProb){
				maxTag = "I-NP";
				maxProb = iTag.probability;
			}
			if(oTag.probability>maxProb){
				maxTag = "O";
				maxProb = oTag.probability;
			}
			return maxTag;
			
		}
		
}

class hashNode{
	double value;
	hashNode(){
		value = 0.0;
	}
}

class tagNode{
	String nextPos;
	String nextTag;
	String prevPos;
	String pos;
	String tag;
	String prevTag;
	double probability;
	tagNode backPointer;
	tagNode frontPointer;
}

