package africa.epf.signalville_backend.api.dto.response;

import java.util.List;
import java.util.Map;

public record SupervisorDashboardResponse(
        long unassignedCount,
        long reopenedCount,
        long last24HoursCount,
        long inProgressCount,
        long toVerifyCount,
        long closedCount,
        long criticalCount,
        Map<String, Long> byStatus,
        Map<String, Long> byCategory,
        Map<String, Long> byDistrict,
        List<ReportResponse> reportsToVerify,
        List<ReportResponse> criticalReports,
        List<ReportResponse> reopenedReports,
        List<ReportResponse> unassignedReports) {
}