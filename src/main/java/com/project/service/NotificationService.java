package com.project.service;

import com.project.model.NotificationRecord;
import com.project.model.User;
import com.project.repository.NotificationRepository;
import com.project.repository.NotificationRepositoryImpl;
import com.project.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService() {
        this(new NotificationRepositoryImpl());
    }

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public NotificationDispatchResult notifyBothOfficers(Connection connection, int caseId, User previousOfficer,
                                                         User newOfficer, User supervisoryAuthority) throws SQLException {
        boolean outgoingOfficerNotified = false;
        if (previousOfficer != null) {
            notificationRepository.save(
                    connection,
                    caseId,
                    previousOfficer.getUserId(),
                    supervisoryAuthority.getUserId(),
                    "Case " + caseId + " has been reassigned from you to " + newOfficer.getName()
                            + " by " + supervisoryAuthority.getRole().getDisplayName() + "."
            );
            outgoingOfficerNotified = true;
        }

        notificationRepository.save(
                connection,
                caseId,
                newOfficer.getUserId(),
                supervisoryAuthority.getUserId(),
                "Case " + caseId + " has been assigned to you by "
                        + supervisoryAuthority.getRole().getDisplayName() + "."
        );

        return new NotificationDispatchResult(outgoingOfficerNotified, true);
    }

    public List<NotificationRecord> getNotificationsForUser(int recipientUserId) {
        try (Connection connection = DBConnection.getConnection()) {
            return notificationRepository.findByRecipientUserId(connection, recipientUserId);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load notifications from the database.", e);
        }
    }
}
