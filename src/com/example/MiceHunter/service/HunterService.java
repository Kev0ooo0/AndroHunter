package com.example.MiceHunter.service;

import com.example.MiceHunter.domain.SSInfo;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Dzmitry Bezugly
 */
public class HunterService {
    private static final String DOMAIN = "http://mice2112.com";

    public static String printStackTrace(Exception e) {
        return e.getMessage();
    }

    public static String prettyResponse(String response, String name) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date()) + " " + name + " " + response;
    }

    public static String hunt(Long vkUserId, String authKey) throws IOException {
        String response = sendHuntRequest(vkUserId, authKey);
        controlCheeseCount(response, vkUserId, authKey);
        return parseHuntResponse(response);
    }

    private static String sendHuntRequest(Long vkUserId, String authKey) throws IOException {
        final String URL_PATTERN = DOMAIN + "/hunt/start/";
        final String URL_PARAMETERS = "auth_key=$authKey&viewer_id=$vkUserId";
        String params = URL_PARAMETERS.replace("$authKey", authKey).replace("$vkUserId", vkUserId.toString());
        return doPOSTRequest(URL_PATTERN, params);
    }

    private static String parseHuntResponse(String response) {
        Document doc = Jsoup.parse(response);
        Elements elements = doc.select("div.journal-b-p");
        if (!elements.isEmpty()) {
            return elements.get(0).text();
        }
        return "";
    }

    public static void getBonus(Long vkUserId, String authKey) throws IOException {
        final String URL_PATTERN = DOMAIN + "/game/bonus";
        final String URL_PARAMETERS = "auth_key=$authKey&viewer_id=$vkUserId";
        String params = URL_PARAMETERS.replace("$authKey", authKey).replace("$vkUserId", vkUserId.toString());
        doPOSTRequest(URL_PATTERN, params);
    }

    public static void controlCheeseCount(String response, Long vkUserId, String authKey) throws IOException {
        String start = "Application.updateParamCampActiveCheeseCount(";
        String end = ");";
        int startIndex = response.indexOf(start);
        int endIndex = response.indexOf(end, startIndex);

        if (startIndex == -1 || endIndex == -1) return;

        String[] activeCheeseInfo = response.substring(startIndex + start.length(), endIndex - 1).split(",");
        Long count = activeCheeseInfo[0].matches("\\d+") ? Long.decode(activeCheeseInfo[0]) : 0L;

        if (count < 20L) {
            Long gold = getMoneyFromHuntResponse(response);
            long cost = 200L; //200 gold - 1 cheese

            if (cost * 200L < gold) {
                buyCheese(vkUserId, authKey, 13, 200, 2);
                setActiveCheese(vkUserId, authKey, 4);
            } else {
                int cnt = (int) (gold / cost);
                buyCheese(vkUserId, authKey, 13, cnt, 2);
                setActiveCheese(vkUserId, authKey, 4);
            }
        }
    }

    private static Long getMoneyFromHuntResponse(String response) {
        String start = "Application.updateParamHeadGold(";
        String end = ");";
        int startIndex = response.indexOf(start);
        int endIndex = response.indexOf(end, startIndex);

        if (startIndex == -1 || endIndex == -1) return 0L;

        String money = response.substring(startIndex + start.length(), endIndex - 1);
        return money.matches("\\d+") ? Long.decode(money) : 0L;
    }

    //blue cheese - cheeseId = 13, locationId = 2
    private static void buyCheese(Long vkUserId, String authKey,
                                  int cheeseId, int count, int locationId) throws IOException {
        final String URL_PATTERN = DOMAIN + "/shop/buy/";
        final String URL_PARAMETERS = "auth_key=$authKey&count=$count&eid=$cheeseId" +
                "&free=1&location_id=$locationId&viewer_id=$vkUserId";
        String params = URL_PARAMETERS.replace("$authKey", authKey).replace("$vkUserId", vkUserId.toString())
                .replace("$count", count + "").replace("$cheeseId", cheeseId + "")
                .replace("$locationId", locationId + "");
        doPOSTRequest(URL_PATTERN, params);
    }

    //blue id = 4
    private static void setActiveCheese(Long vkUserId, String authKey, int id) throws IOException {
        final String URL_PATTERN = DOMAIN + "/game/set-active-cheese/";
        final String URL_PARAMETERS = "auth_key=$authKey&is_boss_camp=0&item_id=$id&type=arm&viewer_id=$vkUserId";
        String params = URL_PARAMETERS.replace("$authKey", authKey).replace("$vkUserId", vkUserId.toString())
                .replace("$id", id + "");
        doPOSTRequest(URL_PATTERN, params);
    }

    public static List<SSInfo> searchSSOnMarket(Long vkUserId, String authKey) throws IOException {
        String response = sendSearchOnMarketRequest(vkUserId, authKey, "option_cheese_5", 7200, 0);
        return parseSearchOnMarketResponse(response);
    }

    private static String sendSearchOnMarketRequest(Long vkUserId, String authKey, String filter,
                                                    Integer maxprice, Integer maxcount) throws IOException {
        final String URL_PATTERN = DOMAIN + "/marketplace/search/";
        final String URL_PARAMETERS = "auth_key=$authKey&viewer_id=$vkUserId&filter=$filter&maxprice=$maxprice&maxcount=$maxcount";
        String params = URL_PARAMETERS.replace("$authKey", authKey)
                .replace("$vkUserId", vkUserId.toString())
                .replace("$filter", filter)
                .replace("$maxprice", (maxprice > 0 && maxprice < 8000) ? maxprice.toString() : "NaN")
                .replace("$maxcount", (maxcount > 0) ? maxcount.toString() : "NaN");
        return doPOSTRequest(URL_PATTERN, params);
    }

    private static List<SSInfo> parseSearchOnMarketResponse(String response) {
        Document doc = Jsoup.parse(response);
        Elements ssItems = doc.select("tr[id^=result_tr_]");
        List<SSInfo> ssInfoList = new ArrayList<SSInfo>();
        for (Element ssItem : ssItems) {
            SSInfo ss = new SSInfo();
            Elements amount = ssItem.select("td:eq(1)");
            ss.amount = Integer.decode(amount.get(0).text());
            Elements price = ssItem.select("td:eq(2)");
            ss.price = Integer.decode(price.get(0).text());
            Elements totalValue = ssItem.select("td:eq(4)");
            ss.totalValue = Long.decode(totalValue.get(0).text());
            Elements eid = ssItem.select("td:eq(5)>a:first-child");
            ss.eid = eid.get(0).attr("eid");
            ssInfoList.add(ss);
        }
        return ssInfoList;
    }

    public static Boolean buySSOnMarket(Long vkUserId, String authKey, String eid) throws ParseException, IOException {
        String response = sendBuyOnMarketRequest(vkUserId, authKey, "option_cheese_5", 7200, 0, eid);
        return parseJSONMarketResponse(response);
    }

    private static String sendBuyOnMarketRequest(Long vkUserId, String authKey, String filter,
                                                 Integer maxprice, Integer maxcount, String eid) throws IOException {
        final String URL_PATTERN = DOMAIN + "/marketplace/buy/";
        final String URL_PARAMETERS = "auth_key=$authKey&viewer_id=$vkUserId&filter=$filter&eid=$eid&maxprice=$maxprice&maxcount=$maxcount";
        String params = URL_PARAMETERS.replace("$authKey", authKey)
                .replace("$vkUserId", vkUserId.toString())
                .replace("$filter", filter)
                .replace("$maxprice", (maxprice > 0 && maxprice < 8000) ? maxprice.toString() : "NaN")
                .replace("$maxcount", (maxcount > 0) ? maxcount.toString() : "NaN")
                .replace("$eid", eid);
        return doPOSTRequest(URL_PATTERN, params);
    }

    private static Boolean parseJSONMarketResponse(String response) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject message = (JSONObject) parser.parse(response);
        String status = (String) message.get("message");
        return status.equals("OK");
    }

    public static Boolean sellSSOnMarket(Long vkUserId, String authKey, Integer count) throws IOException, ParseException {
        String response = sendSellOnMarketRequest(vkUserId, authKey, "5", count, 8000);
        return parseJSONMarketResponse(response);
    }

    private static String sendSellOnMarketRequest(Long vkUserId, String authKey, String eid,
                                                  Integer count, Integer price) throws IOException {
        final String URL_PATTERN = DOMAIN + "/marketplace/sell/";
        final String URL_PARAMETERS = "auth_key=$authKey&viewer_id=$vkUserId&eid=$eid&type=cheese&count=$count&price=$price";
        String params = URL_PARAMETERS.replace("$authKey", authKey)
                .replace("$vkUserId", vkUserId.toString())
                .replace("$price", (price >= 3000 && price <= 8000) ? price.toString() : "8000")
                .replace("$count", count.toString())
                .replace("$eid", eid);
        return doPOSTRequest(URL_PATTERN, params);
    }

    public static Integer getSSCount(Long vkUserId, String authKey) throws IOException {
        String response = sendSSCountRequest(vkUserId, authKey);
        return parseSSCountResponse(response);
    }

    private static String sendSSCountRequest(Long vkUserId, String authKey) throws IOException {
        final String URL_PATTERN = DOMAIN + "/inventory/index/tab/trapsmith";
        final String URL_PARAMETERS = "auth_key=$authKey&viewer_id=$vkUserId";
        String params = URL_PARAMETERS.replace("$authKey", authKey).replace("$vkUserId", vkUserId.toString());
        return doPOSTRequest(URL_PATTERN, params);
    }

    private static Integer parseSSCountResponse(String response) {
        Document doc = Jsoup.parse(response);
        Elements elements = doc.select("#count_5_5");
        if (!elements.isEmpty()) {
            return Integer.decode(elements.get(0).text());
        }
        return 0;
    }

    private static String doPOSTRequest(String url, String parameters) throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setDoOutput(true);

        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(parameters);
        wr.flush();
        wr.close();

        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = reader.readLine()) != null) {
            response.append(inputLine);
        }
        reader.close();

        return response.toString();
    }
}