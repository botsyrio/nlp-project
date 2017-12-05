package baselineSys;
import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
public class baseline {
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
		path2+="output.chunk";
		
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
		String lastTag = new String();
		ArrayList<String> outSentence = new ArrayList<String>();
		for(int i = 0; i<rawWords.size(); i++){
			String[] entry = rawWords.get(i).split("\\s+");
			String word = entry[0];
			String pos = entry[1];
			String tag;
			String line = word+"\t";
			if(pos.equals("NN")||pos.equals("JJ")||pos.equals("NNS")||pos.equals("NNP")||pos.equals("NNPS")||pos.equals("JJR")||pos.equals("JJS")||pos.equals("DT")){
				if(lastTag.equals("B-NP")||lastTag.equals("I-NP"))
					tag = "I-NP";
				else
					tag = "B-NP";
			}
			else
				tag = "O";
			
			line+=tag;
			outSentence.add(line);
			lastTag = tag;
		}
		outSentence.remove(0);
		outSentence.remove(outSentence.size()-1);
		outSentence.add("");
		return outSentence;
	}
}
