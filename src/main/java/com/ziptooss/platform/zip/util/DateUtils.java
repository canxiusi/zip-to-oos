package com.ziptooss.platform.zip.util;

import com.alibaba.fastjson.util.TypeUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author yukun.yan
 * @description DateUtils
 * @date 2023/5/17 14:48
 */
public class DateUtils extends org.apache.commons.lang3.time.DateUtils {

    private static final int NUM_SLOTS = 8;

    private static final int SLOT_LENGTH = 3;

    /**
     * 日期格式
     */
    @Getter
    @AllArgsConstructor
    public enum Style {

        YYYY_MM("yyyy-MM"),
        YYYY_MM_DD("yyyy-MM-dd"),
        YYYY_MM_DD_HH_MM("yyyy-MM-dd HH:mm"),
        YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss"),

        YYYY_MM_EN("yyyy/MM"),
        YYYY_MM_DD_EN("yyyy/MM/dd"),
        YYYY_MM_DD_HH_MM_EN("yyyy/MM/dd HH:mm"),
        YYYY_MM_DD_HH_MM_SS_EN("yyyy/MM/dd HH:mm:ss"),

        YYYY_MM_CN("yyyy年MM月"),
        YYYY_MM_DD_CN("yyyy年MM月dd日"),
        YYYY_MM_DD_HH_MM_CN("yyyy年MM月dd日 HH:mm"),
        YYYY_MM_DD_HH_MM_SS_CN("yyyy年MM月dd日 HH:mm:ss"),

        YYYYMM("yyyyMM"),
        YYYYMMDD("yyyyMMdd"),
        YYYYMMDD_HH_MM("yyyyMMdd HH:mm"),
        YYYYMMDD_HH_MM_SS("yyyyMMdd HH:mm"),

        HH_MM("HH:mm"),
        HH_MM_SS("HH:mm:ss"),

        yyyyMMddHHmmssSS("yyyyMMddHHmmssSS"),
        ;
        private final String value;
    }

    /**
     * 日期转化为Sting类型
     */
    public static String fmDateToStr(Date date, Style pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern.getValue());
        if (date != null) {
            return format.format(date);
        } else {
            return "";
        }
    }

    public static Date getCurrentDate() {
        return new Date();
    }

    public static String getCurrentDateStr() {
        return getCurrentDateStr(Style.YYYY_MM_DD_HH_MM_SS);
    }

    public static String getCurrentDateStr(Style pattern) {
        return fmDateToStr(getCurrentDate(), pattern);
    }

    /**
     * 字符串日期转LocalTime
     *
     * @param dateString
     * @param style
     * @return
     */
    public static LocalTime convertStringToLocalTime(String dateString, Style style) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(style.value);
        LocalDateTime localDateTime = LocalDateTime.parse(dateString, formatter);
        return localDateTime.toLocalTime();
    }

    /**
     * 判断目标日期是否在自定义日期范围内
     *
     * @param targetDate 目标日期
     * @param startDate  起始日期
     * @param endDate    结束日期
     * @return true表示目标日期在自定义日期范围内，false表示不在
     */
    public static boolean isWithinDateRange(LocalDate targetDate, LocalDate startDate, LocalDate endDate) {
        return !targetDate.isBefore(startDate) && !targetDate.isAfter(endDate);
    }

    /**
     * 当前日期往前推几个月, 返回LocalDate类型
     *
     * @param amountToSubtract 月份
     * @return
     */
    public static LocalDate getDateBeforeMonths(long amountToSubtract) {
        return LocalDate.now().minus(amountToSubtract, ChronoUnit.MONTHS);
    }

    /**
     * 判断指定日期是否小于或等于当前日期
     *
     * @param date 指定日期
     * @return 如果小于或等于当前日期返回true，否则返回false
     */
    public static boolean isBeforeOrToday(LocalDate date) {
        return date.isBefore(LocalDate.now()) || date.isEqual(LocalDate.now());
    }

    /**
     * 判断指定日期是否大于当前日期
     *
     * @param date 指定日期
     * @return 如果大于当前日期返回true，否则返回false
     */
    public static boolean isAfterToday(LocalDate date) {
        return date.isAfter(LocalDate.now());
    }

    /**
     * 获取最近几个月内所有的周一日期
     *
     * @param months 最近几个月
     * @return 周一日期列表, 返回Date类型且时分秒为0
     */
    public static List<Date> getMondaysOfRecentMonths(int months) {
        List<Date> mondayList = new ArrayList<>();
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.minusMonths(months).with(DayOfWeek.MONDAY);
        LocalDate endDate = now.with(DayOfWeek.MONDAY);
        while (!startDate.isAfter(endDate)) {
            ZonedDateTime zdt = startDate.atStartOfDay(ZoneId.systemDefault()).withHour(0).withMinute(0).withSecond(0);
            Date date = Date.from(zdt.toInstant());
            mondayList.add(date);
            startDate = startDate.plusWeeks(1);
        }
        return mondayList;
    }

    public static List<String> generateTimeSlots() {
        List<String> timeSlots = new ArrayList<>();
        for (int i = 0; i < NUM_SLOTS; i++) {
            int startHour = i * SLOT_LENGTH;
            int endHour = startHour + SLOT_LENGTH;
            timeSlots.add(startHour + "-" + endHour + "点");
        }
        return timeSlots;
    }

    public static int getCurrentTimeSlot(LocalTime currentTime) {
        int currentHour = currentTime.getHour();
        int currentMinute = currentTime.getMinute();
        int currentMinuteOfDay = currentHour * 60 + currentMinute;
        int slotLengthInMinutes = SLOT_LENGTH * 60;

        for (int i = 0; i < NUM_SLOTS; i++) {
            int startMinuteOfDay = i * slotLengthInMinutes;
            int endMinuteOfDay = startMinuteOfDay + slotLengthInMinutes;
            if (currentMinuteOfDay >= startMinuteOfDay && currentMinuteOfDay < endMinuteOfDay) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取当前时间所在的时段
     *
     * @param currentTime
     * @return
     */
    public static String getCurrentTimeSlotString(LocalTime currentTime) {
        List<String> timeSlots = generateTimeSlots();
        int currentSlot = getCurrentTimeSlot(currentTime);
        return timeSlots.get(currentSlot);
    }

    /**
     * 判断目标日期是否大于当前日期
     *
     * @param date
     * @return
     */
    public static boolean isDateAfterToday(Date date) {
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTime(new Date());
        Calendar targetCalendar = Calendar.getInstance();
        targetCalendar.setTime(date);
        return targetCalendar.after(currentCalendar);
    }

    /**
     * 将Java中的Date类型转换为Linux时间戳, 单位秒
     *
     * @param date Date类型的时间
     * @return long类型的Linux时间戳
     */
    public static long dateToLinuxTimestamp(Date date) {
        return date.getTime() / 1000L;
    }

    /**
     * 将时间戳转换为 Date 类型
     *
     * @param timestampInSeconds 时间戳，单位为秒
     * @return Date 类型的日期时间
     */
    public static Date timestampToDate(long timestampInSeconds) {
        return new Date(timestampInSeconds * 1000);
    }

    public static void main(String[] args) {

        Integer integer = TypeUtils.castToInt("081");

        System.out.println(integer);
        List<Date> mondaysOfRecentMonths = DateUtils.getMondaysOfRecentMonths(3);
        System.out.println("mondaysOfRecentMonths = " + mondaysOfRecentMonths);

        System.out.println("getCurrentTimeSlotString(LocalTime.now()) = " + getCurrentTimeSlotString(LocalTime.now()));
        System.out.println("generateTimeSlots() = " + generateTimeSlots());

        System.out.println(convertStringToLocalTime("2023-04-17 19:15:54", Style.YYYY_MM_DD_HH_MM_SS));
    }

}
