package com.example.dlt;

import java.util.List;
import java.util.Calendar;

public class LotteryEntry {
    private String issueNumber; // 期号，如 "25091"
    private String drawDate;    // 开奖日期，如 "2025-08-11"
    private List<Integer> frontNumbers; // 前区号码
    private List<Integer> backNumbers;  // 后区号码
    private boolean isBlocked;  // 是否被屏蔽

    public LotteryEntry(String issueNumber, String drawDate, List<Integer> frontNumbers, List<Integer> backNumbers) {
        this.issueNumber = issueNumber;
        this.drawDate = drawDate;
        this.frontNumbers = frontNumbers;
        this.backNumbers = backNumbers;
        this.isBlocked = false; // 默认不屏蔽
    }

    // 带屏蔽状态的构造函数
    public LotteryEntry(String issueNumber, String drawDate, List<Integer> frontNumbers, List<Integer> backNumbers, boolean isBlocked) {
        this.issueNumber = issueNumber;
        this.drawDate = drawDate;
        this.frontNumbers = frontNumbers;
        this.backNumbers = backNumbers;
        this.isBlocked = isBlocked;
    }

    // 保持向后兼容的构造函数
    public LotteryEntry(List<Integer> frontNumbers, List<Integer> backNumbers) {
        this("", "", frontNumbers, backNumbers);
    }

    // 自动生成期号
    public static String generateIssueNumber(int sequence) {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int yearSuffix = year % 100; // 获取年份后两位
        return String.format("%d%03d", yearSuffix, sequence);
    }

    // 获取当前日期字符串
    public static String getCurrentDate() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // 月份从0开始
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return String.format("%d-%02d-%02d", year, month, day);
    }

    public String getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(String issueNumber) {
        this.issueNumber = issueNumber;
    }

    public String getDrawDate() {
        return drawDate;
    }

    public void setDrawDate(String drawDate) {
        this.drawDate = drawDate;
    }

    public List<Integer> getFrontNumbers() {
        return frontNumbers;
    }

    public List<Integer> getBackNumbers() {
        return backNumbers;
    }

    // 格式化前区号码为字符串（如 "01 19 22 25 27"）
    public String getFrontNumbersString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < frontNumbers.size(); i++) {
            sb.append(String.format("%02d", frontNumbers.get(i)));
            if (i < frontNumbers.size() - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    // 格式化后区号码为字符串（如 "03 10"）
    public String getBackNumbersString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < backNumbers.size(); i++) {
            sb.append(String.format("%02d", backNumbers.get(i)));
            if (i < backNumbers.size() - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    // 屏蔽状态相关方法
    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }
} 