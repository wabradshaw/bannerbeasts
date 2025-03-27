package com.wabradshaw.bannerbeasts.balancer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.opencsv.CSVWriter;
import com.wabradshaw.bannerbeasts.balancer.harness.FullHarness;
import com.wabradshaw.bannerbeasts.balancer.harness.Harness;
import com.wabradshaw.bannerbeasts.balancer.harness.TestHarness;
import com.wabradshaw.bannerbeasts.balancer.loading.UnitLoader;
import com.wabradshaw.bannerbeasts.balancer.results.BattleResult;
import com.wabradshaw.bannerbeasts.balancer.results.BattleSummary;
import com.wabradshaw.bannerbeasts.balancer.results.RankResult;
import com.wabradshaw.bannerbeasts.balancer.unit.UnitDescription;
import com.wabradshaw.bannerbeasts.balancer.unit.UnitPowers;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Loading Units");

        String filename = "./Bannerbeasts Roller - Units.csv";
        UnitLoader loader = new UnitLoader();
        List<UnitDescription> units = loader.loadUnitsFromCSV(filename);

        for (UnitDescription unit : units) {
            System.out.println(unit);
        }

        System.out.println("Units Loaded");

        // Harness harness = new TestHarness(new int[]{0,15,30,45,60,75,90}, 100);
        // Harness harness = new TestHarness(new int[]{0,3}, 100);
        Harness harness = new FullHarness(2000);
        List<BattleResult> results = harness.runSimulation(units);

        List<BattleSummary> resultsSummary = summarizeBattles(results);

        // printWinrateTable(results);

        String resultsFile = "./Output.csv";
        try (CSVWriter writer = new CSVWriter(new FileWriter(resultsFile))) {
            writer.writeNext(BattleSummary.CSV_HEADERS);
            for (BattleSummary result : resultsSummary) {
                writer.writeNext(result.toCsvRow());
            }
            System.out.println("Summary CSV Saved");
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<RankResult> ranks = calculateRankResults(resultsSummary);

        String rankResultsFile = "./OutputRank.csv";
        try (CSVWriter writer = new CSVWriter(new FileWriter(rankResultsFile))) {
            writer.writeNext(RankResult.CSV_HEADERS);
            for (RankResult result : ranks) {
                writer.writeNext(result.toCsvRow());
            }
            System.out.println("Rank CSV Saved");
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(UnitPowers.unhandledPowers);
    }

    public static List<BattleSummary> summarizeBattles(List<BattleResult> results) {
        return results.stream()
                .collect(Collectors.groupingBy(
                        result -> Arrays.asList(result.getUnit1().getId(), result.getUnit2().getId())))
                .entrySet().stream()
                .map(entry -> {
                    List<Integer> ids = entry.getKey();
                    List<BattleResult> grouped = entry.getValue();
                    return new BattleSummary(ids.get(0), ids.get(1), grouped);
                })
                .collect(Collectors.toList());
    }

    public static void printWinrateTable(List<BattleResult> results) {
        Map<Integer, Map<Integer, Integer>> wins = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> totalBattles = new HashMap<>();
        Set<Integer> ids = new TreeSet<>();

        for (BattleResult result : results) {
            int id1 = result.getUnit1().getId();
            int id2 = result.getUnit2().getId();

            if (!ids.contains(id1)) {
                System.out.println(id1 + " " + result.getUnit1().getUnitMetadata().getUnit());
            }
            if (!ids.contains(id2)) {
                System.out.println(id2 + " " + result.getUnit2().getUnitMetadata().getUnit());
            }
            ids.add(id1);
            ids.add(id2);

            wins.putIfAbsent(id1, new HashMap<>());
            wins.putIfAbsent(id2, new HashMap<>());
            totalBattles.putIfAbsent(id1, new HashMap<>());
            totalBattles.putIfAbsent(id2, new HashMap<>());

            // Track battles and wins
            switch (result.getOutcome()) {
                case WON_WIPEOUT:
                case WON_FLED:
                    wins.get(id1).merge(id2, 1, Integer::sum);
                    break;
                case LOST_WIPEOUT:
                case LOST_FLED:
                    wins.get(id2).merge(id1, 1, Integer::sum);
                    break;
                case TIED:
                case TOTAL_WIPEOUT:
                    // No wins
                    break;
            }
            totalBattles.get(id1).merge(id2, 1, Integer::sum);
            totalBattles.get(id2).merge(id1, 1, Integer::sum);
        }

        // Header
        System.out.print("    | ");
        for (int col : ids) {
            System.out.printf("%03d | ", col);
        }
        System.out.println();
        System.out.print("----|-");
        for (int col : ids) {
            System.out.printf("----|-", col);
        }
        System.out.println();

        // Rows
        for (int row : ids) {
            System.out.printf("%03d ", row);
            for (int col : ids) {
                if (row == col) {
                    System.out.print("| --- ");
                } else {
                    int total = totalBattles.getOrDefault(row, new HashMap<>()).getOrDefault(col, 0);
                    int win = wins.getOrDefault(row, new HashMap<>()).getOrDefault(col, 0);
                    String percent = total == 0 ? "  0" : String.format("%3d", (int) Math.round((100.0 * win) / total));
                    System.out.print("| " + percent + " ");
                }
            }
            System.out.println("|-");
        }
    }

    private static double averageResultsRatios(List<BattleSummary> results) {
        return results.stream().collect(Collectors.averagingDouble(e -> e.getRatio()));
    }

    private  static List<RankResult> calculateRankResults(List<BattleSummary> summaries) {
        Map<UnitDescription, List<BattleSummary>> unitResults = summaries.stream()
                .filter(s -> s.getUnit1().getUnitMetadata().getTargetCombatRanking() > 0)
                .collect(Collectors.groupingBy(BattleSummary::getUnit1));

        List<Double> expectedRanksByPerformance  = unitResults.keySet().stream()
                .map(u -> u.getUnitMetadata().getTargetCombatRanking())
                .sorted(Comparator.reverseOrder())
                .toList();
        
        Map<UnitDescription, Double> unitRatios = unitResults.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> averageResultsRatios(e.getValue()))); 

        List<Map.Entry<UnitDescription, Double>> sortedUnits = unitRatios.entrySet().stream()
                    .sorted(Map.Entry.<UnitDescription, Double>comparingByValue().reversed())
                    .toList();

        List<RankResult> rankResults = new ArrayList<>();

        for(int i = 0; i < unitResults.size(); i++){
            UnitDescription unit = sortedUnits.get(i).getKey();
            double avgRatio = sortedUnits.get(i).getValue();            
            rankResults.add(new RankResult(unit, avgRatio, expectedRanksByPerformance .get(i))); 
        }

        rankResults.sort(Comparator.comparingDouble(r -> r.getRankDiff()));
        
        return rankResults;
    }

}
