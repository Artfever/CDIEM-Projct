package com.project.repository;

import com.project.model.NotificationRecord;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface NotificationRepository {
    void save(Connection connection, Integer caseId, int recipientUserId, int sentByUserId, String message) throws SQLException;

    List<NotificationRecord> findByRecipientUserId(Connection connection, int recipientUserId) throws SQLException;
}
