package com.example.MiceHunter.domain;

import com.example.MiceHunter.service.HunterService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dzmiter
 */
public class Hunter {
    private Long id;
    private String authKey;
    private String name;

    public Hunter(Long id, String authKey, String name) {
        this.id = id;
        this.authKey = authKey;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getAuthKey() {
        return authKey;
    }

    public String getName() {
        return name;
    }

    public String hunt() {
        try {
            String response = HunterService.hunt(id, authKey);
            return response.length() > 0 ? HunterService.prettyResponse(response, name) : "";
        } catch (IOException e) {
            return HunterService.printStackTrace(e);
        }
    }

    public String getBonus() {
        try {
            HunterService.getBonus(id, authKey);
            return HunterService.prettyResponse("попытался получить дневной бонус.", name);
        } catch (IOException e) {
            return HunterService.printStackTrace(e);
        }
    }

    public String sellSS() {
        try {
            Integer ssCount = HunterService.getSSCount(id, authKey);
            if (ssCount > 0) {
                Integer realSellCount = ssCount < 200 ? ssCount : 200;
                Boolean sellResult = HunterService.sellSSOnMarket(id, authKey, realSellCount);
                if (sellResult) {
                    return HunterService.prettyResponse("выставил на продажу " + realSellCount + " СС.", name);
                }
            }
            return "";
        } catch (Exception e) {
            return HunterService.printStackTrace(e);
        }
    }

    public List<String> buyCheapSS() {
        try {
            List<SSInfo> result = HunterService.searchSSOnMarket(id, authKey);
            List<String> responses = new ArrayList<String>();
            for (SSInfo ss : result) {
                Boolean success = HunterService.buySSOnMarket(id, authKey, ss.eid);
                responses.add(buyResponse(ss, success));
            }
            return responses;
        } catch (Exception e) {
            List<String> responses = new ArrayList<String>();
            responses.add(HunterService.printStackTrace(e));
            return responses;
        }
    }

    private String buyResponse(SSInfo ss, Boolean success) {
        String response = success ? "купил на рынке " : "упустил возможность купить ";
        response += ss.amount + " CC по " + ss.price + " монет за штуку. Общая стоимость -> " + ss.totalValue + ".";
        return HunterService.prettyResponse(response, name);
    }
}