package com.example.dlt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 彩票号码生成器类
 * 负责实现12种不同算法的彩票号码生成逻辑
 * 
 * 包含以下算法：
 * 1-6组：基于近10期历史数据的传统算法
 * 7-10组：基于所有历史数据的高级机器学习算法
 * 11组：完全随机算法
 * 12组：基于连号检测的智能预测算法
 */
public class NumberGenerator {
    
    /**
     * 生成结果封装类
     * 用于封装十二组号码生成结果的数据结构
     */
    public static class GenerationResult {
        private String displayText;          // 用于界面显示的格式化文本
        private List<List<Integer>> frontNumbers; // 十二组前区号码的二维列表
        private List<List<Integer>> backNumbers;  // 十二组后区号码的二维列表
        
        /**
         * 构造函数
         * @param displayText 格式化显示文本
         * @param frontNumbers 前区号码列表
         * @param backNumbers 后区号码列表
         */
        public GenerationResult(String displayText, List<List<Integer>> frontNumbers, List<List<Integer>> backNumbers) {
            this.displayText = displayText;
            this.frontNumbers = frontNumbers;
            this.backNumbers = backNumbers;
        }
        
        // Getter方法，提供对内部数据的只读访问
        public String getDisplayText() { return displayText; }
        public List<List<Integer>> getFrontNumbers() { return frontNumbers; }
        public List<List<Integer>> getBackNumbers() { return backNumbers; }
    }
    
    /**
     * 主入口方法：生成所有12组号码
     * 
     * 实现逻辑：
     * 1. 首先验证历史数据是否足够
     * 2. 统计近10期数据作为前6组算法的基础
     * 3. 按顺序生成前6组（传统算法）
     * 4. 按顺序生成后4组（机器学习算法）
     * 5. 按顺序生成第11组（完全随机算法）
     * 6. 按顺序生成第12组（连号检测算法）
     * 7. 格式化输出结果
     * 
     * @param historyList 历史开奖数据列表
     * @return GenerationResult 包含所有12组号码的结果对象
     */
    public static GenerationResult generateAllNumbers(List<LotteryEntry> historyList, Set<Integer> blockedRules) {
        // 第一步：过滤掉被屏蔽的期数，确保算法只使用有效数据
        List<LotteryEntry> filteredHistory = new ArrayList<>();
        for (LotteryEntry entry : historyList) {
            if (!entry.isBlocked()) {
                filteredHistory.add(entry);
            }
        }
        
        // 验证数据有效性：必须有至少一期有效数据才能进行预测
        if (filteredHistory.size() == 0) {
            return new GenerationResult("请先存入至少一期有效数据！", new ArrayList<>(), new ArrayList<>());
        }
        
        // 初始化结果存储容器：各存储12组前区和后区号码
        List<List<Integer>> resultFront = new ArrayList<>();
        List<List<Integer>> resultBack = new ArrayList<>();
        
        // =============================================================================
        // 第一阶段：统计近10期历史数据（作为前6组算法的数据基础）
        // =============================================================================
        
        // 统计容器：记录每个号码在近10期中的出现次数
        Map<Integer, Integer> frontCount = new HashMap<>();  // 前区号码统计
        Map<Integer, Integer> backCount = new HashMap<>();   // 后区号码统计
        
        // 集合容器：记录近10期中所有出现过的号码（去重）
        Set<Integer> usedFront = new HashSet<>();            // 前区已使用号码
        Set<Integer> usedBack = new HashSet<>();             // 后区已使用号码
        
        // 计算近10期的起始位置（如果数据不足10期，则从第0期开始）
        int start = Math.max(0, filteredHistory.size() - 10);
        
        // 遍历近10期有效数据，统计号码出现情况
        for (int i = start; i < filteredHistory.size(); i++) {
            LotteryEntry entry = filteredHistory.get(i);
            
            // 统计前区号码
            for (int n : entry.getFrontNumbers()) {
                usedFront.add(n);  // 添加到已使用集合
                frontCount.put(n, frontCount.getOrDefault(n, 0) + 1);  // 增加出现次数
            }
            
            // 统计后区号码
            for (int n : entry.getBackNumbers()) {
                usedBack.add(n);   // 添加到已使用集合
                backCount.put(n, backCount.getOrDefault(n, 0) + 1);   // 增加出现次数
            }
        }
        
        // =============================================================================
        // 第二阶段：生成前6组号码（基于近10期数据的传统算法）
        // =============================================================================
        generateBasicGroups(resultFront, resultBack, usedFront, usedBack, frontCount, backCount, blockedRules);
        
        // =============================================================================
        // 第三阶段：生成后4组号码（基于所有历史数据的高级算法）
        // =============================================================================
        
        // 第7组：马尔科夫链模型 - 分析号码间的转移概率关系
        if (!blockedRules.contains(7)) {
            resultFront.add(markovFrontNumbers(filteredHistory));
            resultBack.add(markovBackNumbers(filteredHistory));
        } else {
            resultFront.add(new ArrayList<>());
            resultBack.add(new ArrayList<>());
        }
        
        // 第8组：朴素贝叶斯模型 - 分析位置相关的号码分布特征
        if (!blockedRules.contains(8)) {
            resultFront.add(bayesFrontNumbers(filteredHistory));
            resultBack.add(bayesBackNumbers(filteredHistory));
        } else {
            resultFront.add(new ArrayList<>());
            resultBack.add(new ArrayList<>());
        }
        
        // 第9组：神经网络模拟模型 - 模拟多层神经网络的非线性变换
        if (!blockedRules.contains(9)) {
            resultFront.add(neuralNetworkFrontNumbers(filteredHistory));
            resultBack.add(neuralNetworkBackNumbers(filteredHistory));
        } else {
            resultFront.add(new ArrayList<>());
            resultBack.add(new ArrayList<>());
        }
        
        // 第10组：时间序列模型 - 分析号码出现的周期性规律
        if (!blockedRules.contains(10)) {
            resultFront.add(timeSeriesFrontNumbers(filteredHistory));
            resultBack.add(timeSeriesBackNumbers(filteredHistory));
        } else {
            resultFront.add(new ArrayList<>());
            resultBack.add(new ArrayList<>());
        }
        
        // 第11组：完全随机 - 纯随机选择号码，不依赖任何历史数据
        if (!blockedRules.contains(11)) {
            resultFront.add(generateRandomFrontNumbers());
            resultBack.add(generateRandomBackNumbers());
        } else {
            resultFront.add(new ArrayList<>());
            resultBack.add(new ArrayList<>());
        }
        
        // 第12组：连号检测预测算法 - 基于上一期连号情况的智能预测
        if (!blockedRules.contains(12)) {
            resultFront.add(consecutiveDetectionFrontNumbers(filteredHistory));
            resultBack.add(consecutiveDetectionBackNumbers(filteredHistory));
        } else {
            resultFront.add(new ArrayList<>());
            resultBack.add(new ArrayList<>());
        }
        
        // =============================================================================
        // 第四阶段：格式化输出结果
        // =============================================================================
        
        // 构建用于界面显示的格式化字符串
        StringBuilder sb = new StringBuilder();
        
        // 显示所有12组（包括被屏蔽的）
        for (int i = 0; i < 12; i++) {
            if (blockedRules.contains(i + 1)) {
                sb.append(String.format("第%d组：[已屏蔽] 不生成号码\n", i + 1));
            } else {
                sb.append(String.format("第%d组：前区%s 后区%s\n", 
                    i + 1, resultFront.get(i), resultBack.get(i)));
            }
        }
        
        return new GenerationResult(sb.toString(), resultFront, resultBack);
    }
    
    /**
     * 生成前6组基础算法号码
     * 这些算法都基于近10期历史数据进行分析和预测
     * 
     * 具体算法：
     * 第1组：排除法 - 从未出现的号码中选择
     * 第2组：高频法 - 选择出现次数最多的号码
     * 第3组：低频法 - 选择出现次数最少的号码
     * 第4组：混合法 - 结合高频和排除策略
     * 第5组：互斥法 - 排除前4组使用的号码
     * 第6组：集中法 - 选择前4组中出现最多的号码
     * 
     * @param resultFront 前区结果容器
     * @param resultBack 后区结果容器
     * @param usedFront 近10期中使用过的前区号码
     * @param usedBack 近10期中使用过的后区号码
     * @param frontCount 前区号码出现次数统计
     * @param backCount 后区号码出现次数统计
     */
    private static void generateBasicGroups(List<List<Integer>> resultFront, List<List<Integer>> resultBack,
                                          Set<Integer> usedFront, Set<Integer> usedBack,
                                          Map<Integer, Integer> frontCount, Map<Integer, Integer> backCount,
                                          Set<Integer> blockedRules) {
        
        // =============================================================================
        // 第1组：排除法算法
        // 逻辑：从近10期未出现的号码中随机选择，如果不够则从低频号码中补足
        // =============================================================================
        
        if (!blockedRules.contains(1)) {
            // 生成前区号码池：排除近10期中出现过的所有前区号码
            List<Integer> group1FrontPool = excludeSet(1, 35, usedFront);
            List<Integer> group1Front = new ArrayList<>();
            
            if (group1FrontPool.size() >= 5) {
                // 情况A：剩余号码足5个，直接从中随机选择5个
                group1Front = randomFromPool(group1FrontPool, 5);
            } else {
                // 情况B：剩余号码不足5个，需要补足逻辑
                group1Front.addAll(group1FrontPool);  // 先添加所有剩余号码
                
                // 补足策略：从近10期出现次数最少的前区号码中随机补足
                Map<Integer, Integer> frontCountCopy = new HashMap<>(frontCount);
                List<Integer> usedFrontList = new ArrayList<>(usedFront);
                
                // 找出最小出现次数
                int minCount = Integer.MAX_VALUE;
                for (int n : usedFrontList) {
                    int c = frontCountCopy.getOrDefault(n, 0);
                    if (c < minCount) minCount = c;
                }
                
                // 收集所有出现次数等于最小值的号码
                List<Integer> minList = new ArrayList<>();
                for (int n : usedFrontList) {
                    if (frontCountCopy.getOrDefault(n, 0) == minCount && !group1Front.contains(n)) {
                        minList.add(n);
                    }
                }
                
                // 随机打乱并补足到5个
                Collections.shuffle(minList);
                for (int i = 0; group1Front.size() < 5 && i < minList.size(); i++) {
                    group1Front.add(minList.get(i));
                }
            }
            Collections.sort(group1Front);  // 排序输出
            
            // 后区类似处理
            List<Integer> group1BackPool = excludeSet(1, 12, usedBack);
            List<Integer> group1Back = new ArrayList<>();
            if (group1BackPool.size() >= 2) {
                group1Back = randomFromPool(group1BackPool, 2);
            } else {
                group1Back.addAll(group1BackPool);
                // 补足逻辑：从近10期出现次数最少的后区号码中随机补足
                if (group1Back.size() < 2) {
                    Map<Integer, Integer> backCountCopy = new HashMap<>(backCount);
                    List<Integer> usedBackList = new ArrayList<>(usedBack);
                    int minCount = Integer.MAX_VALUE;
                    for (int n : usedBackList) {
                        int c = backCountCopy.getOrDefault(n, 0);
                        if (c < minCount) minCount = c;
                    }
                    List<Integer> minList = new ArrayList<>();
                    for (int n : usedBackList) {
                        if (backCountCopy.getOrDefault(n, 0) == minCount && !group1Back.contains(n)) {
                            minList.add(n);
                        }
                    }
                    Collections.shuffle(minList);
                    for (int i = 0; group1Back.size() < 2 && i < minList.size(); i++) {
                        group1Back.add(minList.get(i));
                    }
                }
            }
            Collections.sort(group1Back);
            resultFront.add(group1Front);
            resultBack.add(group1Back);
        } else {
            resultFront.add(new ArrayList<>());
            resultBack.add(new ArrayList<>());
        }
        
        // 第二组
        if (!blockedRules.contains(2)) {
            resultFront.add(topN(frontCount, 5, true, 1, 35));
            resultBack.add(topN(backCount, 2, true, 1, 12));
        } else {
            resultFront.add(new ArrayList<>());
            resultBack.add(new ArrayList<>());
        }
        
        // 第三组
        if (!blockedRules.contains(3)) {
            resultFront.add(topN(frontCount, 5, false, 1, 35, true));
            resultBack.add(topN(backCount, 2, false, 1, 12, true));
        } else {
            resultFront.add(new ArrayList<>());
            resultBack.add(new ArrayList<>());
        }
        
        // 第四组
        if (!blockedRules.contains(4)) {
            List<Integer> top2Front = topN(frontCount, 2, true, 1, 35);
            List<Integer> remainFront = excludeSet(1, 35, usedFront);
            remainFront.removeAll(top2Front);
            List<Integer> group4Front = new ArrayList<>(top2Front);
            group4Front.addAll(randomFromPool(remainFront, 3));
            resultFront.add(group4Front);
            
            List<Integer> top1Back = topN(backCount, 1, true, 1, 12);
            List<Integer> remainBack = excludeSet(1, 12, usedBack);
            remainBack.removeAll(top1Back);
            List<Integer> group4Back = new ArrayList<>(top1Back);
            if (remainBack.size() >= 1) {
                group4Back.addAll(randomFromPool(remainBack, 1));
            } else {
                // 剩余不够1个，从近10期出现次数最少的后区号码中随机补足
                Map<Integer, Integer> backCountCopy = new HashMap<>(backCount);
                List<Integer> usedBackList = new ArrayList<>(usedBack);
                int minCount = Integer.MAX_VALUE;
                for (int n : usedBackList) {
                    int c = backCountCopy.getOrDefault(n, 0);
                    if (c < minCount && !group4Back.contains(n)) minCount = c;
                }
                List<Integer> minList = new ArrayList<>();
                for (int n : usedBackList) {
                    if (backCountCopy.getOrDefault(n, 0) == minCount && !group4Back.contains(n)) {
                        minList.add(n);
                    }
                }
                Collections.shuffle(minList);
                if (!minList.isEmpty()) {
                    group4Back.add(minList.get(0));
                }
            }
            Collections.sort(group4Back);
            resultBack.add(group4Back);
        } else {
            resultFront.add(new ArrayList<>());
            resultBack.add(new ArrayList<>());
        }
        
        // 第五组
        if (!blockedRules.contains(5)) {
            Set<Integer> usedFront4 = new HashSet<>();
            Set<Integer> usedBack4 = new HashSet<>();
            for (int i = 0; i < 4; i++) {
                if (!blockedRules.contains(i + 1)) {  // 只统计未被屏蔽的组
                    usedFront4.addAll(resultFront.get(i));
                    usedBack4.addAll(resultBack.get(i));
                }
            }
            resultFront.add(randomFromPool(excludeSet(1, 35, usedFront4), 5));
            resultBack.add(randomFromPool(excludeSet(1, 12, usedBack4), 2));
        } else {
            resultFront.add(new ArrayList<>());
            resultBack.add(new ArrayList<>());
        }
        
        // 第六组
        if (!blockedRules.contains(6)) {
            Map<Integer, Integer> frontCount4 = new HashMap<>();
            Map<Integer, Integer> backCount4 = new HashMap<>();
            for (int i = 0; i < 4; i++) {
                if (!blockedRules.contains(i + 1)) {  // 只统计未被屏蔽的组
                    for (int n : resultFront.get(i)) frontCount4.put(n, frontCount4.getOrDefault(n, 0) + 1);
                    for (int n : resultBack.get(i)) backCount4.put(n, backCount4.getOrDefault(n, 0) + 1);
                }
            }
            resultFront.add(topN(frontCount4, 5, true, 1, 35));
            resultBack.add(topN(backCount4, 2, true, 1, 12));
        } else {
            resultFront.add(new ArrayList<>());
            resultBack.add(new ArrayList<>());
        }
    }
    
    // =============================================================================
    // 工具方法区域：提供基础的数据处理和选择功能
    // =============================================================================
    
    /**
     * 从指定数据池中随机选择指定数量的号码
     * 如果数据池不足，会从1-35中补充缺失的号码
     * 
     * @param pool 可选号码池
     * @param count 需要选择的号码数量
     * @return 排序后的号码列表
     */
    private static List<Integer> randomFromPool(List<Integer> pool, int count) {
        // 复制数据池防止修改原数据
        List<Integer> copy = new ArrayList<>(pool);
        Collections.shuffle(copy);  // 随机打乱顺序
        
        // 如果数据池不足，从1-35中补充缺失的号码
        if (copy.size() < count) {
            for (int i = 1; copy.size() < count && i <= 35; i++) {
                if (!copy.contains(i)) copy.add(i);
            }
        }
        
        // 取前 count 个号码并排序返回
        List<Integer> res = copy.subList(0, Math.min(count, copy.size()));
        Collections.sort(res);
        return res;
    }
    
    /**
     * 从指定范围中排除特定号码集合，返回剩余号码列表
     * 
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @param exclude 需要排除的号码集合
     * @return 排除后的号码列表
     */
    private static List<Integer> excludeSet(int min, int max, Set<Integer> exclude) {
        List<Integer> res = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            if (!exclude.contains(i)) res.add(i);
        }
        return res;
    }
    
    private static List<Integer> topN(Map<Integer, Integer> countMap, int n, boolean most, int min, int max) {
        return topN(countMap, n, most, min, max, false);
    }
    
    private static List<Integer> topN(Map<Integer, Integer> countMap, int n, boolean most, int min, int max, boolean onlyUsed) {
        List<Integer> all = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            int count = countMap.getOrDefault(i, 0);
            if (!onlyUsed || count > 0) {
                all.add(i);
            }
        }
        if (all.isEmpty()) return new ArrayList<>();
        
        int targetCount = countMap.getOrDefault(all.get(0), 0);
        for (int i : all) {
            int c = countMap.getOrDefault(i, 0);
            if (most) {
                if (c > targetCount) targetCount = c;
            } else {
                if (c < targetCount) targetCount = c;
            }
        }
        
        List<Integer> candidates = new ArrayList<>();
        for (int i : all) {
            if (countMap.getOrDefault(i, 0) == targetCount) {
                candidates.add(i);
            }
        }
        Collections.shuffle(candidates);
        List<Integer> res = new ArrayList<>();
        for (int i = 0; i < Math.min(n, candidates.size()); i++) {
            res.add(candidates.get(i));
        }
        
        if (res.size() < n) {
            all.removeAll(res);
            all.sort(new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    int c1 = countMap.getOrDefault(o1, 0);
                    int c2 = countMap.getOrDefault(o2, 0);
                    return most ? Integer.compare(c2, c1) : Integer.compare(c1, c2);
                }
            });
            for (int i = 0; i < all.size() && res.size() < n; i++) {
                res.add(all.get(i));
            }
        }
        Collections.sort(res);
        return res;
    }
    
    // 马尔科夫链模型生成前区号码 - 基于号码集合的条件概率模型
    private static List<Integer> markovFrontNumbers(List<LotteryEntry> historyList) {
        if (historyList.size() < 2) return randomFromPool(excludeSet(1, 35, new HashSet<>()), 5);
        
        // 1. 构建转移矩阵：Set<Integer> → Map<Integer, Integer>
        Map<Set<Integer>, Map<Integer, Integer>> trans = new HashMap<>();
        
        for (int i = 0; i < historyList.size() - 1; i++) {
            Set<Integer> currentSet = new HashSet<>(historyList.get(i).getFrontNumbers());
            List<Integer> next = historyList.get(i + 1).getFrontNumbers();
            
            trans.putIfAbsent(currentSet, new HashMap<>());
            Map<Integer, Integer> nextCount = trans.get(currentSet);
            
            for (int num : next) {
                nextCount.put(num, nextCount.getOrDefault(num, 0) + 1);
            }
        }
        
        // 2. 获取最近一期的号码集合
        Set<Integer> lastSet = new HashSet<>(historyList.get(historyList.size() - 1).getFrontNumbers());
        Map<Integer, Integer> nextProb = trans.get(lastSet);
        
        // 3. 拉普拉斯平滑（+1平滑）
        Map<Integer, Integer> smoothedProb = new HashMap<>();
        for (int num = 1; num <= 35; num++) {
            int count = (nextProb != null) ? nextProb.getOrDefault(num, 0) : 0;
            smoothedProb.put(num, count + 1);
        }
        
        // 4. 基于平滑后的概率独立采样5个号码
        List<Integer> res = new ArrayList<>();
        Random rand = new Random();
        
        while (res.size() < 5) {
            int total = 0;
            for (Map.Entry<Integer, Integer> entry : smoothedProb.entrySet()) {
                if (!res.contains(entry.getKey())) {
                    total += entry.getValue();
                }
            }
            
            int r = rand.nextInt(total);
            int acc = 0;
            
            for (Map.Entry<Integer, Integer> entry : smoothedProb.entrySet()) {
                int num = entry.getKey();
                if (!res.contains(num)) {
                    acc += entry.getValue();
                    if (r < acc) {
                        res.add(num);
                        break;
                    }
                }
            }
        }
        
        Collections.sort(res);
        return res;
    }
    
    // 马尔科夫链模型生成后区号码 - 基于号码集合的条件概率模型
    private static List<Integer> markovBackNumbers(List<LotteryEntry> historyList) {
        if (historyList.size() < 2) return randomFromPool(excludeSet(1, 12, new HashSet<>()), 2);
        
        // 1. 构建转移矩阵：Set<Integer> → Map<Integer, Integer>
        Map<Set<Integer>, Map<Integer, Integer>> trans = new HashMap<>();
        
        for (int i = 0; i < historyList.size() - 1; i++) {
            Set<Integer> currentSet = new HashSet<>(historyList.get(i).getBackNumbers());
            List<Integer> next = historyList.get(i + 1).getBackNumbers();
            
            trans.putIfAbsent(currentSet, new HashMap<>());
            Map<Integer, Integer> nextCount = trans.get(currentSet);
            
            for (int num : next) {
                nextCount.put(num, nextCount.getOrDefault(num, 0) + 1);
            }
        }
        
        // 2. 获取最近一期的号码集合
        Set<Integer> lastSet = new HashSet<>(historyList.get(historyList.size() - 1).getBackNumbers());
        Map<Integer, Integer> nextProb = trans.get(lastSet);
        
        // 3. 拉普拉斯平滑（+1平滑）
        Map<Integer, Integer> smoothedProb = new HashMap<>();
        for (int num = 1; num <= 12; num++) {
            int count = (nextProb != null) ? nextProb.getOrDefault(num, 0) : 0;
            smoothedProb.put(num, count + 1);
        }
        
        // 4. 基于平滑后的概率独立采样2个号码
        List<Integer> res = new ArrayList<>();
        Random rand = new Random();
        
        while (res.size() < 2) {
            int total = 0;
            for (Map.Entry<Integer, Integer> entry : smoothedProb.entrySet()) {
                if (!res.contains(entry.getKey())) {
                    total += entry.getValue();
                }
            }
            
            int r = rand.nextInt(total);
            int acc = 0;
            
            for (Map.Entry<Integer, Integer> entry : smoothedProb.entrySet()) {
                int num = entry.getKey();
                if (!res.contains(num)) {
                    acc += entry.getValue();
                    if (r < acc) {
                        res.add(num);
                        break;
                    }
                }
            }
        }
        
        Collections.sort(res);
        return res;
    }
    
    private static List<Integer> bayesFrontNumbers(List<LotteryEntry> historyList) {
        if (historyList.size() == 0) return randomFromPool(excludeSet(1, 35, new HashSet<>()), 5);
        int[][] posCount = new int[5][36];
        for (LotteryEntry entry : historyList) {
            List<Integer> f = entry.getFrontNumbers();
            for (int i = 0; i < 5; i++) {
                int n = f.get(i);
                posCount[i][n]++;
            }
        }
        List<Integer> res = new ArrayList<>();
        Random rand = new Random();
        
        // 改进的选择逻辑：确保选出5个不重复的号码
        for (int i = 0; i < 5; i++) {
            boolean found = false;
            int attempts = 0;
            int maxAttempts = 35; // 最大尝试次数
            
            while (!found && attempts < maxAttempts) {
                int total = 0;
                // 只计算还未选中的号码的总权重
                for (int n = 1; n <= 35; n++) {
                    if (!res.contains(n)) {
                        total += posCount[i][n] + 1;
                    }
                }
                
                if (total == 0) break; // 如果没有可选号码，退出
                
                int r = rand.nextInt(total);
                int acc = 0;
                
                for (int n = 1; n <= 35; n++) {
                    if (!res.contains(n)) {
                        acc += posCount[i][n] + 1;
                        if (r < acc) {
                            res.add(n);
                            found = true;
                            break;
                        }
                    }
                }
                attempts++;
            }
            
            // 如果仍然没有找到，从剩余号码中随机选择一个
            if (!found) {
                List<Integer> remaining = new ArrayList<>();
                for (int n = 1; n <= 35; n++) {
                    if (!res.contains(n)) {
                        remaining.add(n);
                    }
                }
                if (!remaining.isEmpty()) {
                    res.add(remaining.get(rand.nextInt(remaining.size())));
                }
            }
        }
        
        // 确保结果有5个号码，如果不够则随机补足
        while (res.size() < 5) {
            List<Integer> remaining = new ArrayList<>();
            for (int n = 1; n <= 35; n++) {
                if (!res.contains(n)) {
                    remaining.add(n);
                }
            }
            if (!remaining.isEmpty()) {
                res.add(remaining.get(rand.nextInt(remaining.size())));
            } else {
                break; // 如果没有剩余号码，退出
            }
        }
        
        Collections.sort(res);
        return res;
    }
    
    private static List<Integer> bayesBackNumbers(List<LotteryEntry> historyList) {
        if (historyList.size() == 0) return randomFromPool(excludeSet(1, 12, new HashSet<>()), 2);
        int[][] posCount = new int[2][13];
        for (LotteryEntry entry : historyList) {
            List<Integer> b = entry.getBackNumbers();
            for (int i = 0; i < 2; i++) {
                int n = b.get(i);
                posCount[i][n]++;
            }
        }
        List<Integer> res = new ArrayList<>();
        Random rand = new Random();
        
        // 改进的选择逻辑：确保选出2个不重复的号码
        for (int i = 0; i < 2; i++) {
            boolean found = false;
            int attempts = 0;
            int maxAttempts = 12; // 最大尝试次数
            
            while (!found && attempts < maxAttempts) {
                int total = 0;
                // 只计算还未选中的号码的总权重
                for (int n = 1; n <= 12; n++) {
                    if (!res.contains(n)) {
                        total += posCount[i][n] + 1;
                    }
                }
                
                if (total == 0) break; // 如果没有可选号码，退出
                
                int r = rand.nextInt(total);
                int acc = 0;
                
                for (int n = 1; n <= 12; n++) {
                    if (!res.contains(n)) {
                        acc += posCount[i][n] + 1;
                        if (r < acc) {
                            res.add(n);
                            found = true;
                            break;
                        }
                    }
                }
                attempts++;
            }
            
            // 如果仍然没有找到，从剩余号码中随机选择一个
            if (!found) {
                List<Integer> remaining = new ArrayList<>();
                for (int n = 1; n <= 12; n++) {
                    if (!res.contains(n)) {
                        remaining.add(n);
                    }
                }
                if (!remaining.isEmpty()) {
                    res.add(remaining.get(rand.nextInt(remaining.size())));
                }
            }
        }
        
        // 确保结果有2个号码，如果不够则随机补足
        while (res.size() < 2) {
            List<Integer> remaining = new ArrayList<>();
            for (int n = 1; n <= 12; n++) {
                if (!res.contains(n)) {
                    remaining.add(n);
                }
            }
            if (!remaining.isEmpty()) {
                res.add(remaining.get(rand.nextInt(remaining.size())));
            } else {
                break; // 如果没有剩余号码，退出
            }
        }
        
        Collections.sort(res);
        return res;
    }
    
    private static List<Integer> neuralNetworkFrontNumbers(List<LotteryEntry> historyList) {
        if (historyList.size() < 10) return randomFromPool(excludeSet(1, 35, new HashSet<>()), 5);
        
        double[][] inputs = new double[10][35];
        int startIdx = Math.max(0, historyList.size() - 10);
        
        for (int i = 0; i < 10; i++) {
            if (startIdx + i < historyList.size()) {
                List<Integer> numbers = historyList.get(startIdx + i).getFrontNumbers();
                for (int num : numbers) {
                    inputs[i][num - 1] = 1.0;
                }
            }
        }
        
        double[] hiddenLayer = new double[20];
        double[] outputLayer = new double[35];
        Random rand = new Random();
        
        for (int h = 0; h < 20; h++) {
            double sum = 0;
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 35; j++) {
                    double weight = (rand.nextGaussian() * 0.5 + 0.1);
                    sum += inputs[i][j] * weight;
                }
            }
            hiddenLayer[h] = 1.0 / (1.0 + Math.exp(-sum));
        }
        
        for (int o = 0; o < 35; o++) {
            double sum = 0;
            for (int h = 0; h < 20; h++) {
                double weight = (rand.nextGaussian() * 0.5 + 0.1);
                sum += hiddenLayer[h] * weight;
            }
            outputLayer[o] = 1.0 / (1.0 + Math.exp(-sum));
        }
        
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            double totalProb = 0;
            for (int j = 0; j < 35; j++) {
                if (!result.contains(j + 1)) {
                    totalProb += outputLayer[j];
                }
            }
            
            double r = rand.nextDouble() * totalProb;
            double acc = 0;
            
            for (int j = 0; j < 35; j++) {
                if (!result.contains(j + 1)) {
                    acc += outputLayer[j];
                    if (r <= acc) {
                        result.add(j + 1);
                        break;
                    }
                }
            }
        }
        
        Collections.sort(result);
        return result;
    }
    
    private static List<Integer> neuralNetworkBackNumbers(List<LotteryEntry> historyList) {
        if (historyList.size() < 10) return randomFromPool(excludeSet(1, 12, new HashSet<>()), 2);
        
        double[][] inputs = new double[10][12];
        int startIdx = Math.max(0, historyList.size() - 10);
        
        for (int i = 0; i < 10; i++) {
            if (startIdx + i < historyList.size()) {
                List<Integer> numbers = historyList.get(startIdx + i).getBackNumbers();
                for (int num : numbers) {
                    inputs[i][num - 1] = 1.0;
                }
            }
        }
        
        double[] hiddenLayer = new double[8];
        double[] outputLayer = new double[12];
        Random rand = new Random();
        
        for (int h = 0; h < 8; h++) {
            double sum = 0;
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 12; j++) {
                    double weight = (rand.nextGaussian() * 0.5 + 0.1);
                    sum += inputs[i][j] * weight;
                }
            }
            hiddenLayer[h] = 1.0 / (1.0 + Math.exp(-sum));
        }
        
        for (int o = 0; o < 12; o++) {
            double sum = 0;
            for (int h = 0; h < 8; h++) {
                double weight = (rand.nextGaussian() * 0.5 + 0.1);
                sum += hiddenLayer[h] * weight;
            }
            outputLayer[o] = 1.0 / (1.0 + Math.exp(-sum));
        }
        
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            double totalProb = 0;
            for (int j = 0; j < 12; j++) {
                if (!result.contains(j + 1)) {
                    totalProb += outputLayer[j];
                }
            }
            
            double r = rand.nextDouble() * totalProb;
            double acc = 0;
            
            for (int j = 0; j < 12; j++) {
                if (!result.contains(j + 1)) {
                    acc += outputLayer[j];
                    if (r <= acc) {
                        result.add(j + 1);
                        break;
                    }
                }
            }
        }
        
        Collections.sort(result);
        return result;
    }
    
    private static List<Integer> timeSeriesFrontNumbers(List<LotteryEntry> historyList) {
        if (historyList.size() < 10) return randomFromPool(excludeSet(1, 35, new HashSet<>()), 5);
        
        Map<Integer, List<Integer>> numberPositions = new HashMap<>();
        
        for (int i = 0; i < historyList.size(); i++) {
            List<Integer> numbers = historyList.get(i).getFrontNumbers();
            for (int num : numbers) {
                numberPositions.putIfAbsent(num, new ArrayList<>());
                numberPositions.get(num).add(i);
            }
        }
        
        Map<Integer, Double> numberScores = new HashMap<>();
        int currentPeriod = historyList.size();
        
        for (int num = 1; num <= 35; num++) {
            double score = 0.0;
            List<Integer> positions = numberPositions.get(num);
            
            if (positions != null && positions.size() > 1) {
                List<Integer> intervals = new ArrayList<>();
                for (int i = 1; i < positions.size(); i++) {
                    intervals.add(positions.get(i) - positions.get(i - 1));
                }
                
                Map<Integer, Integer> intervalCount = new HashMap<>();
                for (int interval : intervals) {
                    intervalCount.put(interval, intervalCount.getOrDefault(interval, 0) + 1);
                }
                
                int mostCommonInterval = 0;
                int maxCount = 0;
                for (Map.Entry<Integer, Integer> entry : intervalCount.entrySet()) {
                    if (entry.getValue() > maxCount) {
                        maxCount = entry.getValue();
                        mostCommonInterval = entry.getKey();
                    }
                }
                
                double cyclicScore = 0;
                if (mostCommonInterval > 0 && maxCount > 1) {
                    int lastPosition = positions.get(positions.size() - 1);
                    int expectedNext = lastPosition + mostCommonInterval;
                    cyclicScore = Math.max(0, 10 - Math.abs(currentPeriod - expectedNext));
                    score += cyclicScore;
                }
                
                double recentScore = 0;
                int recentPeriods = Math.min(10, historyList.size());
                int recentCount = 0;
                
                for (int pos : positions) {
                    if (pos >= historyList.size() - recentPeriods) {
                        recentCount++;
                    }
                }
                
                recentScore = (double) recentCount / recentPeriods * 5;
                
                int lastAppearance = positions.get(positions.size() - 1);
                int gapFromLast = currentPeriod - lastAppearance - 1;
                double coldScore = Math.min(5, gapFromLast * 0.5);
                
                score = cyclicScore * 0.5 + recentScore * 0.3 + coldScore * 0.2;
            } else {
                score = 1.0;
            }
            
            numberScores.put(num, score);
        }
        
        List<Integer> result = new ArrayList<>();
        Random rand = new Random();
        
        for (int i = 0; i < 5; i++) {
            double totalScore = 0;
            for (int num = 1; num <= 35; num++) {
                if (!result.contains(num)) {
                    totalScore += Math.max(0.1, numberScores.get(num));
                }
            }
            
            double r = rand.nextDouble() * totalScore;
            double acc = 0;
            
            for (int num = 1; num <= 35; num++) {
                if (!result.contains(num)) {
                    acc += Math.max(0.1, numberScores.get(num));
                    if (r <= acc) {
                        result.add(num);
                        break;
                    }
                }
            }
        }
        
        Collections.sort(result);
        return result;
    }
    
    private static List<Integer> timeSeriesBackNumbers(List<LotteryEntry> historyList) {
        if (historyList.size() < 10) return randomFromPool(excludeSet(1, 12, new HashSet<>()), 2);
        
        Map<Integer, List<Integer>> numberPositions = new HashMap<>();
        
        for (int i = 0; i < historyList.size(); i++) {
            List<Integer> numbers = historyList.get(i).getBackNumbers();
            for (int num : numbers) {
                numberPositions.putIfAbsent(num, new ArrayList<>());
                numberPositions.get(num).add(i);
            }
        }
        
        Map<Integer, Double> numberScores = new HashMap<>();
        int currentPeriod = historyList.size();
        
        for (int num = 1; num <= 12; num++) {
            double score = 0.0;
            List<Integer> positions = numberPositions.get(num);
            
            if (positions != null && positions.size() > 1) {
                List<Integer> intervals = new ArrayList<>();
                for (int i = 1; i < positions.size(); i++) {
                    intervals.add(positions.get(i) - positions.get(i - 1));
                }
                
                Map<Integer, Integer> intervalCount = new HashMap<>();
                for (int interval : intervals) {
                    intervalCount.put(interval, intervalCount.getOrDefault(interval, 0) + 1);
                }
                
                int mostCommonInterval = 0;
                int maxCount = 0;
                for (Map.Entry<Integer, Integer> entry : intervalCount.entrySet()) {
                    if (entry.getValue() > maxCount) {
                        maxCount = entry.getValue();
                        mostCommonInterval = entry.getKey();
                    }
                }
                
                double cyclicScore = 0;
                if (mostCommonInterval > 0 && maxCount > 1) {
                    int lastPosition = positions.get(positions.size() - 1);
                    int expectedNext = lastPosition + mostCommonInterval;
                    cyclicScore = Math.max(0, 10 - Math.abs(currentPeriod - expectedNext));
                    score += cyclicScore;
                }
                
                int recentPeriods = Math.min(10, historyList.size());
                int recentCount = 0;
                for (int pos : positions) {
                    if (pos >= historyList.size() - recentPeriods) {
                        recentCount++;
                    }
                }
                double recentScore = (double) recentCount / recentPeriods * 5;
                
                int lastAppearance = positions.get(positions.size() - 1);
                int gapFromLast = currentPeriod - lastAppearance - 1;
                double coldScore = Math.min(5, gapFromLast * 0.5);
                
                score = cyclicScore * 0.5 + recentScore * 0.3 + coldScore * 0.2;
            } else {
                score = 1.0;
            }
            
            numberScores.put(num, score);
        }
        
        List<Integer> result = new ArrayList<>();
        Random rand = new Random();
        
        for (int i = 0; i < 2; i++) {
            double totalScore = 0;
            for (int num = 1; num <= 12; num++) {
                if (!result.contains(num)) {
                    totalScore += Math.max(0.1, numberScores.get(num));
                }
            }
            
            double r = rand.nextDouble() * totalScore;
            double acc = 0;
            
            for (int num = 1; num <= 12; num++) {
                if (!result.contains(num)) {
                    acc += Math.max(0.1, numberScores.get(num));
                    if (r <= acc) {
                        result.add(num);
                        break;
                    }
                }
            }
        }
        
        Collections.sort(result);
        return result;
    }
    
    // =============================================================================
    // 第11组：完全随机算法
    // =============================================================================
    
    /**
     * 生成完全随机的前区号码
     * 不依赖任何历史数据，纯随机选择
     * 
     * @return 随机生成的5个前区号码
     */
    private static List<Integer> generateRandomFrontNumbers() {
        List<Integer> pool = new ArrayList<>();
        for (int i = 1; i <= 35; i++) {
            pool.add(i);
        }
        
        Collections.shuffle(pool);
        List<Integer> result = pool.subList(0, 5);
        Collections.sort(result);
        return result;
    }
    
    /**
     * 生成完全随机的后区号码
     * 不依赖任何历史数据，纯随机选择
     * 
     * @return 随机生成的2个后区号码
     */
    private static List<Integer> generateRandomBackNumbers() {
        List<Integer> pool = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            pool.add(i);
        }
        
        Collections.shuffle(pool);
        List<Integer> result = pool.subList(0, 2);
        Collections.sort(result);
        return result;
    }
    
    // =============================================================================
    // 第12组：连号检测预测算法
    // =============================================================================
    
    /**
     * 第12组前区号码生成算法 - 基于连续数字检测的智能预测
     * 
     * 规则：
     * 1. 检查上一期前区是否存在连续数字（例如：12,13）
     * 2. 情况A（上一期有连号）：
     *    - 从近十期前区从未出现过的号码中，随机选择1至3个
     *    - 从近十期前区出现次数最少的号码中，随机选择1至3个
     * 3. 情况B（上一期无连号）：
     *    - 从近十期前区从未出现过的号码中，随机选择1个
     *    - 从近十期前区出现次数最少的号码中，随机选择2个
     *    - 从近十期前区出现次数第二少的号码中，随机选择2个连续的号码
     * 
     * @param historyList 历史开奖数据列表
     * @return 预测的5个前区号码
     */
    private static List<Integer> consecutiveDetectionFrontNumbers(List<LotteryEntry> historyList) {
        if (historyList.isEmpty()) {
            return generateRandomFrontNumbers();
        }
        
        // 统计近10期前区号码使用情况
        Map<Integer, Integer> frontCount = new HashMap<>();
        Set<Integer> usedFront = new HashSet<>();
        int start = Math.max(0, historyList.size() - 10);
        
        for (int i = start; i < historyList.size(); i++) {
            LotteryEntry entry = historyList.get(i);
            for (int n : entry.getFrontNumbers()) {
                usedFront.add(n);
                frontCount.put(n, frontCount.getOrDefault(n, 0) + 1);
            }
        }
        
        // 获取上一期前区号码并检查是否有连号
        List<Integer> lastFront = historyList.get(historyList.size() - 1).getFrontNumbers();
        boolean hasConsecutive = hasConsecutiveNumbers(lastFront);
        
        // 准备候选号码池
        List<Integer> neverAppeared = excludeSet(1, 35, usedFront); // 从未出现的号码
        List<Integer> leastFrequent = getLeastFrequentNumbers(frontCount, usedFront, 1); // 出现次数最少的号码
        List<Integer> secondLeastFrequent = getSecondLeastFrequentNumbers(frontCount, usedFront); // 出现次数第二少的号码
        
        List<Integer> result = new ArrayList<>();
        Random rand = new Random();
        
        if (hasConsecutive) {
            // 情况A：上一期有连号
            // 从从未出现的号码中选择1-3个
            int neverCount = Math.min(3, Math.max(1, neverAppeared.size()));
            if (rand.nextBoolean() && neverCount > 1) neverCount = rand.nextInt(neverCount) + 1;
            Collections.shuffle(neverAppeared);
            for (int i = 0; i < Math.min(neverCount, neverAppeared.size()); i++) {
                result.add(neverAppeared.get(i));
            }
            
            // 从出现次数最少的号码中补足到5个
            Collections.shuffle(leastFrequent);
            for (int num : leastFrequent) {
                if (result.size() >= 5) break;
                if (!result.contains(num)) {
                    result.add(num);
                }
            }
        } else {
            // 情况B：上一期无连号
            // 从从未出现的号码中选择1个
            if (!neverAppeared.isEmpty()) {
                Collections.shuffle(neverAppeared);
                result.add(neverAppeared.get(0));
            }
            
            // 从出现次数最少的号码中选择2个
            Collections.shuffle(leastFrequent);
            int addedFromLeast = 0;
            for (int num : leastFrequent) {
                if (addedFromLeast >= 2) break;
                if (!result.contains(num)) {
                    result.add(num);
                    addedFromLeast++;
                }
            }
            
            // 从出现次数第二少的号码中选择2个连续的号码
            List<Integer> consecutivePair = findConsecutivePair(secondLeastFrequent, result);
            result.addAll(consecutivePair);
        }
        
        // 如果仍不足5个，从所有可用号码中随机补足
        while (result.size() < 5) {
            List<Integer> remaining = new ArrayList<>();
            for (int i = 1; i <= 35; i++) {
                if (!result.contains(i)) {
                    remaining.add(i);
                }
            }
            if (!remaining.isEmpty()) {
                Collections.shuffle(remaining);
                result.add(remaining.get(0));
            } else {
                break;
            }
        }
        
        Collections.sort(result);
        return result;
    }
    
    /**
     * 第12组后区号码生成算法
     * 
     * 规则：
     * 1. 从近十期后区从未出现过的号码中，随机选择1个
     * 2. 从近十期后区出现次数最少的号码中，随机选择1个
     * 3. 重要约束：选择的这两个号码，绝对不能是上一期后区已经出现过的号码
     * 
     * @param historyList 历史开奖数据列表
     * @return 预测的2个后区号码
     */
    private static List<Integer> consecutiveDetectionBackNumbers(List<LotteryEntry> historyList) {
        if (historyList.isEmpty()) {
            return generateRandomBackNumbers();
        }
        
        // 统计近10期后区号码使用情况
        Map<Integer, Integer> backCount = new HashMap<>();
        Set<Integer> usedBack = new HashSet<>();
        int start = Math.max(0, historyList.size() - 10);
        
        for (int i = start; i < historyList.size(); i++) {
            LotteryEntry entry = historyList.get(i);
            for (int n : entry.getBackNumbers()) {
                usedBack.add(n);
                backCount.put(n, backCount.getOrDefault(n, 0) + 1);
            }
        }
        
        // 获取上一期后区号码（约束条件）
        Set<Integer> lastBack = new HashSet<>(historyList.get(historyList.size() - 1).getBackNumbers());
        
        // 准备候选号码池（排除上一期后区号码）
        List<Integer> neverAppeared = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            if (!usedBack.contains(i) && !lastBack.contains(i)) {
                neverAppeared.add(i);
            }
        }
        
        List<Integer> leastFrequent = new ArrayList<>();
        if (!usedBack.isEmpty()) {
            int minCount = Collections.min(backCount.values());
            for (Map.Entry<Integer, Integer> entry : backCount.entrySet()) {
                if (entry.getValue() == minCount && !lastBack.contains(entry.getKey())) {
                    leastFrequent.add(entry.getKey());
                }
            }
        }
        
        List<Integer> result = new ArrayList<>();
        Random rand = new Random();
        
        // 从从未出现的号码中选择1个
        if (!neverAppeared.isEmpty()) {
            Collections.shuffle(neverAppeared);
            result.add(neverAppeared.get(0));
        }
        
        // 从出现次数最少的号码中选择1个
        if (!leastFrequent.isEmpty()) {
            Collections.shuffle(leastFrequent);
            for (int num : leastFrequent) {
                if (!result.contains(num)) {
                    result.add(num);
                    break;
                }
            }
        }
        
        // 如果仍不足2个，从符合约束的号码中随机补足
        while (result.size() < 2) {
            List<Integer> remaining = new ArrayList<>();
            for (int i = 1; i <= 12; i++) {
                if (!result.contains(i) && !lastBack.contains(i)) {
                    remaining.add(i);
                }
            }
            if (!remaining.isEmpty()) {
                Collections.shuffle(remaining);
                result.add(remaining.get(0));
            } else {
                // 如果所有号码都被约束，则从次级候选中选择
                for (int i = 1; i <= 12; i++) {
                    if (!result.contains(i)) {
                        result.add(i);
                        break;
                    }
                }
            }
        }
        
        Collections.sort(result);
        return result;
    }
    
    /**
     * 检查一组号码中是否存在连续数字
     * 
     * @param numbers 要检查的号码列表
     * @return 如果存在连续数字返回true，否则返回false
     */
    private static boolean hasConsecutiveNumbers(List<Integer> numbers) {
        List<Integer> sorted = new ArrayList<>(numbers);
        Collections.sort(sorted);
        
        for (int i = 0; i < sorted.size() - 1; i++) {
            if (sorted.get(i + 1) - sorted.get(i) == 1) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取出现次数最少的号码列表
     * 
     * @param countMap 号码出现次数统计
     * @param usedNumbers 已使用过的号码集合
     * @param minOccurrence 最小出现次数
     * @return 出现次数最少的号码列表
     */
    private static List<Integer> getLeastFrequentNumbers(Map<Integer, Integer> countMap, Set<Integer> usedNumbers, int minOccurrence) {
        List<Integer> result = new ArrayList<>();
        
        if (usedNumbers.isEmpty()) {
            return result;
        }
        
        int minCount = Collections.min(countMap.values());
        if (minCount >= minOccurrence) {
            for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
                if (entry.getValue() == minCount) {
                    result.add(entry.getKey());
                }
            }
        }
        
        return result;
    }
    
    /**
     * 获取出现次数第二少的号码列表
     * 
     * @param countMap 号码出现次数统计
     * @param usedNumbers 已使用过的号码集合
     * @return 出现次数第二少的号码列表
     */
    private static List<Integer> getSecondLeastFrequentNumbers(Map<Integer, Integer> countMap, Set<Integer> usedNumbers) {
        List<Integer> result = new ArrayList<>();
        
        if (usedNumbers.isEmpty() || countMap.isEmpty()) {
            return result;
        }
        
        Set<Integer> uniqueCounts = new HashSet<>(countMap.values());
        List<Integer> sortedCounts = new ArrayList<>(uniqueCounts);
        Collections.sort(sortedCounts);
        
        if (sortedCounts.size() >= 2) {
            int secondMinCount = sortedCounts.get(1);
            for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
                if (entry.getValue() == secondMinCount) {
                    result.add(entry.getKey());
                }
            }
        }
        
        return result;
    }
    
    /**
     * 从给定号码列表中找到一对连续的号码
     * 
     * @param candidates 候选号码列表
     * @param excluded 要排除的号码列表
     * @return 连续号码对，如果找不到则返回空列表或单个号码
     */
    private static List<Integer> findConsecutivePair(List<Integer> candidates, List<Integer> excluded) {
        List<Integer> result = new ArrayList<>();
        List<Integer> available = new ArrayList<>();
        
        // 过滤掉已排除的号码
        for (int num : candidates) {
            if (!excluded.contains(num)) {
                available.add(num);
            }
        }
        
        Collections.sort(available);
        
        // 寻找所有连续的号码对
        List<List<Integer>> consecutivePairs = new ArrayList<>();
        for (int i = 0; i < available.size() - 1; i++) {
            if (available.get(i + 1) - available.get(i) == 1) {
                List<Integer> pair = new ArrayList<>();
                pair.add(available.get(i));
                pair.add(available.get(i + 1));
                consecutivePairs.add(pair);
            }
        }
        
        // 随机选择一组连续号码
        if (!consecutivePairs.isEmpty()) {
            Random rand = new Random();
            List<Integer> selectedPair = consecutivePairs.get(rand.nextInt(consecutivePairs.size()));
            result.addAll(selectedPair);
            return result;
        }
        
        // 如果没有找到连续对，则随机选择2个号码
        Collections.shuffle(available);
        for (int i = 0; i < Math.min(2, available.size()); i++) {
            result.add(available.get(i));
        }
        
        return result;
    }
}