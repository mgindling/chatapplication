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
            //BufferedReader br = new BufferedReader(fr); //Needs to flush + string wasn't being modified back at the server + I like scanner better ;).

            String filterWord; //Current word that is being searched for
            String filterer; //A string that holds the resulted of a filtering.
            while (s.hasNextLine()) {
                filterer = "";
                filterWord = s.nextLine().toUpperCase();

                if (msg.toUpperCase().contains(filterWord)) { //If the message contains the searched-for word...
                    for (int i = 0; i < filterWord.length(); i++) { //The program loops for as long as the word, making a new string with only *s.
                        filterer += "*";
                    }
                    msg = msg.replaceAll("(?i)" + filterWord, filterer); //The bad word is then replaced with the asterisks.
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return msg;
    }
}
