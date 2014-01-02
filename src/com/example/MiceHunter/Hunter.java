package com.example.MiceHunter;

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

    public String doMoneyOnMarket() {
        List<String> result = HunterService.searchSSOnMarket(id, authKey);
        for (String eid : result) {
            HunterService.buySSOnMarket(id, authKey, eid);
        }
        Integer ssCount = HunterService.getSSCount(id, authKey);
        if (ssCount > 0) {
            Boolean sellResult = HunterService.sellSSOnMarket(id, authKey, ssCount < 200 ? ssCount : 200);
            if (sellResult) {
                return HunterService.prettyResponse("выставил на продажу " + ssCount + " СС.", name);
            }
        }
        return "";
    }
}