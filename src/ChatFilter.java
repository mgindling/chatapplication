package src; //Probably should be removed at the end of programming.

import java.io.*;
import java.util.Scanner;

public class ChatFilter {

    private String badWordsFileName;

    //Constructor for ChatFilter. Initializes the name of the bad words file.
    public ChatFilter(String badWordsFileName) {
        this.badWordsFileName = badWordsFileName;
    }

    //Filters out any words in the given text file (badWordsFileName) and replaces them with asterisks corresponding to the number of letters
    public String filter(String msg) {
        try {
            FileInputStream fis = new FileInputStream(badWordsFileName);
            Scanner s = new Scanner(fis);

            //FileReader fr = new FileReader(badWordsFileName);
            //BufferedReader br = new BufferedReader(fr); //Needs to flush.

            String filterWord;
            String filterer;
            while ((filterWord = s.hasNextLine()) != null) {
                filterer = "";

                if (msg.contains(filterWord)) {
                    for (int i = 0; i < filterWord.length(); i++) {
                        filterer += "*";
                    }
                    msg = msg.replace(filterWord, filterer);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return msg;
    }
}
