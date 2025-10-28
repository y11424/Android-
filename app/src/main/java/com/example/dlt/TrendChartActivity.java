package com.example.dlt;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class TrendChartActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "dlt_history";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_GENERATED_NUMBERS = "generated_numbers";

    private Spinner spinnerChartType;
    private EditText etPeriods;
    private Button btnApplyPeriods;
    private LinearLayout containerChart;
    private Button btnBack, btnExportChart, btnSwitchAxis;
    private List<LotteryEntry> historyList = new ArrayList<>();
    private String lastGeneratedNumbers = "";
    private boolean isAxisSwitched = false; // 坐标转换状态
    private int currentPeriods = 10; // 当前期数，默认10期

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trend_chart);

        initViews();
        loadData();
        setupSpinners();
        setupListeners();
        drawChart();
    }

    private void initViews() {
        spinnerChartType = findViewById(R.id.spinner_chart_type);
        etPeriods = findViewById(R.id.et_periods);
        btnApplyPeriods = findViewById(R.id.btn_apply_periods);
        containerChart = findViewById(R.id.container_chart);
        btnBack = findViewById(R.id.btn_back);
        btnExportChart = findViewById(R.id.btn_export_chart);
        btnSwitchAxis = findViewById(R.id.btn_switch_axis);
        
        // 设置默认期数
        etPeriods.setText(String.valueOf(currentPeriods));
    }

    private void loadData() {
        // 加载历史数据
        loadHistory();
        // 加载生成的号码
        loadGeneratedNumbers();
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
    }

    private List<Integer> strToList(String str) {
        List<Integer> list = new ArrayList<>();
        if (TextUtils.isEmpty(str)) return list;
        String[] arr = str.split(",");
        for (String s : arr) {
            try {
                String trimmed = s.trim();
                if (!TextUtils.isEmpty(trimmed)) {
                    list.add(Integer.parseInt(trimmed));
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    private void setupSpinners() {
        // 图表类型选择器
        String[] chartTypes = {"历史开奖走势图", "号码生成走势图"};
        ArrayAdapter<String> chartTypeAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, chartTypes);
        chartTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChartType.setAdapter(chartTypeAdapter);
        
        // 设置默认选择
        spinnerChartType.setSelection(0);
    }

    private void setupListeners() {
        spinnerChartType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                drawChart();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 期数确定按钮监听
        btnApplyPeriods.setOnClickListener(v -> {
            String periodsStr = etPeriods.getText().toString().trim();
            if (TextUtils.isEmpty(periodsStr)) {
                Toast.makeText(this, "请输入期数", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                int periods = Integer.parseInt(periodsStr);
                if (periods <= 0) {
                    Toast.makeText(this, "期数必须大于0", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (periods > 9999) {
                    Toast.makeText(this, "期数不能超过9999", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentPeriods = periods;
                drawChart();
                Toast.makeText(this, "已设置为最近" + periods + "期", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
            }
        });

        btnBack.setOnClickListener(v -> finish());

        btnExportChart.setOnClickListener(v -> {
            Toast.makeText(this, "导出功能开发中...", Toast.LENGTH_SHORT).show();
        });

        btnSwitchAxis.setOnClickListener(v -> {
            isAxisSwitched = !isAxisSwitched;
            drawChart();
        });
    }

    private void drawChart() {
        containerChart.removeAllViews();
        
        // 添加坐标状态指示器
        addAxisStatusIndicator();
        
        int chartType = spinnerChartType.getSelectedItemPosition();
        
        if (chartType == 0) {
            // 历史开奖走势图
            drawHistoryTrendChart(currentPeriods);
        } else {
            // 号码生成走势图
            drawGeneratedTrendChart();
        }
    }

    private void addAxisStatusIndicator() {
        android.widget.TextView statusView = new android.widget.TextView(this);
        String statusText = isAxisSwitched ? 
            "坐标状态：号码在X轴，期数在Y轴 (可左右滑动查看完整图表)" : 
            "坐标状态：期数在X轴，号码在Y轴";
        statusView.setText(statusText);
        statusView.setTextSize(14);
        statusView.setTextColor(Color.BLUE);
        statusView.setGravity(android.view.Gravity.CENTER);
        statusView.setPadding(0, 10, 0, 10);
        statusView.setBackgroundColor(Color.LTGRAY);
        containerChart.addView(statusView);
    }

    private void drawHistoryTrendChart(int periods) {
        if (historyList.isEmpty()) {
            addEmptyView("暂无历史数据");
            return;
        }

        int maxPeriods = periods;
        int startIndex = Math.max(0, historyList.size() - maxPeriods);
        List<LotteryEntry> displayList = historyList.subList(startIndex, historyList.size());
        
        // 过滤掉被屏蔽的期数，用于走势图显示
        List<LotteryEntry> filteredDisplayList = new ArrayList<>();
        for (LotteryEntry entry : displayList) {
            if (!entry.isBlocked()) {
                filteredDisplayList.add(entry);
            }
        }
        
        if (filteredDisplayList.isEmpty()) {
            addEmptyView("所选期数均被屏蔽，无法显示走势图");
            return;
        }

        // 添加统计信息（传入全部数据以显示屏蔽信息）
        addStatisticsInfo(displayList);

        // 前区走势图（使用过滤后的数据）
        addChartTitle("前区走势图");
        TrendChartView frontChart = new TrendChartView(this, filteredDisplayList, true, isAxisSwitched);
        addChartWithScroll(frontChart);

        // 后区走势图（使用过滤后的数据）
        addChartTitle("后区走势图");
        TrendChartView backChart = new TrendChartView(this, filteredDisplayList, false, isAxisSwitched);
        addChartWithScroll(backChart);
    }

    private void addChartWithScroll(View chartView) {
        if (isAxisSwitched) {
            // 坐标转换时，使用水平滚动视图
            android.widget.HorizontalScrollView horizontalScrollView = new android.widget.HorizontalScrollView(this);
            horizontalScrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            horizontalScrollView.setHorizontalScrollBarEnabled(true);
            horizontalScrollView.setScrollBarStyle(android.view.View.SCROLLBARS_INSIDE_OVERLAY);
            
            // 设置图表的最小宽度
            LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            chartView.setLayoutParams(chartParams);
            
            horizontalScrollView.addView(chartView);
            containerChart.addView(horizontalScrollView);
        } else {
            // 原始坐标时，直接添加图表
            containerChart.addView(chartView);
        }
    }

    private void addStatisticsInfo(List<LotteryEntry> data) {
        if (data.isEmpty()) return;
        
        // 过滤掉被屏蔽的期数
        List<LotteryEntry> filteredData = new ArrayList<>();
        for (LotteryEntry entry : data) {
            if (!entry.isBlocked()) {
                filteredData.add(entry);
            }
        }
        
        if (filteredData.isEmpty()) {
            android.widget.TextView statsView = new android.widget.TextView(this);
            statsView.setText("统计信息：所有期数均被屏蔽，无法统计");
            statsView.setTextSize(14);
            statsView.setTextColor(Color.RED);
            statsView.setPadding(0, 10, 0, 10);
            statsView.setBackgroundColor(Color.LTGRAY);
            containerChart.addView(statsView);
            return;
        }
        
        // 统计前区号码出现次数
        int[] frontCount = new int[36];
        int[] backCount = new int[13];
        
        for (LotteryEntry entry : filteredData) {
            for (int num : entry.getFrontNumbers()) {
                frontCount[num]++;
            }
            for (int num : entry.getBackNumbers()) {
                backCount[num]++;
            }
        }
        
        // 找出出现次数最多的号码
        int maxFront = 0, maxBack = 0;
        for (int i = 1; i <= 35; i++) {
            if (frontCount[i] > maxFront) maxFront = frontCount[i];
        }
        for (int i = 1; i <= 12; i++) {
            if (backCount[i] > maxBack) maxBack = backCount[i];
        }
        
        // 找出出现次数最少的号码（排除未出现的）
        int minFront = Integer.MAX_VALUE, minBack = Integer.MAX_VALUE;
        for (int i = 1; i <= 35; i++) {
            if (frontCount[i] > 0 && frontCount[i] < minFront) {
                minFront = frontCount[i];
            }
        }
        for (int i = 1; i <= 12; i++) {
            if (backCount[i] > 0 && backCount[i] < minBack) {
                minBack = backCount[i];
            }
        }
        
        // 如果所有号码都未出现，则设置最小值为0
        if (minFront == Integer.MAX_VALUE) minFront = 0;
        if (minBack == Integer.MAX_VALUE) minBack = 0;
        
        // 收集未出现的号码
        List<Integer> frontNotAppeared = new ArrayList<>();
        List<Integer> backNotAppeared = new ArrayList<>();
        
        for (int i = 1; i <= 35; i++) {
            if (frontCount[i] == 0) {
                frontNotAppeared.add(i);
            }
        }
        
        for (int i = 1; i <= 12; i++) {
            if (backCount[i] == 0) {
                backNotAppeared.add(i);
            }
        }
        
        StringBuilder stats = new StringBuilder();
        int blockedCount = data.size() - filteredData.size();
        stats.append("统计信息（共").append(data.size()).append("期，其中").append(blockedCount).append("期被屏蔽，实际统计").append(filteredData.size()).append("期）：\n");
        
        // 显示期号范围
        if (!filteredData.isEmpty()) {
            LotteryEntry first = filteredData.get(0);
            LotteryEntry last = filteredData.get(filteredData.size() - 1);
            if (!TextUtils.isEmpty(first.getIssueNumber()) && !TextUtils.isEmpty(last.getIssueNumber())) {
                stats.append("期号范围：").append(first.getIssueNumber()).append(" - ").append(last.getIssueNumber()).append("\n");
            }
        }
        
        stats.append("前区出现最多：");
        for (int i = 1; i <= 35; i++) {
            if (frontCount[i] == maxFront) {
                stats.append(i).append("(").append(maxFront).append("次) ");
            }
        }
        stats.append("\n后区出现最多：");
        for (int i = 1; i <= 12; i++) {
            if (backCount[i] == maxBack) {
                stats.append(i).append("(").append(maxBack).append("次) ");
            }
        }
        
        // 添加出现次数最少的号码信息
        stats.append("\n前区出现最少：");
        if (minFront == 0) {
            stats.append("无（全部号码均未出现）");
        } else {
            boolean hasMinFront = false;
            for (int i = 1; i <= 35; i++) {
                if (frontCount[i] == minFront) {
                    if (hasMinFront) stats.append(", ");
                    stats.append(i).append("(").append(minFront).append("次)");
                    hasMinFront = true;
                }
            }
        }
        
        stats.append("\n后区出现最少：");
        if (minBack == 0) {
            stats.append("无（全部号码均未出现）");
        } else {
            boolean hasMinBack = false;
            for (int i = 1; i <= 12; i++) {
                if (backCount[i] == minBack) {
                    if (hasMinBack) stats.append(", ");
                    stats.append(i).append("(").append(minBack).append("次)");
                    hasMinBack = true;
                }
            }
        }
        
        // 添加未出现的号码信息
        stats.append("\n前区未出现：");
        if (frontNotAppeared.isEmpty()) {
            stats.append("无");
        } else {
            for (int i = 0; i < frontNotAppeared.size(); i++) {
                if (i > 0) stats.append(", ");
                stats.append(frontNotAppeared.get(i));
            }
            stats.append(" (共").append(frontNotAppeared.size()).append("个)");
        }
        
        stats.append("\n后区未出现：");
        if (backNotAppeared.isEmpty()) {
            stats.append("无");
        } else {
            for (int i = 0; i < backNotAppeared.size(); i++) {
                if (i > 0) stats.append(", ");
                stats.append(backNotAppeared.get(i));
            }
            stats.append(" (共").append(backNotAppeared.size()).append("个)");
        }
        
        android.widget.TextView statsView = new android.widget.TextView(this);
        statsView.setText(stats.toString());
        statsView.setTextSize(14);
        statsView.setTextColor(Color.BLUE);
        statsView.setPadding(0, 10, 0, 10);
        statsView.setBackgroundColor(Color.LTGRAY);
        containerChart.addView(statsView);
    }

    private void drawGeneratedTrendChart() {
        if (TextUtils.isEmpty(lastGeneratedNumbers)) {
            addEmptyView("暂无生成的号码数据");
            return;
        }

        // 解析生成的号码
        List<LotteryEntry> generatedList = parseGeneratedNumbers();
        if (generatedList.isEmpty()) {
            addEmptyView("生成的号码格式错误，请检查数据格式");
            return;
        }

        // 前区走势图
        addChartTitle("生成号码前区走势图");
        TrendChartView frontChart = new TrendChartView(this, generatedList, true, isAxisSwitched);
        addChartWithScroll(frontChart);

        // 后区走势图
        addChartTitle("生成号码后区走势图");
        TrendChartView backChart = new TrendChartView(this, generatedList, false, isAxisSwitched);
        addChartWithScroll(backChart);
    }

    private List<LotteryEntry> parseGeneratedNumbers() {
        List<LotteryEntry> result = new ArrayList<>();
        String[] lines = lastGeneratedNumbers.split("\n");
        
        for (String line : lines) {
            if (line.contains("前区") && line.contains("后区")) {
                try {
                    // 解析格式：第X组：前区[1,2,3,4,5] 后区[1,2]
                    int frontStart = line.indexOf("前区") + 2;
                    int frontEnd = line.indexOf("后区");
                    int backStart = line.indexOf("后区") + 2;
                    int backEnd = line.length();
                    
                    if (frontStart > 1 && frontEnd > frontStart && backStart > 1 && backEnd > backStart) {
                        String frontStr = line.substring(frontStart, frontEnd).trim();
                        String backStr = line.substring(backStart, backEnd).trim();
                        
                        // 移除方括号
                        if (frontStr.startsWith("[") && frontStr.endsWith("]")) {
                            frontStr = frontStr.substring(1, frontStr.length() - 1);
                        }
                        if (backStr.startsWith("[") && backStr.endsWith("]")) {
                            backStr = backStr.substring(1, backStr.length() - 1);
                        }
                        
                        List<Integer> front = strToList(frontStr);
                        List<Integer> back = strToList(backStr);
                        
                        if (front.size() == 5 && back.size() == 2) {
                            result.add(new LotteryEntry(front, back));
                        }
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        }
        
        return result;
    }

    private void addChartTitle(String title) {
        android.widget.TextView titleView = new android.widget.TextView(this);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(Color.BLACK);
        titleView.setPadding(0, 20, 0, 10);
        containerChart.addView(titleView);
    }

    private void addEmptyView(String message) {
        android.widget.TextView emptyView = new android.widget.TextView(this);
        emptyView.setText(message);
        emptyView.setTextSize(16);
        emptyView.setTextColor(Color.GRAY);
        emptyView.setGravity(android.view.Gravity.CENTER);
        emptyView.setPadding(0, 50, 0, 50);
        containerChart.addView(emptyView);
    }

    private void addDebugInfo(String message) {
        android.widget.TextView debugView = new android.widget.TextView(this);
        debugView.setText(message);
        debugView.setTextSize(12);
        debugView.setTextColor(Color.RED);
        debugView.setPadding(0, 5, 0, 5);
        debugView.setBackgroundColor(Color.YELLOW);
        containerChart.addView(debugView);
    }

    // 自定义走势图视图
    private static class TrendChartView extends View {
        private List<LotteryEntry> data;
        private boolean isFrontArea;
        private boolean isAxisSwitched;
        private Paint paint;
        private int cellSize = 35; // 增大单元格尺寸
        private int textSize = 14; // 增大文字尺寸

        public TrendChartView(Context context, List<LotteryEntry> data, boolean isFrontArea, boolean isAxisSwitched) {
            super(context);
            this.data = data;
            this.isFrontArea = isFrontArea;
            this.isAxisSwitched = isAxisSwitched;
            
            // 根据坐标转换状态调整单元格大小
            if (isAxisSwitched && isFrontArea) {
                // 前区转换坐标时，使用更小的单元格
                this.cellSize = 25;
                this.textSize = 10;
            } else if (isAxisSwitched && !isFrontArea) {
                // 后区转换坐标时，使用中等单元格
                this.cellSize = 30;
                this.textSize = 12;
            } else {
                // 原始坐标时，使用正常大小
                this.cellSize = 35;
                this.textSize = 14;
            }
            
            initPaint();
        }

        private void initPaint() {
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setTextSize(textSize);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int maxNumber = isFrontArea ? 35 : 12;
            int width, height;
            
            if (isAxisSwitched) {
                // 转换坐标：号码在X轴，期数在Y轴
                width = (maxNumber + 1) * cellSize;
                height = (data.size() + 1) * cellSize;
            } else {
                // 原始坐标：期数在X轴，号码在Y轴
                width = (data.size() + 1) * cellSize;
                height = (maxNumber + 1) * cellSize;
            }
            
            setMeasuredDimension(width, height);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            int maxNumber = isFrontArea ? 35 : 12;
            
            // 绘制背景
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            
            if (isAxisSwitched) {
                // 转换坐标绘制
                drawSwitchedAxis(canvas, maxNumber);
            } else {
                // 原始坐标绘制
                drawNormalAxis(canvas, maxNumber);
            }
        }
        
        private void drawNormalAxis(Canvas canvas, int maxNumber) {
            // 绘制网格和数字
            for (int i = 0; i <= data.size(); i++) {
                for (int j = 0; j <= maxNumber; j++) {
                    float x = i * cellSize;
                    float y = j * cellSize;
                    
                    // 绘制边框
                    paint.setColor(Color.LTGRAY);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(1);
                    canvas.drawRect(x, y, x + cellSize, y + cellSize, paint);
                    
                    // 绘制列标题（期数）
                    if (j == 0 && i > 0) {
                        paint.setColor(Color.BLUE);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setTextAlign(Paint.Align.CENTER);
                        paint.setFakeBoldText(true);
                        canvas.drawText(String.valueOf(i), x + cellSize/2, y + cellSize/2 + 5, paint);
                        paint.setFakeBoldText(false);
                    }
                    
                    // 绘制行标题（号码）
                    if (i == 0 && j > 0) {
                        paint.setColor(Color.GREEN);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setTextAlign(Paint.Align.CENTER);
                        paint.setFakeBoldText(true);
                        canvas.drawText(String.valueOf(j), x + cellSize/2, y + cellSize/2 + 5, paint);
                        paint.setFakeBoldText(false);
                    }
                }
            }
            
            // 绘制开奖号码
            for (int i = 0; i < data.size(); i++) {
                LotteryEntry entry = data.get(i);
                List<Integer> numbers = isFrontArea ? entry.getFrontNumbers() : entry.getBackNumbers();
                
                for (int number : numbers) {
                    if (number > 0 && number <= maxNumber) {
                        float x = (i + 1) * cellSize;
                        float y = number * cellSize;
                        
                        drawNumberCircle(canvas, x, y);
                    }
                }
            }
        }
        
        private void drawSwitchedAxis(Canvas canvas, int maxNumber) {
            // 绘制网格和数字
            for (int i = 0; i <= maxNumber; i++) {
                for (int j = 0; j <= data.size(); j++) {
                    float x = i * cellSize;
                    float y = j * cellSize;
                    
                    // 绘制边框
                    paint.setColor(Color.LTGRAY);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(1);
                    canvas.drawRect(x, y, x + cellSize, y + cellSize, paint);
                    
                    // 绘制列标题（号码）
                    if (j == 0 && i > 0) {
                        paint.setColor(Color.GREEN);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setTextAlign(Paint.Align.CENTER);
                        paint.setFakeBoldText(true);
                        canvas.drawText(String.valueOf(i), x + cellSize/2, y + cellSize/2 + 5, paint);
                        paint.setFakeBoldText(false);
                    }
                    
                    // 绘制行标题（期数）
                    if (i == 0 && j > 0) {
                        paint.setColor(Color.BLUE);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setTextAlign(Paint.Align.CENTER);
                        paint.setFakeBoldText(true);
                        canvas.drawText(String.valueOf(j), x + cellSize/2, y + cellSize/2 + 5, paint);
                        paint.setFakeBoldText(false);
                    }
                }
            }
            
            // 绘制开奖号码
            for (int i = 0; i < data.size(); i++) {
                LotteryEntry entry = data.get(i);
                List<Integer> numbers = isFrontArea ? entry.getFrontNumbers() : entry.getBackNumbers();
                
                for (int number : numbers) {
                    if (number > 0 && number <= maxNumber) {
                        float x = number * cellSize;
                        float y = (i + 1) * cellSize;
                        
                        drawNumberCircle(canvas, x, y);
                    }
                }
            }
        }
        
        private void drawNumberCircle(Canvas canvas, float x, float y) {
            // 绘制圆点背景
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x + cellSize/2, y + cellSize/2, cellSize/3, paint);
            
            // 绘制圆点边框
            paint.setColor(Color.parseColor("#8B0000")); // 深红色
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            canvas.drawCircle(x + cellSize/2, y + cellSize/2, cellSize/3, paint);
            
            // 绘制数字
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            
            // 根据坐标转换状态计算正确的号码
            int number;
            if (isAxisSwitched) {
                number = (int)(x / cellSize);
            } else {
                number = (int)(y / cellSize);
            }
            canvas.drawText(String.valueOf(number), x + cellSize/2, y + cellSize/2 + 5, paint);
            paint.setFakeBoldText(false);
        }
    }
}
