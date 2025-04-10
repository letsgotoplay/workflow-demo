package com.example.creditpromotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.creditpromotion.entity.ReminderRecord;
import com.example.creditpromotion.enums.ReminderStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒记录数据访问接口
 */
@Repository
public interface ReminderRecordRepository extends JpaRepository<ReminderRecord, Long> {

    /**
     * 根据流程实例ID查询提醒记录
     * 
     * @param processInstanceId 流程实例ID
     * @return 提醒记录列表
     */
    List<ReminderRecord> findByProcessInstanceId(Long processInstanceId);

    /**
     * 查询待发送的提醒记录
     * 
     * @param status 提醒状态
     * @param reminderTime 提醒时间
     * @return 提醒记录列表
     */
    List<ReminderRecord> findByStatusAndReminderTimeLessThanEqual(ReminderStatus status, LocalDateTime reminderTime);
}