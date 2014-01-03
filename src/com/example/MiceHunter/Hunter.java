package com.example.MiceHunter;

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
        String response = HunterService.hunt(id, authKey);
        return response.length() > 0 ? HunterService.prettyResponse(response, name) : "";
    }

    public String getBonus() {
        HunterService.getBonus(id, authKey);
        return HunterService.prettyResponse("попытался получить дневной бонус.", name);
    }

    public String sellSS() {
        Integer ssCount = HunterService.getSSCount(id, authKey);
        if (ssCount > 0) {
            Integer realSellCount = ssCount < 200 ? ssCount : 200;
            Boolean sellResult = HunterService.sellSSOnMarket(id, authKey, realSellCount);
            if (sellResult) {
                return HunterService.prettyResponse("выставил на продажу " + realSellCount + " СС.", name);
            }
        }
        return "";
    }

    public List<String> buyCheapSS() {
        List<SSInfo> result = HunterService.searchSSOnMarket(id, authKey);
        List<String> responses = new ArrayList<String>();
        for (SSInfo ss : result) {
            Boolean success = HunterService.buySSOnMarket(id, authKey, ss.eid);
            if (success) {
                responses.add(buyResponse(ss));
            }
        }
        return responses;
    }

    private String buyResponse(SSInfo ss) {
        StringBuilder sb = new StringBuilder("купил на рынке ");
        sb.append(ss.amount);
        sb.append(" CC по ");
        sb.append(ss.price);
        sb.append(" монет за штуку. Общая стоимость -> ");
        sb.append(ss.totalValue);
        sb.append(".");
        return HunterService.prettyResponse(sb.toString(), name);
    }
}