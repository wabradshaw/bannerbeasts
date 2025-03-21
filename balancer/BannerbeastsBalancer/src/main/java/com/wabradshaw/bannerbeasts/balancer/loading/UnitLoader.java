package com.wabradshaw.bannerbeasts.balancer.loading;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.wabradshaw.bannerbeasts.balancer.unit.UnitDescription;
import com.wabradshaw.bannerbeasts.balancer.unit.UnitMetadata;
import com.wabradshaw.bannerbeasts.balancer.unit.dynamicvalues.DynamicInt;
import com.wabradshaw.bannerbeasts.balancer.unit.dynamicvalues.DynamicIntFactory;

public class UnitLoader {
    public List<UnitDescription> loadUnitsFromCSV(String filename) {
        List<UnitDescription> units = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(filename))) {
            List<String[]> records = reader.readAll();
            if (records.isEmpty()) return units;
            
            String[] headers = records.get(0);
            
            for (int i = 1; i < records.size(); i++) {
                String[] values = records.get(i);
                if (values.length < headers.length) continue;
                
                Map<String, String> data = new java.util.HashMap<>();
                for (int j = 0; j < headers.length; j++) {
                    data.put(headers[j].trim(), values[j].trim());
                }
                
                if ("TRUE".equalsIgnoreCase(data.get("Include"))) {
                    UnitMetadata metadata = new UnitMetadata(
                        data.get("Id"),
                        data.get("Name"),
                        data.get("Unit"),
                        data.get("Faction"),
                        data.get("Class"),
                        data.get("Comparison Class"),
                        Integer.parseInt(data.get("Tier")),
                        data.containsKey("Cost") && !data.get("Cost").isEmpty() ? Integer.parseInt(data.get("Cost")) : 0
                    );
                    
                    int startingNumber = Integer.parseInt(data.get("Number"));
                    int startingHp = Integer.parseInt(data.get("HP"));
                    String powers = data.get("Powers");
                    DynamicInt movement = DynamicIntFactory.parse(data.get("Movement"), 0);
                    DynamicInt meleeHit = DynamicIntFactory.parse(data.get("Melee Hit"), 7);
                    DynamicInt rangedHit = DynamicIntFactory.parse(data.get("Ranged Hit"), 7);
                    DynamicInt attacks = DynamicIntFactory.parse(data.get("Attacks"), 0);
                    DynamicInt block = DynamicIntFactory.parse(data.get("Block"), 7);
                    
                    units.add(new UnitDescription(metadata, startingNumber, startingHp, powers, movement, meleeHit, rangedHit, attacks, block));
                }
            }
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
        return units;
    }
}