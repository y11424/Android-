package com.example.dlt;

// Android 核心组件导入
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Android UI 组件导入
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

// Java 标准库导入
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

// 对话框和权限相关导入
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// 文件操作相关导入
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import android.content.Intent;

// 日期时间相关导入
import java.util.Calendar;
import java.util.Date;
import android.app.DatePickerDialog;

/**
 * 主活动类 - 大乐透彩票分析应用的核心界面
 * 
 * 功能概述：
 * 1. 历史数据管理：存储、查看、编辑、删除开奖记录
 * 2. 号码生成：基于10种不同算法生成预测号码
 * 3. 号码确认：确认预测号码用于下一期投注
 * 4. 中奖分析：计算各组算法的中奖情况和得分
 * 5. 数据导入导出：CSV格式的数据交换
 * 6. 趋势分析：统计分析和趋势图表展示
 * 
 * @author dlt项目组
 * @version 2.0
 * @since 1.0
 */

public class MainActivity extends AppCompatActivity {
    
    // =============================================================================
    // 常量定义区域：用于数据存储和业务逻辑控制
    // =============================================================================
    
    // SharedPreferences 相关常量
    private static final String PREFS_NAME = "dlt_history";                    // 本地存储文件名
    private static final String KEY_HISTORY = "history";                       // 历史数据存储键
    private static final String KEY_GENERATED_NUMBERS = "generated_numbers";   // 生成号码存储键
    private static final String KEY_GENERATION_RECORDS = "generation_records"; // 生成记录存储键
    private static final String KEY_CONFIRMED_NUMBERS = "confirmed_numbers";   // 确认号码存储键
    private static final String KEY_GROUP_SCORES = "group_scores";             // 各组得分存储键
    private static final String KEY_GROUP_MAX_PRIZE = "group_max_prize";       // 各组最大中奖等级存储键
    private static final String KEY_GROUP_MAX_PRIZE_COUNT = "group_max_prize_count"; // 各组最高奖中奖次数存储键
    private static final String KEY_BLOCKED_RULES = "blocked_rules";           // 屏蔽规则存储键
    
    // 业务逻辑相关常量
    private static final int MAX_HISTORY = 10;                    // 最大历史记录数（用于界面显示）
    private static final int MAX_GENERATION_RECORDS = 20;         // 最大生成记录数

    // =============================================================================
    // UI 组件声明区域：所有界面元素的引用
    // =============================================================================
    
    // 按钮组件
    private Button btnSave;           // "存入数据"按钮
    private Button btnGenerate;       // "生成号码"按钮
    private Button btnViewAll;        // "查看全部"按钮
    private Button btnTrendChart;     // "趋势图"按钮
    private Button btnViewRecords;    // "查看记录"按钮
    private Button btnConfirmCancel;  // "确认号码/取消确认"按钮
    private Button btnViewScores;     // "查看得分"按钮
    
    // 文本显示组件
    private TextView tvHistory;       // 历史数据显示区域
    private TextView tvResult;        // 生成结果显示区域
    
    // =============================================================================
    // 数据存储区域：应用的所有数据和状态管理
    // =============================================================================
    
    // 开奖数据相关
    private List<LotteryEntry> historyList = new ArrayList<>();     // 历史开奖数据列表
    
    // 号码生成相关
    private String lastGeneratedNumbers = "";                       // 上次生成的号码缓存
    private List<String> generationRecords = new ArrayList<>();     // 历史生成记录列表
    
    // 号码确认相关
    private String confirmedNumbers = "";                           // 当前确认的预测号码
    private boolean isNumbersConfirmed = false;                    // 号码确认状态标记
    
    // 得分统计相关
    private Map<Integer, Integer> groupScores = new HashMap<>();   // 各组算法的得分记录（组号->得分）
    private Map<Integer, Integer> groupMaxPrize = new HashMap<>(); // 记录每组的最大中奖等级
    private Map<Integer, Integer> groupMaxPrizeCount = new HashMap<>(); // 记录每组最高奖的中奖次数
    private Set<Integer> blockedRules = new HashSet<>();          // 记录被屏蔽的规则编号
    
    // 文件操作相关
    private ActivityResultLauncher<String> importLauncher;         // CSV文件导入启动器
    
    // =============================================================================
    // 算法规则说明区域：详细描述每种算法的实现逻辑
    // =============================================================================
    
    /**
     * 十一组算法的详细规则说明
     * 用于在界面中向用户展示每种算法的具体实现逻辑
     */
    private static final String[] GROUP_RULES = {
            "第1组：前区从排除近10期前区所有用过的号码后的剩余号码中随机选5个,如果排除近10期前区所有用过的号码后，剩余号码不足5个，则会从近10期出现次数最少的前区号码中随机补足；后区从排除近10期后区所有用过的号码后的剩余号码中随机选2个,如果排除近10期后区所有用过的号码后，剩余号码不足2个，则会从近10期出现次数最少的后区号码中随机补足",
            "第2组：前区选取近10期前区出现次数最多的5个号码（如有次数一样多则随机选）；后区选取近10期后区出现次数最多的2个号码（如有次数一样多则随机选）",
            "第3组：前区选取近10期前区出现次数最少的5个号码（只在出现过的号码中选，如有次数一样少则随机选）；后区选取近10期后区出现次数最少的2个号码（只在出现过的号码中选，如有次数一样少则随机选）",
            "第4组：前区包含近10期前区出现次数最多的2个号码（如有次数一样多则随机选），另3个从排除近10期前区用过的号码后的剩余号码中随机选；后区包含近10期后区出现次数最多的1个号码（如有次数一样多则随机选），另1个从排除近10期后区用过的号码后的剩余号码中随机选,如果排除近10期后区所有用过的号码后，剩余号码不足1个，则会从近10期出现次数最少的后区号码中随机补足",
            "第5组：前区从排除前四组前区所有用过的号码后的剩余号码中随机选5个；后区从排除前四组后区所有用过的号码后的剩余号码中随机选2个",
            "第6组：前区选取前四组前区出现次数最多的5个号码（如有次数一样多则随机选）；后区选取前四组后区出现次数最多的2个号码（如有次数一样多则随机选）",
            "第7组：基于所有历史数据，使用马尔科夫链模型分析从一期到下一期的号码转移概率，生成预测号码",
            "第8组：基于所有历史数据，使用朴素贝叶斯模型分析各位置号码分布特征，结合拉普拉斯平滑生成预测号码",
            "第9组：基于最近10期开奖数据，模拟神经网络前向传播过程，通过隐藏层非线性变换，计算各号码出现概率并加权随机生成",
            "第10组：分析号码出现的时间周期性规律，结合季节性趋势、长期趋势和冷号权重，基于时间序列模型预测最可能的号码组合",
            "第11组：完全随机算法，不依赖任何历史数据，纯随机从前区1-35中选5个号码，从后区1-12中选2个号码",
            "第12组：基于上一期连号检测的智能预测算法。前区规则：检查上一期是否有连号，若有则从未出现和最少出现的号码中选择；若无则从未出现的号码中选1个，从最少出现的号码中选2个，从第二少出现的号码中选2个连续号码。后区规则：从未出现和最少出现的号码中各选1个，且不能是上一期出现的号码。"
    };

    /**
     * Activity 生命周期方法 - 初始化入口
     * 
     * 负责完成以下初始化任务：
     * 1. 文件导入启动器的初始化
     * 2. UI 组件的绑定和初始化
     * 3. 数据的加载和恢复
     * 4. 事件监听器的设置
     * 5. 界面状态的初始化
     * 
     * @param savedInstanceState 保存的实例状态（用于恢复）
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // =============================================================================
        // 第一阶段：关键组件的优先初始化
        // =============================================================================
        
        // 注意：必须最先初始化 importLauncher，防止为 null 导致的崩溃
        importLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                importHistoryFromCSV(uri);  // 导入 CSV 文件数据
            }
        });
        
        // =============================================================================
        // 第二阶段：界面布局和组件初始化
        // =============================================================================
        
        EdgeToEdge.enable(this);                      // 启用全屏显示模式
        setContentView(R.layout.activity_main);         // 设置布局文件
        
        // 绑定 UI 组件引用
        btnSave = findViewById(R.id.btn_save);
        btnGenerate = findViewById(R.id.btn_generate);
        btnViewAll = findViewById(R.id.btn_view_all);
        btnTrendChart = findViewById(R.id.btn_trend_chart);
        btnViewRecords = findViewById(R.id.btn_view_records);
        btnConfirmCancel = findViewById(R.id.btn_confirm_cancel);
        btnViewScores = findViewById(R.id.btn_view_scores);
        tvHistory = findViewById(R.id.tv_history);
        tvResult = findViewById(R.id.tv_result);
        
        // =============================================================================
        // 第三阶段：数据加载和状态恢复
        // =============================================================================
        
        loadHistory();              // 从本地存储加载历史开奖数据
        loadGeneratedNumbers();     // 加载上次生成的号码数据
        loadGenerationRecords();    // 加载历史生成记录
        loadConfirmedNumbers();     // 加载已确认的预测号码
        loadGroupScores();          // 加载各组算法的得分数据
        loadBlockedRules();         // 加载被屏蔽的规则
        
        updateHistoryView();        // 更新历史数据显示区域
        updateButtonStates();       // 更新按钮状态和可用性
        
        // =============================================================================
        // 第四阶段：事件监听器设置
        // =============================================================================

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });
        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateNumbers();
            }
        });
        btnViewAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAllHistoryDialog();
            }
        });
        btnTrendChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TrendChartActivity.class);
                startActivity(intent);
            }
        });
        
        btnViewRecords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGenerationRecordsDialog();
            }
        });
        
        btnConfirmCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNumbersConfirmed) {
                    // 取消确认
                    cancelConfirmedNumbers();
                } else {
                    // 直接显示生成记录选择对话框
                    if (generationRecords.size() > 0) {
                        showGenerationRecordsDialog();
                    } else {
                        Toast.makeText(MainActivity.this, "请先生成号码", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        
        btnViewScores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGroupScoresDialog();
            }
        });
        // 结果区点击弹窗显示规则
        tvResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGroupRuleDialog();
            }
        });

        // 历史区点击弹窗显示号码统计
        tvHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistoryStatsDialog();
            }
        });
    }

    private void updateHistoryView() {
        StringBuilder sb = new StringBuilder();
        sb.append("近10期数据：\n");
        int total = historyList.size();
        int start = Math.max(0, total - 10);
        int idx = 1;
        for (int i = total - 1; i >= start; i--) {
            LotteryEntry entry = historyList.get(i);
            String issueInfo = "";
            if (!TextUtils.isEmpty(entry.getIssueNumber()) && !TextUtils.isEmpty(entry.getDrawDate())) {
                issueInfo = String.format("[%s %s] ", entry.getIssueNumber(), entry.getDrawDate());
            }
            String blockedTag = entry.isBlocked() ? "[屏蔽] " : "";
            sb.append(String.format("%2d: %s%s前区%s 后区%s\n", 
                idx++, blockedTag, issueInfo, entry.getFrontNumbersString(), entry.getBackNumbersString()));
        }
        tvHistory.setText(sb.toString());
    }

    private void saveHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (LotteryEntry entry : historyList) {
            sb.append(entry.getIssueNumber()).append("|")
              .append(entry.getDrawDate()).append("|")
              .append(listToStr(entry.getFrontNumbers())).append("|")
              .append(listToStr(entry.getBackNumbers())).append("|")
              .append(entry.isBlocked() ? "1" : "0").append(";");  // 新增屏蔽状态
        }
        prefs.edit().putString(KEY_HISTORY, sb.toString()).apply();
    }

    private void loadHistory() {
        historyList.clear();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String all = prefs.getString(KEY_HISTORY, "");
        if (TextUtils.isEmpty(all)) return;
        String[] groups = all.split(";");
        for (String group : groups) {
            if (TextUtils.isEmpty(group)) continue;
            String[] parts = group.split("\\|");
            if (parts.length == 2) {
                // 兼容旧格式：只有前区和后区号码
                List<Integer> front = strToList(parts[0]);
                List<Integer> back = strToList(parts[1]);
                if (front.size() == 5 && back.size() == 2) {
                    historyList.add(new LotteryEntry(front, back));
                }
            } else if (parts.length == 4) {
                // 旧新格式：期号|开奖日期|前区号码|后区号码
                String issueNumber = parts[0];
                String drawDate = parts[1];
                List<Integer> front = strToList(parts[2]);
                List<Integer> back = strToList(parts[3]);
                if (front.size() == 5 && back.size() == 2) {
                    historyList.add(new LotteryEntry(issueNumber, drawDate, front, back, false));  // 默认不屏蔽
                }
            } else if (parts.length == 5) {
                // 最新格式：期号|开奖日期|前区号码|后区号码|屏蔽状态
                String issueNumber = parts[0];
                String drawDate = parts[1];
                List<Integer> front = strToList(parts[2]);
                List<Integer> back = strToList(parts[3]);
                boolean isBlocked = "1".equals(parts[4]);
                if (front.size() == 5 && back.size() == 2) {
                    historyList.add(new LotteryEntry(issueNumber, drawDate, front, back, isBlocked));
                }
            }
        }
    }

    private void loadGeneratedNumbers() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        lastGeneratedNumbers = prefs.getString(KEY_GENERATED_NUMBERS, "");
        // 如果有确认号码，优先显示确认号码；否则显示最后生成的号码
        if (isNumbersConfirmed && !TextUtils.isEmpty(confirmedNumbers)) {
            tvResult.setText("已确认号码（下一期预测）：\n" + confirmedNumbers);
        } else if (!TextUtils.isEmpty(lastGeneratedNumbers)) {
            tvResult.setText(lastGeneratedNumbers);
        }
    }

    private void saveGeneratedNumbers(String numbers) {
        lastGeneratedNumbers = numbers;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_GENERATED_NUMBERS, numbers).apply();
    }

    private String listToStr(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(",");
        }
        return sb.toString();
    }

    private List<Integer> strToList(String str) {
        List<Integer> list = new ArrayList<>();
        if (TextUtils.isEmpty(str)) return list;
        String[] arr = str.split(",");
        for (String s : arr) {
            try {
                list.add(Integer.parseInt(s));
            } catch (Exception ignored) {}
        }
        return list;
    }

    /**
     * 核心方法：生成预测号码
     * 
     * 功能流程：
     * 1. 验证当前状态：检查是否已确认号码，如果已确认则禁止重新生成
     * 2. 调用算法引擎：使用 NumberGenerator 生成 10 组预测号码
     * 3. 显示结果：将生成的号码显示在结果区域
     * 4. 数据持久化：保存生成的号码到本地存储
     * 5. 记录管理：将本次生成记录添加到历史列表
     * 6. 状态更新：更新相关按钮的可用性状态
     * 
     * 注意事项：
     * - 只有在未确认号码的情况下才能进行生成
     * - 生成后会自动保存到多个位置以确保数据安全
     */
    private void generateNumbers() {
        // 状态验证：如果已经确认了号码，则不允许重新生成
        if (isNumbersConfirmed) {
            Toast.makeText(this, "已确认号码，无法重新生成！请先取消确认。", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 调用核心算法引擎：使用 NumberGenerator 生成所有10组号码，传入屏蔽规则
        NumberGenerator.GenerationResult result = NumberGenerator.generateAllNumbers(historyList, blockedRules);
        String generatedText = result.getDisplayText();
        
        // 显示生成结果在界面上
        tvResult.setText(generatedText);
        
        // 数据持久化：保存到本地存储以便下次启动时恢复
        saveGeneratedNumbers(generatedText);
        
        // 记录管理：添加到历史生成记录中供用户查看和选择
        saveToGenerationRecords(generatedText);
        
        // 状态更新：根据新的生成状态更新按钮可用性
        updateButtonStates();
    }

    // 处理本地化日期和解析方法
    
    // 自动分隔和输入限制
    private void setupAutoSpace(EditText editText, int maxCount, int numLen) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isEditing = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;
                String raw = s.toString().replaceAll("[^\\d]", "");
                StringBuilder sb = new StringBuilder();
                int idx = 0;
                for (int i = 0; i < maxCount && idx < raw.length(); i++) {
                    int remain = raw.length() - idx;
                    int take = Math.min(numLen, remain);
                    sb.append(raw, idx, idx + take);
                    idx += take;
                    if (i < maxCount - 1 && idx < raw.length()) sb.append(' ');
                }
                String result = sb.toString();
                if (!result.equals(s.toString())) {
                    s.replace(0, s.length(), result);
                }
                isEditing = false;
            }
        });
    }

    // 弹窗显示规则
    private void showGroupRuleDialog() {
        String[] items = new String[GROUP_RULES.length];
        for (int i = 0; i < GROUP_RULES.length; i++) {
            String blockedTag = blockedRules.contains(i + 1) ? "[屏蔽] " : "";
            items[i] = blockedTag + "第" + (i + 1) + "组规则：\n" + GROUP_RULES[i];
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("组号生成规则说明\n点击规则可设置屏蔽状态")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showRuleActionDialog(which + 1);
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
    }
        
    // 显示规则操作对话框
    private void showRuleActionDialog(int ruleNumber) {
        boolean isBlocked = blockedRules.contains(ruleNumber);
        String actionText = isBlocked ? "取消屏蔽" : "屏蔽该规则";
        String statusText = isBlocked ? "当前状态：已屏蔽" : "当前状态：正常";
            
        String message = String.format("第%d组规则\n%s\n\n%s\n\n屏蔽后该规则不会生成号码，已确认的号码中对应组也不会参与中奖计算。", 
            ruleNumber, GROUP_RULES[ruleNumber - 1], statusText);
            
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("规则管理")
                .setMessage(message)
                .setPositiveButton(actionText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isBlocked) {
                            blockedRules.remove(ruleNumber);
                            Toast.makeText(MainActivity.this, String.format("第%d组规则已取消屏蔽", ruleNumber), Toast.LENGTH_SHORT).show();
                        } else {
                            blockedRules.add(ruleNumber);
                            Toast.makeText(MainActivity.this, String.format("第%d组规则已屏蔽", ruleNumber), Toast.LENGTH_SHORT).show();
                        }
                        saveBlockedRules();
                        // 如果有确认号码，提示用户重新确认
                        if (isNumbersConfirmed) {
                            new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert)
                                .setTitle("提示")
                                .setMessage("规则屏蔽状态已更改，建议重新生成并确认号码以保证中奖计算的准确性。")
                                .setPositiveButton("知道了", null)
                                .show();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 弹窗显示近10期号码统计
    private void showHistoryStatsDialog() {
        // 只统计近10期数据
        Map<Integer, Integer> frontCount = new HashMap<>();
        Map<Integer, Integer> backCount = new HashMap<>();
        int total = historyList.size();
        int start = Math.max(0, total - 10);
        for (int i = start; i < total; i++) {
            LotteryEntry entry = historyList.get(i);
            for (int n : entry.getFrontNumbers()) {
                frontCount.put(n, frontCount.getOrDefault(n, 0) + 1);
            }
            for (int n : entry.getBackNumbers()) {
                backCount.put(n, backCount.getOrDefault(n, 0) + 1);
            }
        }
        List<Map.Entry<Integer, Integer>> frontList = new ArrayList<>(frontCount.entrySet());
        List<Map.Entry<Integer, Integer>> backList = new ArrayList<>(backCount.entrySet());
        frontList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        backList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        StringBuilder sb = new StringBuilder();
        sb.append("前区出现号码及次数：\n");
        for (Map.Entry<Integer, Integer> e : frontList) {
            sb.append(String.format("%2d（%d次）  ", e.getKey(), e.getValue()));
        }
        sb.append("\n\n后区出现号码及次数：\n");
        for (Map.Entry<Integer, Integer> e : backList) {
            sb.append(String.format("%2d（%d次）  ", e.getKey(), e.getValue()));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("近10期号码统计")
                .setMessage(sb.toString())
                .setNegativeButton("关闭", null)
                .show();
    }

    // 弹窗显示所有历史数据，并支持修改/删除
    private void showAllHistoryDialog() {
        if (historyList.isEmpty()) {
            Toast.makeText(this, "暂无历史数据", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = new String[historyList.size()];
        int total = historyList.size();
        for (int i = 0; i < total; i++) {
            LotteryEntry entry = historyList.get(total - 1 - i);
            String issueInfo = "";
            if (!TextUtils.isEmpty(entry.getIssueNumber()) && !TextUtils.isEmpty(entry.getDrawDate())) {
                issueInfo = String.format("[%s %s] ", entry.getIssueNumber(), entry.getDrawDate());
            }
            String blockedTag = entry.isBlocked() ? "[屏蔽] " : "";
            items[i] = String.format("第%02d期 %s%s前区%s 后区%s", 
                total - i, blockedTag, issueInfo, entry.getFrontNumbersString(), entry.getBackNumbersString());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("所有历史开奖结果")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showEditOrDeleteDialog(total - 1 - which);
                    }
                })
                .setNegativeButton("关闭", null)
                .setNeutralButton("导出", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exportHistoryToCSV();
                    }
                })
                .setPositiveButton("导入", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        importLauncher.launch("text/*");
                    }
                })
                .show();
    }

    // 弹窗：修改或删除某条历史数据
    private void showEditOrDeleteDialog(int index) {
        LotteryEntry entry = historyList.get(index);
        String issueInfo = "";
        if (!TextUtils.isEmpty(entry.getIssueNumber()) && !TextUtils.isEmpty(entry.getDrawDate())) {
            issueInfo = String.format("期号：%s\n开奖日期：%s\n", entry.getIssueNumber(), entry.getDrawDate());
        }
        String blockedStatus = entry.isBlocked() ? "当前状态：已屏蔽\n" : "当前状态：正常\n";
        String msg = String.format("%s%s前区：%s\n后区：%s", issueInfo, blockedStatus, entry.getFrontNumbersString(), entry.getBackNumbersString());
        
        String blockButtonText = entry.isBlocked() ? "取消屏蔽" : "屏蔽该期";
        
        // 创建自定义对话框来支持更多按钮
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("修改或删除该期数据")
                .setMessage(msg)
                .setPositiveButton("修改", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showModifyEntryDialog(index);
                    }
                })
                .setNegativeButton("取消", null)
                .setNeutralButton("更多操作", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showMoreActionsDialog(index);
                    }
                })
                .show();
    }
    
    // 显示更多操作对话框（删除和屏蔽操作）
    private void showMoreActionsDialog(int index) {
        LotteryEntry entry = historyList.get(index);
        String blockButtonText = entry.isBlocked() ? "取消屏蔽" : "屏蔽该期";
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("操作选择")
                .setMessage("请选择要执行的操作：")
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 再次确认删除
                        new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert)
                            .setTitle("确认删除")
                            .setMessage("确定要删除这条记录吗？删除后无法恢复。")
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    historyList.remove(index);
                                    saveHistory();
                                    updateHistoryView();
                                    Toast.makeText(MainActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                    }
                })
                .setNeutralButton(blockButtonText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        entry.setBlocked(!entry.isBlocked());
                        saveHistory();
                        updateHistoryView();
                        String message = entry.isBlocked() ? "屏蔽成功，该期号码不再参与统计和生成" : "取消屏蔽成功";
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 弹窗：编辑某条历史数据
    private void showModifyEntryDialog(int index) {
        LotteryEntry entry = historyList.get(index);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_entry, null);
        EditText etIssueNumber = view.findViewById(R.id.et_issue_number);
        EditText etDrawDate = view.findViewById(R.id.et_draw_date);
        EditText etFrontEdit = view.findViewById(R.id.et_front_edit);
        EditText etBackEdit = view.findViewById(R.id.et_back_edit);
        etIssueNumber.setText(entry.getIssueNumber());
        etDrawDate.setText(entry.getDrawDate());
        etFrontEdit.setText(listToStr(entry.getFrontNumbers()).replace(",", " "));
        etBackEdit.setText(listToStr(entry.getBackNumbers()).replace(",", " "));
        
        // 设置日期选择器
        etDrawDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(etDrawDate);
            }
        });
        etDrawDate.setFocusable(false); // 防止键盘弹出
        
        setupAutoSpace(etFrontEdit, 5, 2);
        setupAutoSpace(etBackEdit, 2, 2);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle(getString(R.string.edit_title))
                .setView(view)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String issueNumber = etIssueNumber.getText().toString().trim();
                        String drawDate = etDrawDate.getText().toString().trim();
                        List<Integer> front = parseNumbers(etFrontEdit.getText().toString().trim(), 5, 1, 35);
                        List<Integer> back = parseNumbers(etBackEdit.getText().toString().trim(), 2, 1, 12);
                        if (front == null || back == null) {
                            Toast.makeText(MainActivity.this, "号码格式错误！", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        historyList.set(index, new LotteryEntry(issueNumber, drawDate, front, back));
                        saveHistory();
                        updateHistoryView();
                        Toast.makeText(MainActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 存入弹窗输入
    private void showInputDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_entry, null);
        EditText etIssueNumber = view.findViewById(R.id.et_issue_number);
        EditText etDrawDate = view.findViewById(R.id.et_draw_date);
        EditText etFrontEdit = view.findViewById(R.id.et_front_edit);
        EditText etBackEdit = view.findViewById(R.id.et_back_edit);
        
        // 自动生成期号
        int nextSequence = getNextIssueSequence();
        String autoIssueNumber = LotteryEntry.generateIssueNumber(nextSequence);
        etIssueNumber.setText(autoIssueNumber);
        
        // 设置当前日期
        String currentDate = LotteryEntry.getCurrentDate();
        etDrawDate.setText(currentDate);
        
        // 设置日期选择器
        etDrawDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog(etDrawDate);
            }
        });
        etDrawDate.setFocusable(false); // 防止键盘弹出
        
        setupAutoSpace(etFrontEdit, 5, 2);
        setupAutoSpace(etBackEdit, 2, 2);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle(getString(R.string.input_title))
                .setView(view)
                .setPositiveButton("存入", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String issueNumber = etIssueNumber.getText().toString().trim();
                        String drawDate = etDrawDate.getText().toString().trim();
                        String frontStr = etFrontEdit.getText().toString().trim();
                        String backStr = etBackEdit.getText().toString().trim();
                        List<Integer> front = parseNumbers(frontStr, 5, 1, 35);
                        List<Integer> back = parseNumbers(backStr, 2, 1, 12);
                        if (front == null || back == null) {
                            Toast.makeText(MainActivity.this, "号码格式错误！", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        LotteryEntry entry = new LotteryEntry(issueNumber, drawDate, front, back);
                        historyList.add(entry);
                        saveHistory();
                        updateHistoryView();
                        
                        // 如果有确认号码，进行中奖对比
                        if (isNumbersConfirmed) {
                            calculateWinningScores(entry);
                        }
                        
                        Toast.makeText(MainActivity.this, "存入成功！", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 获取下一期序号
    private int getNextIssueSequence() {
        if (historyList.isEmpty()) {
            return 1;
        }
        
        // 获取当前年份
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int yearSuffix = currentYear % 100;
        
        // 查找当前年份的最大序号
        int maxSequence = 0;
        for (LotteryEntry entry : historyList) {
            String issueNumber = entry.getIssueNumber();
            if (!TextUtils.isEmpty(issueNumber) && issueNumber.length() == 5) {
                try {
                    int issueYear = Integer.parseInt(issueNumber.substring(0, 2));
                    if (issueYear == yearSuffix) {
                        int sequence = Integer.parseInt(issueNumber.substring(2));
                        if (sequence > maxSequence) {
                            maxSequence = sequence;
                        }
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        }
        
        return maxSequence + 1;
    }
    
    // 显示日期选择器
    private void showDatePickerDialog(final EditText dateEditText) {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(android.widget.DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                String selectedDate = String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                dateEditText.setText(selectedDate);
            }
        }, year, month, day);
        
        datePickerDialog.show();
    }

    // 恢复parseNumbers方法，供弹窗输入校验使用
    private List<Integer> parseNumbers(String str, int count, int min, int max) {
        if (TextUtils.isEmpty(str)) return null;
        String[] arr = str.split("\\s+");
        if (arr.length != count) return null;
        Set<Integer> set = new HashSet<>();
        for (String s : arr) {
            try {
                int n = Integer.parseInt(s);
                if (n < min || n > max) return null;
                set.add(n);
            } catch (Exception e) {
                return null;
            }
        }
        if (set.size() != count) return null;
        List<Integer> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }

    // 导出历史数据为CSV
    private void exportHistoryToCSV() {
        if (historyList.isEmpty()) {
            Toast.makeText(this, "暂无数据可导出", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("期号,开奖日期,前区1,前区2,前区3,前区4,前区5,后区1,后区2\n");
        for (LotteryEntry entry : historyList) {
            List<Integer> f = entry.getFrontNumbers();
            List<Integer> b = entry.getBackNumbers();
            sb.append(entry.getIssueNumber()).append(",")
              .append(entry.getDrawDate()).append(",");
            for (int i = 0; i < 5; i++) sb.append(f.get(i)).append(i < 4 ? "," : ",");
            sb.append(b.get(0)).append(",").append(b.get(1)).append("\n");
        }
        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir != null && !dir.exists()) dir.mkdirs();
            File file = new File(dir, "dlt_history.csv");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            writer.write(sb.toString());
            writer.close();
            Toast.makeText(this, "导出成功: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    // 导入历史数据（CSV）
    private void importHistoryFromCSV(Uri uri) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri), "UTF-8"));
            String line = reader.readLine(); // 跳过表头
            List<LotteryEntry> importList = new ArrayList<>();
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;
                String[] arr = line.split(",");
                if (arr.length != 9) {
                    Toast.makeText(this, "第" + lineNum + "行格式错误（列数应为9）", Toast.LENGTH_LONG).show();
                    continue;
                }
                String issueNumber = arr[0].trim();
                String drawDate = arr[1].trim();
                List<Integer> front = new ArrayList<>();
                List<Integer> back = new ArrayList<>();
                boolean valid = true;
                try {
                    for (int i = 2; i < 7; i++) {
                        int n = Integer.parseInt(arr[i].trim());
                        if (n < 1 || n > 35) valid = false;
                        front.add(n);
                    }
                    for (int i = 7; i < 9; i++) {
                        int n = Integer.parseInt(arr[i].trim());
                        if (n < 1 || n > 12) valid = false;
                        back.add(n);
                    }
                } catch (Exception e) {
                    valid = false;
                }
                if (valid && front.size() == 5 && back.size() == 2) {
                    importList.add(new LotteryEntry(issueNumber, drawDate, front, back));
                } else {
                    Toast.makeText(this, "第" + lineNum + "行数据有误，已跳过", Toast.LENGTH_SHORT).show();
                }
            }
            reader.close();
            if (!importList.isEmpty()) {
                historyList.clear();
                historyList.addAll(importList);
                saveHistory();
                updateHistoryView();
                Toast.makeText(this, "导入成功，共" + importList.size() + "条", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "导入内容为空或全部格式错误", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "导入失败: " + (e.getMessage() == null ? "未知错误" : e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
    
    // 加载生成记录
    private void loadGenerationRecords() {
        generationRecords.clear();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String records = prefs.getString(KEY_GENERATION_RECORDS, "");
        if (!TextUtils.isEmpty(records)) {
            String[] recordArray = records.split("\\|\\|\\|");
            for (String record : recordArray) {
                if (!TextUtils.isEmpty(record.trim())) {
                    generationRecords.add(record.trim());
                }
            }
        }
    }
    
    // 保存生成记录
    private void saveGenerationRecords() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < generationRecords.size(); i++) {
            sb.append(generationRecords.get(i));
            if (i < generationRecords.size() - 1) {
                sb.append("|||");
            }
        }
        prefs.edit().putString(KEY_GENERATION_RECORDS, sb.toString()).apply();
    }
    
    // 添加到生成记录
    private void saveToGenerationRecords(String generatedNumbers) {
        // 添加时间戳
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
        String recordEntry = timestamp + "\n" + generatedNumbers;
        
        generationRecords.add(recordEntry); // 添加到列表末尾，保持时间顺序
        
        // 保持最多20条记录
        while (generationRecords.size() > MAX_GENERATION_RECORDS) {
            generationRecords.remove(0); // 删除最旧的记录
        }
        
        saveGenerationRecords();
        
        // 生成记录更新后，更新按钮状态
        updateButtonStates();
    }
    
    // 加载确认号码
    private void loadConfirmedNumbers() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        confirmedNumbers = prefs.getString(KEY_CONFIRMED_NUMBERS, "");
        isNumbersConfirmed = !TextUtils.isEmpty(confirmedNumbers);
    }
    
    // 保存确认号码
    private void saveConfirmedNumbers(String numbers) {
        confirmedNumbers = numbers;
        isNumbersConfirmed = !TextUtils.isEmpty(numbers);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CONFIRMED_NUMBERS, numbers).apply();
        updateButtonStates();
        
        if (isNumbersConfirmed) {
            tvResult.setText("已确认号码（下一期预测）：\n" + numbers);
        }
    }
    
    // 取消确认号码
    private void cancelConfirmedNumbers() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("取消确认")
                .setMessage("确定要取消已确认的号码吗？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveConfirmedNumbers("");
                        // 清除显示，显示取消确认的提示
                        String cancelMessage = "生成的号码已取消确认，可以重新生成号码。";
                        tvResult.setText(cancelMessage);
                        saveGeneratedNumbers(cancelMessage); // 保存取消状态
                        Toast.makeText(MainActivity.this, "已取消确认", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    // 更新按钮状态
    private void updateButtonStates() {
        btnGenerate.setEnabled(!isNumbersConfirmed);
        btnViewRecords.setEnabled(generationRecords.size() > 0);
        btnViewScores.setEnabled(isNumbersConfirmed); // 只有确认号码后才能查看分数
        
        if (isNumbersConfirmed) {
            btnConfirmCancel.setText("取消确认");
            btnConfirmCancel.setEnabled(true);
        } else {
            btnConfirmCancel.setText("确认号码");
            // 只有在有生成记录时才能确认号码
            btnConfirmCancel.setEnabled(generationRecords.size() > 0);
        }
    }
    
    // 显示生成记录对话框
    private void showGenerationRecordsDialog() {
        if (generationRecords.isEmpty()) {
            Toast.makeText(this, "暂无生成记录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] items = new String[generationRecords.size()];
        for (int i = 0; i < generationRecords.size(); i++) {
            int displayIndex = generationRecords.size() - i; // 最新的记录显示为最大的序号
            String record = generationRecords.get(generationRecords.size() - 1 - i); // 从后往前取记录
            String[] parts = record.split("\n", 2);
            if (parts.length >= 2) {
                String timestamp = parts[0];
                String summary = parts[1].split("\n")[0]; // 只显示第一组作为预览
                items[i] = String.format("第%d次 [%s]\n%s...", displayIndex, timestamp, summary);
            } else {
                items[i] = String.format("第%d次\n%s", displayIndex, record);
            }
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("生成记录（前" + generationRecords.size() + "次）")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 将显示索引转换为实际存储索引（最新在前）
                        int actualIndex = generationRecords.size() - 1 - which;
                        showRecordDetailDialog(actualIndex, generationRecords.size() - which);
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
    }
    
    // 显示记录详情对话框
    private void showRecordDetailDialog(int recordIndex, int displayNumber) {
        String record = generationRecords.get(recordIndex);
        String[] parts = record.split("\n", 2);
        String timestamp = parts.length >= 2 ? parts[0] : "未知时间";
        String numbers = parts.length >= 2 ? parts[1] : record;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("第" + displayNumber + "次生成记录")
                .setMessage("生成时间：" + timestamp + "\n\n" + numbers)
                .setPositiveButton("确认此组号码", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        confirmSelectedNumbers(numbers);
                    }
                })
                .setNegativeButton("返回", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        showGenerationRecordsDialog(); // 返回列表
                    }
                })
                .show();
    }
    
    // 确认选中的号码
    private void confirmSelectedNumbers(String numbers) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("确认号码")
                .setMessage("确定要将此组号码设为下一期的预测号码吗？\n\n确认后将无法重新生成，除非取消确认。")
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveConfirmedNumbers(numbers);
                        // 将确认的号码也保存为最后生成的号码，确保重新进入应用时显示正确
                        saveGeneratedNumbers("已确认号码（下一期预测）：\n" + numbers);
                        Toast.makeText(MainActivity.this, "号码已确认！", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    // 加载各组分数
    private void loadGroupScores() {
        groupScores.clear();
        groupMaxPrize.clear();
        groupMaxPrizeCount.clear();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
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
        
        // 初始化所有组的分数为0（如果没有记录）
        for (int i = 1; i <= 12; i++) {
            if (!groupScores.containsKey(i)) {
                groupScores.put(i, 0);
            }
        }
    }
    
    // 保存各组分数
    private void saveGroupScores() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // 保存分数
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
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
        
        prefs.edit()
            .putString(KEY_GROUP_SCORES, sb.toString())
            .putString(KEY_GROUP_MAX_PRIZE, sbPrize.toString())
            .putString(KEY_GROUP_MAX_PRIZE_COUNT, sbCount.toString())
            .apply();
    }
    
    // 加载被屏蔽的规则
    private void loadBlockedRules() {
        blockedRules.clear();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String blocked = prefs.getString(KEY_BLOCKED_RULES, "");
        if (!TextUtils.isEmpty(blocked)) {
            String[] ruleArray = blocked.split(",");
            for (String ruleStr : ruleArray) {
                try {
                    int ruleNum = Integer.parseInt(ruleStr.trim());
                    if (ruleNum >= 1 && ruleNum <= 12) {
                        blockedRules.add(ruleNum);
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        }
    }
    
    // 保存被屏蔽的规则
    private void saveBlockedRules() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int ruleNum : blockedRules) {
            if (!first) sb.append(",");
            sb.append(ruleNum);
            first = false;
        }
        prefs.edit().putString(KEY_BLOCKED_RULES, sb.toString()).apply();
    }
    
    // 显示各组分数对话框
    private void showGroupScoresDialog() {
        if (!isNumbersConfirmed) {
            Toast.makeText(this, "暂无确认号码，无法查看分数", Toast.LENGTH_SHORT).show();
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("各组分数情况：\n\n");
        for (int i = 1; i <= 12; i++) {
            int score = groupScores.getOrDefault(i, 0);
            int maxPrize = groupMaxPrize.getOrDefault(i, 0);
            int prizeCount = groupMaxPrizeCount.getOrDefault(i, 0);
            String prizeText = maxPrize > 0 ? getPrizeText(maxPrize) : "无";
            String countText = prizeCount > 0 ? "（" + prizeCount + "次）" : "";
            sb.append(String.format("第%d组：%d分 | 最高：%s%s\n", i, score, prizeText, countText));
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("各组分数")
                .setMessage(sb.toString())
                .setPositiveButton("清空分数", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearAllScores();
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
    }
    
    // 清空所有分数
    private void clearAllScores() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("清空分数")
                .setMessage("确定要清空所有组的分数吗？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (int i = 1; i <= 12; i++) {
                            groupScores.put(i, 0);
                            groupMaxPrize.remove(i);  // 同时清空最大中奖等级记录
                            groupMaxPrizeCount.remove(i);  // 清空中奖次数记录
                        }
                        blockedRules.clear();  // 清空屏蔽规则
                        saveBlockedRules();    // 保存屏蔽规则状态
                        saveGroupScores();
                        Toast.makeText(MainActivity.this, "已清空所有分数", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    // 计算中奖分数
    private void calculateWinningScores(LotteryEntry newEntry) {
        if (!isNumbersConfirmed || TextUtils.isEmpty(confirmedNumbers)) {
            return;
        }
        
        // 解析确认的号码（10组）
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
                    // 忽略解析错误，记录调试信息
                    android.util.Log.d("DLT_DEBUG", "解析行失败: " + line + ", 错误: " + e.getMessage());
                }
            }
        }
        
        // 添加调试信息
        android.util.Log.d("DLT_DEBUG", "解析结果 - 前区组数: " + confirmedFrontGroups.size() + ", 后区组数: " + confirmedBackGroups.size());
        
        if (confirmedFrontGroups.size() != 12 || confirmedBackGroups.size() != 12) {
            String debugInfo = String.format("确认号码格式错误，无法进行计算。解析到前区%d组，后区%d组（期望各12组）", 
                confirmedFrontGroups.size(), confirmedBackGroups.size());
            Toast.makeText(this, debugInfo, Toast.LENGTH_LONG).show();
            android.util.Log.e("DLT_ERROR", debugInfo);
            android.util.Log.e("DLT_ERROR", "确认号码内容：\n" + confirmedNumbers);
            return;
        }
        
        // 获取开奖号码
        List<Integer> winningFront = newEntry.getFrontNumbers();
        List<Integer> winningBack = newEntry.getBackNumbers();
        
        int totalScoreChange = 0;
        StringBuilder resultMessage = new StringBuilder();
        resultMessage.append("中奖结果：\n\n");
        
        // 对比每一组（跳过被屏蔽的规则）
        for (int groupIndex = 0; groupIndex < 12; groupIndex++) {
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
        
        // 显示结果
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle("中奖结果")
                .setMessage(resultMessage.toString())
                .setPositiveButton("查看分数", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showGroupScoresDialog();
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
    }
    
    // 解析号码字符串
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
                android.util.Log.d("DLT_DEBUG", "无法解析数字: " + part);
            }
        }
        return numbers;
    }
    
    // 计算匹配数量
    private int countMatches(List<Integer> group, List<Integer> winning) {
        int matches = 0;
        for (int number : group) {
            if (winning.contains(number)) {
                matches++;
            }
        }
        return matches;
    }
    
    // 判断奖级
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
    
    // 获取分数变化
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
    
    // 获取奖级文本
    private String getPrizeText(int prizeLevel) {
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
}