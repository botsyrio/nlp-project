package errorFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Sniffin {
	public static void main (String[] args){

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
		

		String path2 = System.getProperty("user.dir");//build path to training corpus from cwd and args[0]
		if(path.contains("\\"))
			path2+="\\";
		else
			path2+="/";
		path2+=args[1];
		
		File training2 = new File (path2);//get file and attempt to scan it.
		Scanner scan2;
		try {
			scan2 = new Scanner (training2);
		} catch (FileNotFoundException e) {//if file is not found, terminate the program
			System.out.println("FATAL ERROR: file "+args[1]+" not found at "+path2);
			e.printStackTrace();

			scan.close();
			return;
		}
		
		ArrayList<String> out = new ArrayList<String>();
		int i = 1;
		while (scan.hasNext()){
			if(!scan.nextLine().equals(scan2.nextLine()))
				out.add("Error on line "+i);
			i++;
		}
		
		String path3 = System.getProperty("user.dir");//builds path to output file (ALWAYS ouput.pos in the cwd)
		if(path3.contains("\\"))
			path3+="\\";
		else
			path3="/";
		path3+="errors.chunk";
		
		try {//writes to the output file
			File chunk = new File(path3);
			Files.write(Paths.get(path3), out, StandardCharsets.UTF_8);
		} catch (IOException e) {
			
			e.printStackTrace();
			System.out.println("FATAL ERROR: Can't write to output.pos at location "+path2);
			//scan.close();

			scan.close();
			scan2.close();
			return;
		}
		scan.close();
		scan2.close();
	}
}
