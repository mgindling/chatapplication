import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ChatFilter {

    private String badWordsFileName;

    public ChatFilter(String badWordsFileName) {
        this.badWordsFileName = badWordsFileName;
    }

    //filters out any words in the given text file (badWordsFileName) and replaces them with asterisks corresponding to the number of letters
    public String filter(String msg) {
        try {
            FileReader fr = new FileReader(badWordsFileName);
            BufferedReader br = new BufferedReader(fr);

            String filterWord;
            String filterer;
            while ((filterWord = br.readLine()) != null) {
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
