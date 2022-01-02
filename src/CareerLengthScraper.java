import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CareerLengthScraper {
    // THE GENERAL GIST:
    // 1. Split the 80sFootballerList into two parts: name and votes.
    // 2. Store the names into some data structure (an ArrayList for ease).
    // 3. Iterate through the JSON file using the names in the ArrayList.
    // 4. Scrape through each name's Wikipedia page to find the career length of the footballer in question.
        // 4a. This step might be a bit more complex.

    public static void main(String[] args) throws IOException, ParseException {
        BufferedReader reader;
        ArrayList<String> nameList = new ArrayList<>(); // The data structure for step 2.

        try {
            reader = new BufferedReader(new FileReader("/Users/asadbekshamsiev/Desktop/FootballFunProject/80sFootballerList.txt"));
            String line = reader.readLine();
            while (line != null) { // Successfully adds names to ArrayList.
                String[] arr = line.split(": ");
                nameList.add(arr[0]);

                line = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }

        JSONParser jsonP = new JSONParser(); // https://www.youtube.com/watch?v=4gWPCaMdQRI
        Object obj = null;

        try (FileReader fReader = new FileReader("80sJSON.json")) {
            obj = jsonP.parse(fReader);
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }

        JSONObject jObj = (JSONObject) obj;

        // Step 3.
//        for (String name: nameList) { // Successfully iterates through the nameList; if you use jObj.get(name), you access the JSON file.
//            String arr[] = jObj.get(name).toString().split(",");
//            String wikiLink = arr[1]; // Leaves me this format: ("wiki":"https:\/\/it.wikipedia.org\/wiki\/Roberto_Mancini")
//
//            wikiLink = wikiLink.replaceFirst("wiki", "").replace("\"", "").replaceAll("\\\\","").substring(1); // Generates all 162 links.
//
//            if (name.equals("Míchel")) {
//                wikiLink = "https://en.wikipedia.org/wiki/M%C3%ADchel_(footballer,_born_1963)"; // Michel is a special case.
//            } else if (name.equals("Gary Stevens")) {
//                wikiLink = "https://en.wikipedia.org/wiki/Gary_Stevens_(footballer,_born_1962)"; // Stevens is also a special case.
//            }
//
//            Document doc = Jsoup.connect(wikiLink).get();
////            Elements body = doc.select("table.infobox.vcard tr").select("tbody"); // Grabs the senior career table of the wikipedia pages.
//            Elements body = doc.select("div#content.mw-body"); // Grabs the senior career table of the wikipedia pages.
//
//            System.out.println(body.toString());
//        }

//        Document doc = Jsoup.connect("https://en.wikipedia.org/wiki/Michel_Platini").get();

        Document doc = Jsoup.connect("https://en.wikipedia.org/wiki/Kenny_Dalglish").get();

        Elements body = doc.select("div#content.mw-body").select("div#bodyContent.vector-body")
                .select("div#mw-content-text.mw-body-content.mw-content-ltr").select("div.mw-parser-output")
                .select("table.infobox.vcard"); // https://gyazo.com/49c12c47d1410bb8161cb3fe5b07db43: this is where this path leads you.

        for (Element e : body.select("tr")) {
            System.out.println(e.toString());
        }

//        System.out.println(body.toString());

    }
}
