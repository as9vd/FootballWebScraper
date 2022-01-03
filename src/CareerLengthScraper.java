import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jdk.swing.interop.SwingInterOpUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// 1. Vladimir Petrovic is a duplicate (fix issue with accent marks).

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

        HashMap<String, HashMap<String, String>> playerCareers = new HashMap<>();
        // Step 3.
        for (String name: nameList) { // Successfully iterates through the nameList; if you use jObj.get(name), you access the JSON file.
            String arr[] = jObj.get(name).toString().split(",");
            String wikiLink = arr[1]; // Leaves me this format: ("wiki":"https:\/\/it.wikipedia.org\/wiki\/Roberto_Mancini")

            wikiLink = wikiLink.replaceFirst("wiki", "").replace("\"", "").replaceAll("\\\\","").substring(1); // Generates all 162 links.

            if (name.equals("Míchel")) {
                wikiLink = "https://en.wikipedia.org/wiki/M%C3%ADchel_(footballer,_born_1963)"; // Michel is a special case.
            } else if (name.equals("Gary Stevens")) {
                wikiLink = "https://en.wikipedia.org/wiki/Gary_Stevens_(footballer,_born_1962)"; // Stevens is also a special case.
            } else if (name.equals("António Oliveira")) { // As is everyone below.
                wikiLink = "https://en.wikipedia.org/wiki/Ant%C3%B3nio_Oliveira_(footballer,_born_1952)";
            } else if (name.equals("Georgi Dimitrov")) {
                wikiLink = "https://en.wikipedia.org/wiki/Georgi_Dimitrov_(footballer,_born_1959)";
            }

            // Difference between Platini's and Dalglish's Wikipedia pages:
            // Platini's information isn't directly in tr (table rows); the table with his playing career is inside a tr.
            // On the other hand, for Dalglish, the rows are all his playing career information.

            // Problem to solve here: differentiating normal web pages (e.g. Luiz Fernandez, Dalglish) from special ones (Platini, Lato).
            // Special cases do not have a caption underneath the "infobox-vcard" bit like normal ones do (https://gyazo.com/f87f57dd584c166bdbafa70f01175eef).
            Document doc = Jsoup.connect(wikiLink).get();
            Elements body = doc.select("div#content.mw-body").select("div#bodyContent.vector-body")
                    .select("div#mw-content-text.mw-body-content.mw-content-ltr").select("div.mw-parser-output")
                    .select("table.infobox.vcard"); // https://gyazo.com/49c12c47d1410bb8161cb3fe5b07db43: this is where this path leads you.

            boolean debounce = false;

            HashMap<String, String> inner = new HashMap<>(); // Creates the inner HashMap of the footballer's HashMap.

            if (!(body.select("caption.infobox-title.fn").text().contains("Association"))) { // Differentiating the special ones from the normal ones. Although I don't get why this works.
                for (Element e : body.select("tbody").select("tr")) { // This does go through all 25 rows in the V-Table (in Dalglish's case).
                    // The thinking here:
                    // Teams/years played follows directly after the Years / Team / Apps / (Gls) section.
                    // So the section(s) immediately following include team information + years.
                    if (e.select("th.infobox-label").toString().contains("Total") || e.text().contains("National team")) { // Recognises the end of the team/years section. Only problem might be if the footballer played for a club with "Total" in its name.
                        debounce = false;
                        playerCareers.put(name, inner);
                    }

                    if (debounce) {
                        if (inner.containsKey(e.select("td.infobox-data.infobox-data-a").text())) { // Checking for if the footballer played for the same club twice (e.g. Frank Rijkaard and John Wark).
                            inner.put(e.select("td.infobox-data.infobox-data-a").text(), inner.get(e.select("td.infobox-data.infobox-data-a").text()) + ", " + e.select("span").text());
                            continue;
                        }

                        if (e.select("td.infobox-data.infobox-data-a").text().contains("→")) {
                            if (inner.containsKey(e.select("td.infobox-data.infobox-data-a").text().replaceAll("→ ", "").replaceAll("[()]", "").replaceAll(" loan", "").replaceAll(" trial", ""))) {
                                inner.put(e.select("td.infobox-data.infobox-data-a").text().replaceAll("→ ", "").replaceAll("[()]", "").replaceAll(" loan", "").replaceAll(" trial", ""), inner.get(e.select("td.infobox-data.infobox-data-a").text().replaceAll("→ ", "").replaceAll("[()]", "").replaceAll(" loan", "").replaceAll(" trial", "")) + ", " + e.select("span").text());
                                continue;
                            }

                            inner.put(e.select("td.infobox-data.infobox-data-a").text().replaceAll("→ ", "").replaceAll("[()]", "").replaceAll(" loan", "").replaceAll(" trial", ""), e.select("span").text());
                            continue;
                        }

                        inner.put(e.select("td.infobox-data.infobox-data-a").text(), e.select("span").text());
                    }

                    if (e.select("th.infobox-label").toString().contains("Years")) { // Allows for the special teams/years section to be accessed.
                        debounce = true;
                    }
                }
            } else {
                for (Element e : body.select("tbody").select("tr")) {
                    if (e.select("td.infobox-full-data").select("table.infobox-subbox.infobox-3cols-child.vcard").select("caption.infobox-title.fn").text().contains("career")) { // https://gyazo.com/19afe79e42e3dffef172945a2ca1ce90: where we're at.
                        for (Element row : e.select("td.infobox-full-data").select("table.infobox-subbox.infobox-3cols-child.vcard").select("tbody").select("tr")) { // Goes throw the rows of the table, starting from underneath "Association football career."
                            if (row.text().contains("Total") || row.text().contains("National team")) {
                                debounce = false;
                                playerCareers.put(name, inner);
                            }

                            if (debounce) {
                                if (inner.containsKey(row.select("td.infobox-data.infobox-data-a").text())) { // Checking for if the footballer played for the same club twice (e.g. Frank Rijkaard and John Wark).
                                    inner.put(row.select("td.infobox-data.infobox-data-a").text(), inner.get(row.select("td.infobox-data.infobox-data-a").text()) + ", " + row.select("span").text());
                                    continue;
                                }

                                if (row.select("td.infobox-data.infobox-data-a").text().contains("→")) {
                                    if (inner.containsKey(row.select("td.infobox-data.infobox-data-a").text().replaceAll("→ ", "").replaceAll("[()]", "").replaceAll(" loan", "").replaceAll(" trial", ""))) {
                                        inner.put(row.select("td.infobox-data.infobox-data-a").text().replaceAll("→ ", "").replaceAll("[()]", "").replaceAll(" loan", "").replaceAll(" trial", ""), inner.get(row.select("td.infobox-data.infobox-data-a").text().replaceAll("→ ", "").replaceAll("[()]", "").replaceAll(" loan", "").replaceAll(" trial", "")) + ", " + e.select("span").text());
                                        continue;
                                    }

                                    inner.put(row.select("td.infobox-data.infobox-data-a").text().replaceAll("→ ", "").replaceAll("[()]", "").replaceAll(" loan", "").replaceAll(" trial", ""), row.select("span").text());
                                    continue;
                                }

                                inner.put(row.select("td.infobox-data.infobox-data-a").select("td.infobox-data.infobox-data-a").text(), row.select("th.infobox-label").select("span").text());
                            }

                            if (row.text().contains("Years")) {
                                debounce = true;
                            }
                        }
                    }
                }
            }

            System.out.println(playerCareers.get(name));
        }
    }
}
