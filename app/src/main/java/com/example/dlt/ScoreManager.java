package com.example.dlt;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分数管理类 - 负责管理各组算法的得分、中奖等级和屏蔽规则
 * 
 * 功能概述：
 * 1. 分数管理：加载、保存、清空各组的得分
 * 2. 中奖等级管理：记录每组的最高中奖等级和中奖次数
 * 3. 屏蔽规则管理：管理被屏蔽的算法组
 * 4. 中奖计算：根据开奖号码和确认号码计算中奖情况
 * 
 * @author dlt项目组
 * @version 1.0
 */
public class ScoreManager {
    
    // =============================================================================
    // 常量定义
    // =============================================================================
    
    private static final String PREFS_NAME = "dlt_history";
    private static final String KEY_GROUP_SCORES = "group_scores";
    private static final String KEY_GROUP_MAX_PRIZE = "group_max_prize";
    private static final String KEY_GROUP_MAX_PRIZE_COUNT = "group_max_prize_count";
    private static final String KEY_BLOCKED_RULES = "blocked_rules";
    private static final String KEY_GROUP_PRIZE_HISTORY = "group_prize_history"; // 各组中奖历史
    private static final int TOTAL_GROUPS = 13; // 总共13组算法
    
    // =============================================================================
    // 数据存储
    // =============================================================================
    
    private Context context;
    private Map<Integer, Integer> groupScores = new HashMap<>();       // 各组得分
    private Map<Integer, Integer> groupMaxPrize = new HashMap<>();     // 各组最大中奖等级
    private Map<Integer, Integer> groupMaxPrizeCount = new HashMap<>(); // 各组最高奖中奖次数
    private Set<Integer> blockedRules = new HashSet<>();              // 被屏蔽的规则
    private Map<Integer, List<PrizeRecord>> groupPrizeHistory = new HashMap<>(); // 各组中奖历史
    
    // =============================================================================
    // 构造方法
    // =============================================================================
    
    /**
     * 构造方法
     * @param context 上下文对象
     */
    public ScoreManager(Context context) {
        this.context = context;
        loadAllData();
    }
    
    // =============================================================================
    // 数据加载和保存
    // =============================================================================
    
    /**
     * 加载所有数据（分数、中奖等级、屏蔽规则）
     */
    public void loadAllData() {
        loadGroupScores();
        loadBlockedRules();
    }
    
    /**
     * 加载各组分数
     */
    private void loadGroupScores() {
        groupScores.clear();
        groupMaxPrize.clear();
        groupMaxPrizeCount.clear();
        groupPrizeHistory.clear();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // 加载分数
        String scores = prefs.getString(KEY_GROUP_SCORES, "");
        if (!TextUtils.isEmpty(scores)) {
            String[] scoreArray = scores.split(",");
            for (String scoreEntry : scoreArray) {
                String[] parts = scoreEntry.split(":");
                if (parts.length == 2) {
                    try {
                        int groupNum = Integer.parseInt(parts[0]);
                        int score = Integer.parseInt(parts[1]);
                        groupScores.put(groupNum, score);
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }
        }
        
        // 加载最大中奖等级
        String maxPrizes = prefs.getString(KEY_GROUP_MAX_PRIZE, "");
        if (!TextUtils.isEmpty(maxPrizes)) {
            String[] prizeArray = maxPrizes.split(",");
            for (String prizeEntry : prizeArray) {
                String[] parts = prizeEntry.split(":");
                if (parts.length == 2) {
                    try {
                        int groupNum = Integer.parseInt(parts[0]);
                        int prize = Integer.parseInt(parts[1]);
                        groupMaxPrize.put(groupNum, prize);
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }
        }
        
        // 加载最高奖中奖次数
        String maxPrizeCounts = prefs.getString(KEY_GROUP_MAX_PRIZE_COUNT, "");
        if (!TextUtils.isEmpty(maxPrizeCounts)) {
            String[] countArray = maxPrizeCounts.split(",");
            for (String countEntry : countArray) {
                String[] parts = countEntry.split(":");
                if (parts.length == 2) {
                    try {
                        int groupNum = Integer.parseInt(parts[0]);
                        int count = Integer.parseInt(parts[1]);
                        groupMaxPrizeCount.put(groupNum, count);
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }
        }
        
        // 加载各组中奖历史
        String prizeHistories = prefs.getString(KEY_GROUP_PRIZE_HISTORY, "");
        if (!TextUtils.isEmpty(prizeHistories)) {
            String[] groupHistories = prizeHistories.split("\\|\\|\\|");
            for (String groupHistory : groupHistories) {
                if (TextUtils.isEmpty(groupHistory)) continue;
                String[] parts = groupHistory.split(":::", 2);
                if (parts.length == 2) {
                    try {
                        int groupNum = Integer.parseInt(parts[0]);
                        String historyData = parts[1];
                        List<PrizeRecord> records = new ArrayList<>();
                        if (!TextUtils.isEmpty(historyData)) {
                            String[] recordArray = historyData.split(";;");
                            for (String recordStr : recordArray) {
                                if (TextUtils.isEmpty(recordStr)) continue;
                                String[] recordParts = recordStr.split("@@");
                                if (recordParts.length == 2) {
                                    String issueNumber = recordParts[0];
                                    int prizeLevel = Integer.parseInt(recordParts[1]);
                                    records.add(new PrizeRecord(issueNumber, prizeLevel));
                                }
                            }
                        }
                        groupPrizeHistory.put(groupNum, records);
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }
        }
        
        // 初始化所有组的分数为0（如果没有记录）
        for (int i = 1; i <= TOTAL_GROUPS; i++) {
            if (!groupScores.containsKey(i)) {
                groupScores.put(i, 0);
            }
            if (!groupPrizeHistory.containsKey(i)) {
                groupPrizeHistory.put(i, new ArrayList<>());
            }
        }
    }
    
    /**
     * 保存各组分数
     */
    public void saveGroupScores() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // 保存分数
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= TOTAL_GROUPS; i++) {
            if (i > 1) sb.append(",");
            sb.append(i).append(":").append(groupScores.getOrDefault(i, 0));
        }
        
        // 保存最大中奖等级
        StringBuilder sbPrize = new StringBuilder();
        boolean first = true;
        for (Map.Entry<Integer, Integer> entry : groupMaxPrize.entrySet()) {
            if (!first) sbPrize.append(",");
            sbPrize.append(entry.getKey()).append(":").append(entry.getValue());
            first = false;
        }
        
        // 保存最高奖中奖次数
        StringBuilder sbCount = new StringBuilder();
        boolean firstCount = true;
        for (Map.Entry<Integer, Integer> entry : groupMaxPrizeCount.entrySet()) {
            if (!firstCount) sbCount.append(",");
            sbCount.append(entry.getKey()).append(":").append(entry.getValue());
            firstCount = false;
        }
        
        // 保存中奖历史
        StringBuilder sbHistory = new StringBuilder();
        boolean firstHistory = true;
        for (Map.Entry<Integer, List<PrizeRecord>> entry : groupPrizeHistory.entrySet()) {
            if (!firstHistory) sbHistory.append("|||");
            sbHistory.append(entry.getKey()).append(":::");
            List<PrizeRecord> records = entry.getValue();
            for (int i = 0; i < records.size(); i++) {
                if (i > 0) sbHistory.append(";;");
                PrizeRecord record = records.get(i);
                sbHistory.append(record.getIssueNumber()).append("@@").append(record.getPrizeLevel());
            }
            firstHistory = false;
        }
        
        prefs.edit()
            .putString(KEY_GROUP_SCORES, sb.toString())
            .putString(KEY_GROUP_MAX_PRIZE, sbPrize.toString())
            .putString(KEY_GROUP_MAX_PRIZE_COUNT, sbCount.toString())
            .putString(KEY_GROUP_PRIZE_HISTORY, sbHistory.toString())
            .apply();
    }
    
    /**
     * 加载被屏蔽的规则
     */
    private void loadBlockedRules() {
        blockedRules.clear();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String blocked = prefs.getString(KEY_BLOCKED_RULES, "");
        if (!TextUtils.isEmpty(blocked)) {
            String[] ruleArray = blocked.split(",");
            for (String ruleStr : ruleArray) {
                try {
                    int ruleNum = Integer.parseInt(ruleStr.trim());
                    if (ruleNum >= 1 && ruleNum <= TOTAL_GROUPS) {
                        blockedRules.add(ruleNum);
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        }
    }
    
    /**
     * 保存被屏蔽的规则
     */
    public void saveBlockedRules() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int ruleNum : blockedRules) {
            if (!first) sb.append(",");
            sb.append(ruleNum);
            first = false;
        }
        prefs.edit().putString(KEY_BLOCKED_RULES, sb.toString()).apply();
    }
    
    // =============================================================================
    // 分数管理
    // =============================================================================
    
    /**
     * 清空所有分数
     */
    public void clearAllScores() {
        for (int i = 1; i <= TOTAL_GROUPS; i++) {
            groupScores.put(i, 0);
            groupMaxPrize.remove(i);
            groupMaxPrizeCount.remove(i);
            groupPrizeHistory.put(i, new ArrayList<>());
        }
        blockedRules.clear();
        saveBlockedRules();
        saveGroupScores();
    }
    
    /**
     * 获取指定组的分数
     */
    public int getGroupScore(int groupNum) {
        return groupScores.getOrDefault(groupNum, 0);
    }
    
    /**
     * 获取指定组的最高中奖等级
     */
    public int getGroupMaxPrize(int groupNum) {
        return groupMaxPrize.getOrDefault(groupNum, 0);
    }
    
    /**
     * 获取指定组的最高奖中奖次数
     */
    public int getGroupMaxPrizeCount(int groupNum) {
        return groupMaxPrizeCount.getOrDefault(groupNum, 0);
    }
    
    /**
     * 获取指定组的中奖历史
     */
    public List<PrizeRecord> getGroupPrizeHistory(int groupNum) {
        return groupPrizeHistory.getOrDefault(groupNum, new ArrayList<>());
    }
    
    /**
     * 获取所有组的分数信息（用于显示）
     */
    public String getAllScoresInfo() {
        StringBuilder sb = new StringBuilder();
        
        // 计算总分数
        int totalScore = 0;
        for (int i = 1; i <= TOTAL_GROUPS; i++) {
            totalScore += groupScores.getOrDefault(i, 0);
        }
        
        sb.append("总分数：" + totalScore + "\n\n");
        sb.append("各组分数情况：\n\n");
        for (int i = 1; i <= TOTAL_GROUPS; i++) {
            int score = groupScores.getOrDefault(i, 0);
            int maxPrize = groupMaxPrize.getOrDefault(i, 0);
            int prizeCount = groupMaxPrizeCount.getOrDefault(i, 0);
            String prizeText = maxPrize > 0 ? getPrizeText(maxPrize) : "无";
            String countText = prizeCount > 0 ? "（" + prizeCount + "次）" : "";
            sb.append(String.format("第%d组：%d分 | 最高：%s%s\n", i, score, prizeText, countText));
        }
        return sb.toString();
    }
    
    // =============================================================================
    // 屏蔽规则管理
    // =============================================================================
    
    /**
     * 检查规则是否被屏蔽
     */
    public boolean isRuleBlocked(int ruleNum) {
        return blockedRules.contains(ruleNum);
    }
    
    /**
     * 屏蔽指定规则
     */
    public void blockRule(int ruleNum) {
        blockedRules.add(ruleNum);
        saveBlockedRules();
    }
    
    /**
     * 取消屏蔽指定规则
     */
    public void unblockRule(int ruleNum) {
        blockedRules.remove(ruleNum);
        saveBlockedRules();
    }
    
    /**
     * 获取被屏蔽的规则集合
     */
    public Set<Integer> getBlockedRules() {
        return new HashSet<>(blockedRules);
    }
    
    // =============================================================================
    // 中奖计算
    // =============================================================================
    
    /**
     * 计算中奖分数
     * 
     * @param confirmedNumbers 确认的号码字符串
     * @param newEntry 新开奖数据
     * @return 中奖结果信息
     */
    public WinningResult calculateWinningScores(String confirmedNumbers, LotteryEntry newEntry) {
        if (TextUtils.isEmpty(confirmedNumbers)) {
            return null;
        }
        
        // 解析确认的号码（13组）
        List<List<Integer>> confirmedFrontGroups = new ArrayList<>();
        List<List<Integer>> confirmedBackGroups = new ArrayList<>();
        
        String[] lines = confirmedNumbers.split("\n");
        for (String line : lines) {
            // 跳过被屏蔽的组
            if (line.contains("[已屏蔽]")) {
                confirmedFrontGroups.add(new ArrayList<>());
                confirmedBackGroups.add(new ArrayList<>());
                continue;
            }
            
            if (line.contains("第") && line.contains("组") && line.contains("前区") && line.contains("后区")) {
                try {
                    // 解析格式：第X组：前区[1, 2, 3, 4, 5] 后区[1, 2]
                    int frontStart = line.indexOf("前区") + 2;
                    int frontEnd = line.indexOf("后区");
                    int backStart = line.indexOf("后区") + 2;
                    
                    if (frontStart > 1 && frontEnd > frontStart && backStart > 1) {
                        String frontStr = line.substring(frontStart, frontEnd).trim();
                        String backStr = line.substring(backStart).trim();
                        
                        // 移除方括号和处理数字列表
                        frontStr = frontStr.replaceAll("[\\[\\]]", "").trim();
                        backStr = backStr.replaceAll("[\\[\\]]", "").trim();
                        
                        List<Integer> frontNumbers = parseNumberString(frontStr);
                        List<Integer> backNumbers = parseNumberString(backStr);
                        
                        if (frontNumbers.size() == 5 && backNumbers.size() == 2) {
                            confirmedFrontGroups.add(frontNumbers);
                            confirmedBackGroups.add(backNumbers);
                        }
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                    android.util.Log.d("DLT_DEBUG", "解析行失败: " + line + ", 错误: " + e.getMessage());
                }
            }
        }
        
        // 验证解析结果
        if (confirmedFrontGroups.size() != TOTAL_GROUPS || confirmedBackGroups.size() != TOTAL_GROUPS) {
            String errorMsg = String.format("确认号码格式错误，无法进行计算。解析到前区%d组，后区%d组（期望各%d组）", 
                confirmedFrontGroups.size(), confirmedBackGroups.size(), TOTAL_GROUPS);
            android.util.Log.e("DLT_ERROR", errorMsg);
            return new WinningResult(false, errorMsg, 0);
        }
        
        // 获取开奖号码
        List<Integer> winningFront = newEntry.getFrontNumbers();
        List<Integer> winningBack = newEntry.getBackNumbers();
        
        int totalScoreChange = 0;
        StringBuilder resultMessage = new StringBuilder();
        resultMessage.append("中奖结果：\n\n");
        
        // 对比每一组（跳过被屏蔽的规则）
        for (int groupIndex = 0; groupIndex < TOTAL_GROUPS; groupIndex++) {
            if (blockedRules.contains(groupIndex + 1)) {
                // 被屏蔽的规则不参与中奖计算
                continue;
            }
            
            List<Integer> groupFront = confirmedFrontGroups.get(groupIndex);
            List<Integer> groupBack = confirmedBackGroups.get(groupIndex);
            
            // 检查是否为空的屏蔽组（防止错误数据）
            if (groupFront.isEmpty() || groupBack.isEmpty()) {
                continue;
            }
            
            int frontMatches = countMatches(groupFront, winningFront);
            int backMatches = countMatches(groupBack, winningBack);
            
            int prizeLevel = determinePrizeLevel(frontMatches, backMatches);
            int scoreChange = getScoreChange(prizeLevel);
            
            // 更新分数
            int currentScore = groupScores.getOrDefault(groupIndex + 1, 0);
            groupScores.put(groupIndex + 1, currentScore + scoreChange);
            totalScoreChange += scoreChange;
            
            // 更新最大中奖等级（数字越小等级越高）
            if (prizeLevel > 0) {
                int currentMaxPrize = groupMaxPrize.getOrDefault(groupIndex + 1, Integer.MAX_VALUE);
                if (prizeLevel < currentMaxPrize) {
                    // 更新最高奖等级和中奖次数
                    groupMaxPrize.put(groupIndex + 1, prizeLevel);
                    groupMaxPrizeCount.put(groupIndex + 1, 1); // 新的最高奖，次数重置为1
                } else if (prizeLevel == currentMaxPrize) {
                    // 相同的最高奖等级，中奖次数+1
                    int currentCount = groupMaxPrizeCount.getOrDefault(groupIndex + 1, 0);
                    groupMaxPrizeCount.put(groupIndex + 1, currentCount + 1);
                }
                
                // 保存中奖记录（期号和等级）
                List<PrizeRecord> history = groupPrizeHistory.getOrDefault(groupIndex + 1, new ArrayList<>());
                history.add(new PrizeRecord(newEntry.getIssueNumber(), prizeLevel));
                groupPrizeHistory.put(groupIndex + 1, history);
            }
            
            // 构建结果信息
            String prizeText = getPrizeText(prizeLevel);
            String changeText = scoreChange > 0 ? "+" + scoreChange : String.valueOf(scoreChange);
            resultMessage.append(String.format("第%d组：%s (%s分)\n", 
                groupIndex + 1, prizeText, changeText));
        }
        
        resultMessage.append(String.format("\n本次总计：%s%d分", 
            totalScoreChange >= 0 ? "+" : "", totalScoreChange));
        
        // 保存分数
        saveGroupScores();
        
        return new WinningResult(true, resultMessage.toString(), totalScoreChange);
    }
    
    // =============================================================================
    // 辅助方法
    // =============================================================================
    
    /**
     * 解析号码字符串
     */
    private List<Integer> parseNumberString(String numberStr) {
        List<Integer> numbers = new ArrayList<>();
        if (TextUtils.isEmpty(numberStr)) return numbers;
        
        // 处理多种分隔符：逗号、空格、混合使用
        String[] parts = numberStr.split("[,\\s]+");
        for (String part : parts) {
            try {
                String trimmed = part.trim();
                if (!TextUtils.isEmpty(trimmed)) {
                    numbers.add(Integer.parseInt(trimmed));
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        return numbers;
    }
    
    /**
     * 计算匹配数量
     */
    private int countMatches(List<Integer> group, List<Integer> winning) {
        int matches = 0;
        for (int number : group) {
            if (winning.contains(number)) {
                matches++;
            }
        }
        return matches;
    }
    
    /**
     * 判断奖级
     */
    private int determinePrizeLevel(int frontMatches, int backMatches) {
        if (frontMatches == 5 && backMatches == 2) return 1; // 一等奖
        if (frontMatches == 5 && backMatches == 1) return 2; // 二等奖
        if (frontMatches == 5 && backMatches == 0) return 3; // 三等奖
        if (frontMatches == 4 && backMatches == 2) return 4; // 四等奖
        if (frontMatches == 4 && backMatches == 1) return 5; // 五等奖
        if (frontMatches == 3 && backMatches == 2) return 6; // 六等奖
        if (frontMatches == 4 && backMatches == 0) return 7; // 七等奖
        if ((frontMatches == 3 && backMatches == 1) || (frontMatches == 2 && backMatches == 2)) return 8; // 八等奖
        if (frontMatches == 3 || (frontMatches == 1 && backMatches == 2) || 
            (frontMatches == 2 && backMatches == 1) || (frontMatches == 0 && backMatches == 2)) return 9; // 九等奖
        return 0; // 未中奖
    }
    
    /**
     * 获取分数变化
     */
    private int getScoreChange(int prizeLevel) {
        switch (prizeLevel) {
            case 1: return 9999998;  // 一等奖
            case 2: return 99998;  // 二等奖
            case 3: return 9998;  // 三等奖
            case 4: return 2998;  // 四等奖
            case 5: return 298;  // 五等奖
            case 6: return 198;  // 六等奖
            case 7: return 98;  // 七等奖
            case 8: return 13;  // 八等奖
            case 9: return 3;  // 九等奖
            default: return -2; // 未中奖
        }
    }
    
    /**
     * 获取奖级文本
     */
    public static String getPrizeText(int prizeLevel) {
        switch (prizeLevel) {
            case 1: return "一等奖";
            case 2: return "二等奖";
            case 3: return "三等奖";
            case 4: return "四等奖";
            case 5: return "五等奖";
            case 6: return "六等奖";
            case 7: return "七等奖";
            case 8: return "八等奖";
            case 9: return "九等奖";
            default: return "未中奖";
        }
    }
    
    // =============================================================================
    // 内部类：中奖结果
    // =============================================================================
    
    /**
     * 中奖结果类
     */
    public static class WinningResult {
        private boolean success;
        private String message;
        private int totalScoreChange;
        
        public WinningResult(boolean success, String message, int totalScoreChange) {
            this.success = success;
            this.message = message;
            this.totalScoreChange = totalScoreChange;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public int getTotalScoreChange() {
            return totalScoreChange;
        }
    }
    
    /**
     * 中奖记录类
     */
    public static class PrizeRecord {
        private String issueNumber; // 期号
        private int prizeLevel;     // 中奖等级
        
        public PrizeRecord(String issueNumber, int prizeLevel) {
            this.issueNumber = issueNumber;
            this.prizeLevel = prizeLevel;
        }
        
        public String getIssueNumber() {
            return issueNumber;
        }
        
        public int getPrizeLevel() {
            return prizeLevel;
        }
    }
}
