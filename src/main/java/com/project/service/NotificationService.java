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

    public CaseReopenNotificationResult notifyInvestigatingOfficerAndAnalyst(Connection connection, int caseId,
                                                                             User assignedOfficer, User forensicAnalyst,
                                                                             User supervisoryAuthority)
            throws SQLException {
        boolean officerNotified = false;
        if (assignedOfficer != null) {
            notificationRepository.save(
                    connection,
                    caseId,
                    assignedOfficer.getUserId(),
                    supervisoryAuthority.getUserId(),
                    "Case " + caseId + " has been reopened and moved to Supervisor Review by "
                            + supervisoryAuthority.getRole().getDisplayName() + "."
            );
            officerNotified = true;
        }

        boolean analystNotified = false;
        if (forensicAnalyst != null) {
            notificationRepository.save(
                    connection,
                    caseId,
                    forensicAnalyst.getUserId(),
                    supervisoryAuthority.getUserId(),
                    "Case " + caseId + " has been reopened and returned to Supervisor Review by "
                            + supervisoryAuthority.getRole().getDisplayName() + "."
            );
            analystNotified = true;
        }

        return new CaseReopenNotificationResult(officerNotified, analystNotified);
    }

    public int notifySupervisorsForReviewSubmission(Connection connection, int caseId, List<User> supervisors,
                                                    User investigatingOfficer) throws SQLException {
        if (supervisors == null || supervisors.isEmpty()) {
            return 0;
        }

        int notifiedCount = 0;
        for (User supervisor : supervisors) {
            notificationRepository.save(
                    connection,
                    caseId,
                    supervisor.getUserId(),
                    investigatingOfficer.getUserId(),
                    "Case " + caseId + " has been submitted for Supervisor Review by "
                            + investigatingOfficer.getRole().getDisplayName() + " "
                            + investigatingOfficer.getName() + "."
            );
            notifiedCount++;
        }

        return notifiedCount;
    }

    public List<NotificationRecord> getNotificationsForUser(int recipientUserId) {
        try (Connection connection = DBConnection.getConnection()) {
            return notificationRepository.findByRecipientUserId(connection, recipientUserId);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load notifications from the database.", e);
        }
    }
}
