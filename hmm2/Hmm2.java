package hmm2;

import java.util.*;


import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Hmm2 {

	static HashMap<String,HashMap<String, HashMap<String, hashNode>>> posToWordToTag  = new HashMap<String, HashMap<String, HashMap<String, hashNode>>>();
	static HashMap<String, HashMap<String,HashMap<String,HashMap<String, hashNode>>>> posToTagToPosToTag = new HashMap<String, HashMap<String,HashMap<String,HashMap<String, hashNode>>>>();

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
		
	/*
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
				//System.out.println("OOV");
				if(word.getValue().containsKey("B-NP")&&word.getValue().get("B-NP").value<=2){
					numOovB++;
					numOOV++;
				}
				if(word.getValue().containsKey("I-NP")&&word.getValue().get("I-NP").value<=2){
					numOovI++;
					numOOV++;
				}
				if (word.getValue().containsKey("O")&&word.getValue().get("O").value<=2){
					numOovO++;
					numOOV++;
				}
			}
			//System.out.println(word.getKey()+": "+total);

			for(HashMap.Entry<String, hashNode> yaBoi:word.getValue().entrySet()){
				yaBoi.getValue().value=yaBoi.getValue().value/total;//increment total
			}

		}
			System.out.println(pos.getKey()+" B: " +numOovB);
		System.out.println(pos.getKey()+" I: " +numOovI);
		System.out.println(pos.getKey()+" O: " +numOovO);
		pos.getValue().get("OOV").get("B-NP").value=numOovB;//sets OOV count to value explained above
		pos.getValue().get("OOV").get("I-NP").value=numOovI;
		pos.getValue().get("OOV").get("O").value=numOovO;
		
		for(HashMap.Entry<String, hashNode> yaBoi:pos.getValue().get("OOV").entrySet()){
			yaBoi.getValue().value=yaBoi.getValue().value/numOOV;//converts count to probability of the tag given the pos and the word
		}
	}
		*/
		
		//the gist of the following code (that is, how to read from and edit a file
		//line by line) was taken from stackoverflow user maltesmann 's response here:
		//https://stackoverflow.com/questions/8563294/modifying-existing-file-content-in-java
		
		ArrayList<String> newLines = new ArrayList<>();//arrayList of all the lines of output
		newLines.add("");
		String path2 = System.getProperty("user.dir");//builds path for test corpus out of the cwd and args[1]
		if(path2.contains("\\"))
			path2+="\\";
		else
			path2+="/";
		path2+=args[1];
		File testCorpus = new File (path2);
		try {
			scan = new Scanner (testCorpus);
		} catch (FileNotFoundException e) {//if the file is not found, an error is printed and the program terminates
			e.printStackTrace();
			System.out.println("FATAL ERROR: file "+args[1]+" not found at "+path2);
			return;
		}
		
		/*while(scan.hasNextLine()){//builds and tags all sentences
			//System.out.println(1);
			ArrayList<String> nonTaggedSentence= new ArrayList<String>();//builds next non-tagged sentence from the test corpus
			nonTaggedSentence.add("stopSentence"+"\t"+"stopSentence");
			String tmp=null;
			while(!(tmp = scan.nextLine()).equals("")){
				nonTaggedSentence.add(tmp);
				//System.out.println(tmp);
			}
			nonTaggedSentence.add("stopSentence"+"\t"+"stopSentence");
			
			ArrayList<String> taggedSentence = tagSentence(nonTaggedSentence);//tags the sentence*/
		while(scan.hasNext()){
			ArrayList<String> nonTaggedSentence= new ArrayList<String>();//builds next non-tagged sentence from the test corpus
			
			nonTaggedSentence.add("stopSentence\tstopSentence");//add the sentence's start state 
			String tmp=null;
			while(!(tmp = scan.nextLine()).equals("")){//add tokens until the end of the sentence
				nonTaggedSentence.add(tmp);
			}
			nonTaggedSentence.add("stopSentence\tstopSentence");//add the sentence's stop state
			
			if(nonTaggedSentence.size()>2){
				ArrayList<String>taggedSentence = tagSentence(nonTaggedSentence);//tag the sentence
			
				for(int i = 0; i<taggedSentence.size();i++)//adds all lines of the newly tagged sentence to the output list
					newLines.add(taggedSentence.get(i));
			}
		}
		
		String path3 = System.getProperty("user.dir");//builds path to output file (ALWAYS ouput.pos in the cwd)
		if(path3.contains("\\"))
			path3+="\\";
		else
			path3+="/";
		path3+="hmmOut2.chunk";
		
		try {//writes to the output file
			File out = new File(path3);
			Files.write(Paths.get(path3), newLines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			
			e.printStackTrace();
			System.out.println("FATAL ERROR: Can't write to output.pos at location "+path3);
			scan.close();
			return;
		}
		scan.close();
	}//end tagging
	
	static ArrayList<String>tagSentence(ArrayList<String> rawWords){//function which takes an untagged sentence as an arraylist of strings (tokens) and returns a tagged version of that sentence as an arrayList of strings
		
		ArrayList<ArrayList<tagNode>> tagging = new ArrayList<ArrayList<tagNode>>();//tagging.get(i) will contain the list of possible tags for the token at rawWords.get(i)
		tagging.add(new ArrayList<tagNode>());//manually make the node for the start state
		tagging.get(0).add(new tagNode());
		
		tagging.get(0).get(0).pos = "stopSentence";
		tagging.get(0).get(0).probability = 1;
		tagging.get(0).get(0).tag = "O";
		
		for(int i=1; i<rawWords.size();i++){//run through rest of the sentence
			System.out.println(rawWords.get(i));
			tagging.add(new ArrayList<tagNode>());//create new list of the possible tags for the word at rawWords.get(i)
			HashMap <String, ArrayList<tagNode>> possibleTag = new HashMap<String, ArrayList<tagNode>>();//this maps a (key) part of speech to a list of the tags which map the current word to the given part of speech. Only the most likely of these, given the possible previous states, will be entered into tagging
			
			for(int j=0; j<tagging.get(i-1).size(); j++){//runs through the possible tags for the previous word
				
				tagNode prev = tagging.get(i-1).get(j);
				
				String currWord = rawWords.get(i).split("\\s+")[0];
				String currPos = rawWords.get(i).split("\\s+")[1];
				
				if(currWord.equals(""))//manually set current word when it's the end state
					currWord="stopSentence";
				if(posToTagToPosToTag.get(prev.pos).containsKey(prev.tag)){
					if(posToTagToPosToTag.get(prev.pos).get(prev.tag).containsKey(currPos)){
						for(HashMap.Entry<String, hashNode> entry:posToTagToPosToTag.get(prev.pos).get(prev.tag).get(currPos).entrySet()){//runs through all possible current pos given the prev pos
							if(posToWordToTag.get(currPos).containsKey(currWord)){
								if(posToWordToTag.get(currPos).get(currWord).containsKey(entry.getKey())){//if the current word can be linked to a pos which would follow the pos of the previous word
									tagNode possibility = new tagNode();//makes new node to catalog possible tag from previous possible pos
									possibility.prevTag = prev.tag;
									possibility.tag = entry.getKey();
									possibility.pos = currPos;

									double chance1 = tagToPosToTag.get(prev.tag).get(possibility.pos).get(possibility.tag).value;;
									if(posToTagToPosToTag.containsKey(prev.pos)){
										if(posToTagToPosToTag.get(prev.pos).containsKey(prev.tag)){
											if(posToTagToPosToTag.get(prev.pos).get(prev.tag).containsKey(possibility.pos))
												chance1 = posToTagToPosToTag.get(prev.pos).get(prev.tag).get(possibility.pos).get(possibility.tag).value;
										}
									}//calculate probability of this tag


									double chance2 = posToWordToTag.get(possibility.pos).get(currWord).get(possibility.tag).value;//probability that this word is tagged with this
									double chance3 = prev.probability;
									if(i<rawWords.size()-1){
										double chance4;
										if(nextPosToPosToTag.get(rawWords.get(i+1).split("\\s+")[1]).containsKey(currPos))
											chance4 = nextPosToPosToTag.get(rawWords.get(i+1).split("\\s+")[1]).get(possibility.pos).get(possibility.tag).value;
										else
											chance4 = nextPosToPosToTag.get(rawWords.get(i+1).split("\\s+")[1]).get("OOV").get(possibility.tag).value;
										possibility.probability = chance1*chance2*chance3*chance4;
									}
									else
										possibility.probability = chance1*chance2*chance3;

									if(!possibleTag.containsKey(possibility.tag)){//enter into possibleTag
										possibleTag.put(possibility.tag,new ArrayList<tagNode>());
									}
									possibleTag.get(possibility.tag).add(possibility);
								}
							}
						}//end for each loop
					}
				}//end j loop (end enumeration of possibilities for ith  word in the sentence)

				else if(tagToPosToTag.get(prev.tag).containsKey(currPos)){
					for(HashMap.Entry<String, hashNode> entry:tagToPosToTag.get(prev.tag).get(currPos).entrySet()){//runs through all possible current pos given the prev pos

						if(posToWordToTag.get(currPos).containsKey(currWord)){//if the current word can be linked to a pos which would follow the pos of the previous word
							//for(HashMap.Entry<String,hashNode> entry:posToWordToTag.get(currPos).get(currWord).entrySet()){
							if(posToWordToTag.get(currPos).get(currWord).containsKey(entry.getKey())){
								tagNode possibility = new tagNode();//makes new node to catalog possible tag from previous possible pos
								possibility.prevTag = prev.tag;
								possibility.tag = entry.getKey();
								possibility.pos = currPos;
								//System.out.println(possibility.tag);


								double chance1 = tagToPosToTag.get(prev.tag).get(currPos).get(possibility.tag).value;//calculate probability of this tag
								double chance2 = posToWordToTag.get(possibility.pos).get(currWord).get(possibility.tag).value;//probability that this word is tagged with this
								double chance3 = prev.probability;
								double chance4 = nextPosToPosToTag.get(rawWords.get(i+1).split("\\s+")[1]).get(possibility.pos).get(possibility.tag).value;
								possibility.probability = chance1*chance2*chance3*chance4;

								if(!possibleTag.containsKey(possibility.tag)){//enter into possibleTag
									possibleTag.put(possibility.tag,new ArrayList<tagNode>());
								}
								possibleTag.get(possibility.tag).add(possibility);
							}//end if
						}//end if 					
					}//end for each loop
				}//end if
			}
			
			
			if(possibleTag.isEmpty()){//CASE WHERE THE CURRENT WORD IS OOV
				
				for(int j = 0; j<tagging.get(i-1).size(); j++){
					tagNode prev = tagging.get(i-1).get(j);
					String currPos = rawWords.get(i).split("\\s+")[1];
					
					//if(posToTagToPosToTag.get(prev.pos).containsKey(prev.tag)){
					if(posToTagToPosToTag.get(prev.pos).get(prev.tag).containsKey(currPos)){
						for(HashMap.Entry<String, hashNode> entry:posToTagToPosToTag.get(prev.pos).get(prev.tag).get(currPos).entrySet()){
							if(posToWordToTag.get(rawWords.get(i).split("\\s+")[1]).containsKey("OOV")){//if the current word can be linked to a pos which would follow the pos of the previous word
								tagNode possibility = new tagNode();
								possibility.prevTag = prev.tag;
								possibility.tag = entry.getKey();
								possibility.pos = rawWords.get(i).split("\\s+")[1];
								double chance1=tagToPosToTag.get(prev.tag).get(possibility.pos).get(possibility.tag).value;;
								if(posToTagToPosToTag.containsKey(prev.pos)){
									if(posToTagToPosToTag.get(prev.pos).containsKey(prev.tag)){
										if(posToTagToPosToTag.get(prev.pos).get(prev.tag).containsKey(possibility.pos))
											chance1 = posToTagToPosToTag.get(prev.pos).get(prev.tag).get(possibility.pos).get(possibility.tag).value;
									}
								}//calculate probability of this tag
								else if (tagToPosToTag.get(prev.tag).containsKey(possibility.pos)){
									if(tagToPosToTag.get(prev.tag).containsKey(possibility.pos))
										chance1 = tagToPosToTag.get(prev.tag).get(possibility.pos).get(possibility.tag).value;
								}

								double chance2 = posToWordToTag.get(possibility.pos).get("OOV").get(possibility.tag).value;//probability that this word is tagged with this
								double chance3 = prev.probability;
								if(i<rawWords.size()-1){
									double chance4;
									if(nextPosToPosToTag.get(rawWords.get(i+1).split("\\s+")[1]).containsKey(currPos))
										chance4 = nextPosToPosToTag.get(rawWords.get(i+1).split("\\s+")[1]).get(possibility.pos).get(possibility.tag).value;
									else
										chance4 = nextPosToPosToTag.get(rawWords.get(i+1).split("\\s+")[1]).get("OOV").get(possibility.tag).value;
									possibility.probability = chance1*chance2*chance3*chance4;
								}
								else
									possibility.probability = chance1*chance2*chance3;

								if(!possibleTag.containsKey(possibility.pos)){//enter into hashMap
									possibleTag.put(possibility.pos,new ArrayList<tagNode>());
								}
								possibleTag.get(possibility.pos).add(possibility);
							}
						}
					}
					//}
					else{
						for(HashMap.Entry<String, hashNode> entry:tagToPosToTag.get(prev.tag).get(rawWords.get(i).split("\\s+")[1]).entrySet()){
							if(posToWordToTag.get(rawWords.get(i).split("\\s+")[1]).containsKey("OOV")){//if the current word can be linked to a pos which would follow the pos of the previous word
								tagNode possibility = new tagNode();
								possibility.prevTag = prev.tag;
								possibility.tag = entry.getKey();
								possibility.pos = rawWords.get(i).split("\\s+")[1];

								double chance1 = tagToPosToTag.get(prev.tag).get(possibility.pos).get(possibility.tag).value;//calculate probability of this tag
								double chance2 = posToWordToTag.get(possibility.pos).get("OOV").get(possibility.tag).value;//probability that this word is tagged with this
								double chance3 = prev.probability;
								if(i<rawWords.size()-1){
									double chance4;
									if(nextPosToPosToTag.get(rawWords.get(i+1).split("\\s+")[1]).containsKey(currPos))
										chance4 = nextPosToPosToTag.get(rawWords.get(i+1).split("\\s+")[1]).get(possibility.pos).get(possibility.tag).value;
									else
										chance4 = nextPosToPosToTag.get(rawWords.get(i+1).split("\\s+")[1]).get("OOV").get(possibility.tag).value;
									possibility.probability = chance1*chance2*chance3*chance4;
								}
								else
									possibility.probability = chance1*chance2*chance3;

								if(!possibleTag.containsKey(possibility.pos)){//enter into hashMap
									possibleTag.put(possibility.pos,new ArrayList<tagNode>());
								}
								possibleTag.get(possibility.pos).add(possibility);
							}
						}
					}

				}
			}
			
			
			//NOW: FIGURE OUT WHICH NODE IN tagging(i) WITH A GIVEN POS HAS THE HIGHEST PROBABILITY AMONG ALL NODES WITH SUCH A POS
			//DELETE ALL OTHER NODES WITH POS POS
			//SET BACKPOINTER TO THE NODE IN tagging(i-1) WHOSE POS IS PREVPOS
			for(HashMap.Entry<String, ArrayList<tagNode>> maybe:possibleTag.entrySet()){
				tagNode max = maybe.getValue().get(0);
				for(int findMax=0; findMax<maybe.getValue().size();findMax++){//LOCATE THE MOST PROBABLE NODE FOR THIS POS
					tagNode tmp = maybe.getValue().get(findMax);
					if(max.probability<tmp.probability)
						max = tmp;
				}//max should now have been found
				boolean pointing = false;
				int findPrev = 0;
				while(pointing==false && findPrev<tagging.get(i-1).size()){//find and set max.backPointer
					if(tagging.get(i-1).get(findPrev).tag.equals(max.prevTag)){
						max.backPointer=tagging.get(i-1).get(findPrev);
						pointing = true;
					}
					findPrev++;
				}
				tagging.get(i).add(max);
				
			}
			
		}//end word
		
		ArrayList<tagNode> backwardSentence = new ArrayList<tagNode>();//build a backward version of the final sentence
		tagNode n = tagging.get(tagging.size()-1).get(0);
		backwardSentence.add(n);
		tagNode prevN=null;
		while(n.backPointer!=null){
			backwardSentence.add(n.backPointer);
			if(prevN!=null)
				n.frontPointer = prevN;//set front pointers so sentence can be read in correct order
			prevN=n;
			n=n.backPointer;
		}
		System.out.println(backwardSentence.size());
		n=backwardSentence.get(backwardSentence.size()-1);
		n.frontPointer=backwardSentence.get(backwardSentence.size()-2);//set forward pointer of last node
		
		ArrayList <String> finalSentence=new ArrayList<String>();
		
		for(int i=0; i<rawWords.size(); i++){//build final sentence by tagging all the original words with their most likely pos
			if(!n.pos.equals("stopSentence")){
				String s = rawWords.get(i).split("\\s+")[0];
				s+= "\t"+n.tag;
				finalSentence.add(s);
			}
			else
				finalSentence.add("");
			n=n.frontPointer;
		}
		
		finalSentence.remove(0);//prevent doubling all instances of end of sentence
		return finalSentence;
		
	}//end tagSentence
	
}//end class hmm

class hashNode{
	double value;
	hashNode(){
		value = 0.0;
	}
}

class tagNode{
	String prevPos;
	String pos;
	String tag;
	String prevTag;
	double probability;
	tagNode backPointer;
	tagNode frontPointer;
}