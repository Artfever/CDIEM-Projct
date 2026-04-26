package com.project.repository;

import com.project.model.NotificationRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of NotificationRepository using SQL Server.
 * Handles storing and retrieving user notifications.
 */
public class NotificationRepositoryImpl implements NotificationRepository {
    private static final String INSERT_NOTIFICATION_SQL = """
            INSERT INTO Notifications (CaseID, RecipientUserID, SentByUserID, Message, Channel)
            VALUES (?, ?, ?, ?, 'SYSTEM')
            """;
    private static final String DELETE_BY_CASE_ID_SQL = """
            DELETE FROM Notifications
            WHERE CaseID = ?
            """;
    private static final String FIND_BY_RECIPIENT_SQL = """
            SELECT n.NotificationID, n.CaseID, n.RecipientUserID, n.SentByUserID, n.Message, n.Channel, n.CreatedAt,
                   sender.Name AS SenderName
            FROM Notifications n
            JOIN Users sender
                ON sender.UserID = n.SentByUserID
            WHERE n.RecipientUserID = ?
            ORDER BY n.CreatedAt DESC, n.NotificationID DESC
            """;

    @Override
    public void save(Connection connection, Integer caseId, int recipientUserId, int sentByUserId, String message) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_NOTIFICATION_SQL)) {
            if (caseId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, caseId);
            }

            statement.setInt(2, recipientUserId);
            statement.setInt(3, sentByUserId);
            statement.setString(4, message);
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteByCaseId(Connection connection, int caseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_BY_CASE_ID_SQL)) {
            statement.setInt(1, caseId);
            statement.executeUpdate();
        }
    }

    @Override
    public List<NotificationRecord> findByRecipientUserId(Connection connection, int recipientUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_RECIPIENT_SQL)) {
            statement.setInt(1, recipientUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<NotificationRecord> notifications = new ArrayList<>();
                while (resultSet.next()) {
                    Timestamp createdAt = resultSet.getTimestamp("CreatedAt");
                    notifications.add(new NotificationRecord(
                            resultSet.getInt("NotificationID"),
                            (Integer) resultSet.getObject("CaseID"),
                            resultSet.getInt("RecipientUserID"),
                            resultSet.getInt("SentByUserID"),
                            resultSet.getString("SenderName"),
                            resultSet.getString("Message"),
                            resultSet.getString("Channel"),
                            createdAt == null ? null : createdAt.toLocalDateTime()
                    ));
                }
                return notifications;
            }
        }
    }
}
