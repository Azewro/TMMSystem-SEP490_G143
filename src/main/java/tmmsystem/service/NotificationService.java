package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.Notification;
import tmmsystem.entity.Rfq;
import tmmsystem.entity.User;
import tmmsystem.entity.Quotation;
import tmmsystem.entity.Contract;
import tmmsystem.entity.ProductionOrder;
import tmmsystem.entity.WorkOrder;
import tmmsystem.repository.NotificationRepository;
import tmmsystem.repository.UserRepository;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Notification createNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Transactional
    public void notifyNewRfq(Rfq rfq) {
        // Tìm tất cả Sale Staff
        List<User> saleStaff = userRepository.findByRoleName("SALE_STAFF");

        for (User user : saleStaff) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("INFO");
            notification.setCategory("ORDER");
            notification.setTitle("RFQ mới từ khách hàng");
            notification.setMessage("Có RFQ mới từ khách hàng " +
                    (rfq.getCustomer() != null ? rfq.getCustomer().getCompanyName() : "N/A") +
                    " - RFQ #" + rfq.getRfqNumber());
            notification.setReferenceType("RFQ");
            notification.setReferenceId(rfq.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyRfqForwardedToPlanning(Rfq rfq) {
        // Tìm tất cả Planning Staff
        List<User> planningStaff = userRepository.findByRoleName("PLANNING_STAFF");

        for (User user : planningStaff) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("INFO");
            notification.setCategory("ORDER");
            notification.setTitle("RFQ chuyển đến Planning");
            notification.setMessage(
                    "RFQ #" + rfq.getRfqNumber() + " đã được chuyển đến phòng kế hoạch để kiểm tra khả năng sản xuất");
            notification.setReferenceType("RFQ");
            notification.setReferenceId(rfq.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyRfqReceivedByPlanning(Rfq rfq) {
        // Thông báo cho Sale Staff rằng Planning đã nhận RFQ
        List<User> saleStaff = userRepository.findByRoleName("SALE_STAFF");

        for (User user : saleStaff) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("INFO");
            notification.setCategory("ORDER");
            notification.setTitle("RFQ đã được Planning nhận");
            notification.setMessage("RFQ #" + rfq.getRfqNumber() + " đã được phòng kế hoạch nhận và đang xử lý");
            notification.setReferenceType("RFQ");
            notification.setReferenceId(rfq.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyRfqCanceled(Rfq rfq) {
        // Thông báo cho tất cả người liên quan
        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("WARNING");
            notification.setCategory("ORDER");
            notification.setTitle("RFQ đã bị hủy");
            notification.setMessage("RFQ #" + rfq.getRfqNumber() + " đã bị hủy");
            notification.setReferenceType("RFQ");
            notification.setReferenceId(rfq.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyQuotationCreated(Quotation quotation) {
        // Nếu có assignedSales thì chỉ gửi cho Sales đó; nếu không, gửi cho tất cả Sale
        // Staff như cũ
        List<User> targets;
        if (quotation.getAssignedSales() != null) {
            targets = java.util.Collections.singletonList(quotation.getAssignedSales());
        } else {
            targets = userRepository.findByRoleName("SALE_STAFF");
        }
        for (User user : targets) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("INFO");
            notification.setCategory("ORDER");
            notification.setTitle("Báo giá đã được tạo");
            notification.setMessage("Báo giá #" + quotation.getQuotationNumber() + " đã được Planning tạo từ RFQ #" +
                    (quotation.getRfq() != null ? quotation.getRfq().getRfqNumber() : "N/A"));
            notification.setReferenceType("QUOTATION");
            notification.setReferenceId(quotation.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyQuotationSentToCustomer(Quotation quotation) {
        List<User> targets;
        if (quotation.getAssignedSales() != null) {
            targets = java.util.Collections.singletonList(quotation.getAssignedSales());
        } else {
            targets = userRepository.findByRoleName("SALE_STAFF");
        }
        for (User user : targets) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("INFO");
            notification.setCategory("ORDER");
            notification.setTitle("Báo giá đã gửi cho khách hàng");
            notification.setMessage("Báo giá #" + quotation.getQuotationNumber() + " đã được gửi cho khách hàng " +
                    (quotation.getCustomer() != null ? quotation.getCustomer().getCompanyName() : "N/A") +
                    " với tổng giá trị " + quotation.getTotalAmount() + " VND");
            notification.setReferenceType("QUOTATION");
            notification.setReferenceId(quotation.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyQuotationApproved(Quotation quotation) {
        List<User> targets;
        if (quotation.getAssignedSales() != null) {
            targets = java.util.Collections.singletonList(quotation.getAssignedSales());
        } else {
            targets = userRepository.findByRoleName("SALE_STAFF");
        }
        for (User user : targets) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("SUCCESS");
            notification.setCategory("ORDER");
            notification.setTitle("Báo giá được duyệt");
            notification.setMessage("Báo giá #" + quotation.getQuotationNumber() + " đã được khách hàng duyệt");
            notification.setReferenceType("QUOTATION");
            notification.setReferenceId(quotation.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyQuotationRejected(Quotation quotation) {
        List<User> targets;
        if (quotation.getAssignedSales() != null) {
            targets = java.util.Collections.singletonList(quotation.getAssignedSales());
        } else {
            targets = userRepository.findByRoleName("SALE_STAFF");
        }
        for (User user : targets) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("WARNING");
            notification.setCategory("ORDER");
            notification.setTitle("Báo giá bị từ chối");
            notification.setMessage("Báo giá #" + quotation.getQuotationNumber() + " đã bị khách hàng từ chối");
            notification.setReferenceType("QUOTATION");
            notification.setReferenceId(quotation.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyOrderCreated(Contract contract) {
        // Thông báo cho Sale Staff
        List<User> saleStaff = userRepository.findByRoleName("SALE_STAFF");

        for (User user : saleStaff) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("SUCCESS");
            notification.setCategory("ORDER");
            notification.setTitle("Đơn hàng mới được tạo");
            notification.setMessage("Đơn hàng #" + contract.getContractNumber() + " đã được tạo từ báo giá #" +
                    (contract.getQuotation() != null ? contract.getQuotation().getQuotationNumber() : "N/A"));
            notification.setReferenceType("CONTRACT");
            notification.setReferenceId(contract.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    // ===== GIAI ĐOẠN 3: CONTRACT UPLOAD & APPROVAL NOTIFICATIONS =====

    @Transactional
    public void notifyContractUploaded(Contract contract) {
        // Thông báo cho Director
        List<User> directors = userRepository.findByRoleName("DIRECTOR");

        for (User user : directors) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("INFO");
            notification.setCategory("CONTRACT");
            notification.setTitle("Hợp đồng mới được upload");
            notification.setMessage(
                    "Hợp đồng #" + contract.getContractNumber() + " đã được Sale Staff upload và chờ duyệt");
            notification.setReferenceType("CONTRACT");
            notification.setReferenceId(contract.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyContractApproved(Contract contract) {
        // Thông báo cho Planning Department
        List<User> planningStaff = userRepository.findByRoleName("PLANNING_STAFF");

        for (User user : planningStaff) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("SUCCESS");
            notification.setCategory("CONTRACT");
            notification.setTitle("Hợp đồng đã được duyệt");
            notification.setMessage(
                    "Hợp đồng #" + contract.getContractNumber() + " đã được Director duyệt, có thể tạo lệnh sản xuất");
            notification.setReferenceType("CONTRACT");
            notification.setReferenceId(contract.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyContractRejected(Contract contract) {
        // Thông báo cho Sale Staff
        List<User> saleStaff = userRepository.findByRoleName("SALE_STAFF");

        for (User user : saleStaff) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("WARNING");
            notification.setCategory("CONTRACT");
            notification.setTitle("Hợp đồng bị từ chối");
            notification.setMessage(
                    "Hợp đồng #" + contract.getContractNumber() + " đã bị Director từ chối, cần upload lại");
            notification.setReferenceType("CONTRACT");
            notification.setReferenceId(contract.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    // ===== GIAI ĐOẠN 4: PRODUCTION ORDER CREATION & APPROVAL NOTIFICATIONS =====

    @Transactional
    public void notifyProductionOrderCreated(ProductionOrder po) {
        // Thông báo cho Director
        List<User> directors = userRepository.findByRoleName("DIRECTOR");

        for (User user : directors) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("INFO");
            notification.setCategory("PRODUCTION");
            notification.setTitle("Lệnh sản xuất mới được tạo");
            notification.setMessage("Lệnh sản xuất #" + po.getPoNumber() + " đã được Planning tạo và chờ duyệt");
            notification.setReferenceType("PRODUCTION_ORDER");
            notification.setReferenceId(po.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyProductionOrderApproved(ProductionOrder po) {
        // Thông báo cho Production Team
        List<User> productionStaff = userRepository.findByRoleName("PRODUCTION_STAFF");

        for (User user : productionStaff) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("SUCCESS");
            notification.setCategory("PRODUCTION");
            notification.setTitle("Lệnh sản xuất đã được duyệt");
            notification.setMessage(
                    "Lệnh sản xuất #" + po.getPoNumber() + " đã được Director duyệt, có thể bắt đầu sản xuất");
            notification.setReferenceType("PRODUCTION_ORDER");
            notification.setReferenceId(po.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyProductionOrderRejected(ProductionOrder po) {
        // Thông báo cho Planning Department
        List<User> planningStaff = userRepository.findByRoleName("PLANNING_STAFF");

        for (User user : planningStaff) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("WARNING");
            notification.setCategory("PRODUCTION");
            notification.setTitle("Lệnh sản xuất bị từ chối");
            notification.setMessage("Lệnh sản xuất #" + po.getPoNumber() + " đã bị Director từ chối, cần chỉnh sửa");
            notification.setReferenceType("PRODUCTION_ORDER");
            notification.setReferenceId(po.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    // ===== PRODUCTION PLAN WORKFLOW NOTIFICATIONS =====

    @Transactional
    public void notifyProductionPlanCreated(tmmsystem.entity.ProductionPlan plan) {
        // Thông báo cho Director
        List<User> directors = userRepository.findByRoleName("DIRECTOR");

        for (User user : directors) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("INFO");
            notification.setCategory("PRODUCTION");
            notification.setTitle("Kế hoạch sản xuất mới được tạo");
            notification.setMessage(
                    "Kế hoạch sản xuất #" + plan.getPlanCode() + " đã được Planning tạo và sẵn sàng để duyệt");
            notification.setReferenceType("PRODUCTION_PLAN");
            notification.setReferenceId(plan.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyProductionPlanSubmittedForApproval(tmmsystem.entity.ProductionPlan plan) {
        // Thông báo cho Director
        List<User> directors = userRepository.findByRoleName("DIRECTOR");

        for (User user : directors) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("WARNING");
            notification.setCategory("PRODUCTION");
            notification.setTitle("Kế hoạch sản xuất chờ duyệt");
            notification.setMessage("Kế hoạch sản xuất #" + plan.getPlanCode() + " đã được Planning gửi để duyệt");
            notification.setReferenceType("PRODUCTION_PLAN");
            notification.setReferenceId(plan.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyProductionPlanApproved(tmmsystem.entity.ProductionPlan plan) {
        // Thông báo cho Planning Department
        List<User> planningStaff = userRepository.findByRoleName("PLANNING_STAFF");

        for (User user : planningStaff) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("SUCCESS");
            notification.setCategory("PRODUCTION");
            notification.setTitle("Kế hoạch sản xuất đã được duyệt");
            notification.setMessage("Kế hoạch sản xuất #" + plan.getPlanCode()
                    + " đã được Director duyệt và Production Order đã được tạo tự động");
            notification.setReferenceType("PRODUCTION_PLAN");
            notification.setReferenceId(plan.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }

        // Thông báo cho Production Team về Production Order mới
        List<User> productionStaff = userRepository.findByRoleName("PRODUCTION_STAFF");

        for (User user : productionStaff) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("INFO");
            notification.setCategory("PRODUCTION");
            notification.setTitle("Production Order mới từ kế hoạch đã duyệt");
            notification.setMessage(
                    "Production Order đã được tạo từ kế hoạch sản xuất #" + plan.getPlanCode() + " đã được duyệt");
            notification.setReferenceType("PRODUCTION_PLAN");
            notification.setReferenceId(plan.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyProductionPlanRejected(tmmsystem.entity.ProductionPlan plan) {
        // Thông báo cho Planning Department
        List<User> planningStaff = userRepository.findByRoleName("PLANNING_STAFF");

        for (User user : planningStaff) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType("WARNING");
            notification.setCategory("PRODUCTION");
            notification.setTitle("Kế hoạch sản xuất bị từ chối");
            notification
                    .setMessage("Kế hoạch sản xuất #" + plan.getPlanCode() + " đã bị Director từ chối, cần chỉnh sửa");
            notification.setReferenceType("PRODUCTION_PLAN");
            notification.setReferenceId(plan.getId());
            notification.setRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifySalesConfirmed(Rfq rfq) {
        // Thông báo cho Planning Staff: Sales đã xác nhận RFQ và chuyển bước tiếp
        List<User> planningStaff = userRepository.findByRoleName("PLANNING_STAFF");
        for (User user : planningStaff) {
            Notification n = new Notification();
            n.setUser(user);
            n.setType("INFO");
            n.setCategory("ORDER");
            n.setTitle("Sales đã xác nhận RFQ");
            n.setMessage("RFQ #" + rfq.getRfqNumber() + " đã được Sales xác nhận, chờ kiểm tra năng lực");
            n.setReferenceType("RFQ");
            n.setReferenceId(rfq.getId());
            n.setRead(false);
            n.setCreatedAt(Instant.now());
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void notifyCapacityInsufficient(Rfq rfq) {
        // Thông báo cho Sales: Planning đánh giá không đủ năng lực và đề xuất ngày mới
        if (rfq.getAssignedSales() != null) {
            User user = userRepository.findById(rfq.getAssignedSales().getId()).orElse(null);
            if (user != null) {
                Notification n = new Notification();
                n.setUser(user);
                n.setType("WARNING");
                n.setCategory("ORDER");
                n.setTitle("Không đủ năng lực sản xuất");
                n.setMessage("RFQ #" + rfq.getRfqNumber() + ": "
                        + (rfq.getCapacityReason() != null ? rfq.getCapacityReason() : "Không đủ công suất") +
                        (rfq.getProposedNewDeliveryDate() != null
                                ? ". Đề xuất ngày: " + rfq.getProposedNewDeliveryDate()
                                : ""));
                n.setReferenceType("RFQ");
                n.setReferenceId(rfq.getId());
                n.setRead(false);
                n.setCreatedAt(Instant.now());
                notificationRepository.save(n);
                return;
            }
        }
        // fallback: gửi cho tất cả Sale Staff
        List<User> sales = userRepository.findByRoleName("SALE_STAFF");
        for (User u : sales) {
            Notification n = new Notification();
            n.setUser(u);
            n.setType("WARNING");
            n.setCategory("ORDER");
            n.setTitle("Không đủ năng lực sản xuất");
            n.setMessage("RFQ #" + rfq.getRfqNumber() + ": cần thương lượng lại thời gian giao hàng");
            n.setReferenceType("RFQ");
            n.setReferenceId(rfq.getId());
            n.setRead(false);
            n.setCreatedAt(Instant.now());
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void notifyWorkOrderApproved(WorkOrder wo) {
        // Notify leaders and production staff
        java.util.List<User> productionStaff = userRepository.findByRoleName("PRODUCTION_STAFF");
        for (User user : productionStaff) {
            Notification n = new Notification();
            n.setUser(user);
            n.setType("SUCCESS");
            n.setCategory("WORK_ORDER");
            n.setTitle("Work Order đã được PM duyệt");
            n.setMessage("WO #" + wo.getWoNumber() + " đã được PM duyệt, công đoạn có thể bắt đầu");
            n.setReferenceType("WORK_ORDER");
            n.setReferenceId(wo.getId());
            n.setRead(false);
            n.setCreatedAt(Instant.now());
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void notifyWorkOrderRejected(WorkOrder wo) {
        // Notify technical/production planning
        java.util.List<User> planningStaff = userRepository.findByRoleName("PLANNING_STAFF");
        for (User user : planningStaff) {
            Notification n = new Notification();
            n.setUser(user);
            n.setType("WARNING");
            n.setCategory("WORK_ORDER");
            n.setTitle("Work Order bị PM từ chối");
            n.setMessage("WO #" + wo.getWoNumber() + " đã bị từ chối. Lý do: "
                    + (wo.getSendStatus() != null ? wo.getSendStatus() : "N/A"));
            n.setReferenceType("WORK_ORDER");
            n.setReferenceId(wo.getId());
            n.setRead(false);
            n.setCreatedAt(Instant.now());
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void notifyOrderCompleted(ProductionOrder po) {
        // Notify all stakeholders: Sales, Planning, Director, Technical, PM
        java.util.List<User> users = new java.util.ArrayList<>();
        users.addAll(userRepository.findByRoleName("SALE_STAFF"));
        users.addAll(userRepository.findByRoleName("PLANNING_STAFF"));
        users.addAll(userRepository.findByRoleName("DIRECTOR"));
        users.addAll(userRepository.findByRoleName("TECHNICAL_STAFF"));
        users.addAll(userRepository.findByRoleName("PRODUCTION_MANAGER"));
        for (User user : users) {
            Notification n = new Notification();
            n.setUser(user);
            n.setType("SUCCESS");
            n.setCategory("PRODUCTION");
            n.setTitle("Đơn hàng đã hoàn tất");
            n.setMessage("Lệnh sản xuất #" + po.getPoNumber() + " đã hoàn tất (PACKAGING PASS)");
            n.setReferenceType("PRODUCTION_ORDER");
            n.setReferenceId(po.getId());
            n.setRead(false);
            n.setCreatedAt(Instant.now());
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void notifyPlanningNewLotsCreated(int count) {
        List<User> planningStaff = userRepository.findByRoleName("PLANNING_STAFF");
        for (User user : planningStaff) {
            Notification n = new Notification();
            n.setUser(user);
            n.setType("INFO");
            n.setCategory("PRODUCTION");
            n.setTitle("Lô sản xuất mới sẵn sàng lập kế hoạch");
            n.setMessage("Có " + count + " lô mới ở trạng thái READY_FOR_PLANNING.");
            n.setReferenceType("PRODUCTION_LOT");
            n.setReferenceId(null);
            n.setRead(false);
            n.setCreatedAt(Instant.now());
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void notifyUser(User user, String category, String type, String title, String message, String referenceType,
            Long referenceId) {
        if (user == null)
            return;
        Notification n = new Notification();
        n.setUser(user);
        n.setCategory(category);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setReferenceType(referenceType);
        n.setReferenceId(referenceId);
        n.setRead(false);
        n.setCreatedAt(Instant.now());
        notificationRepository.save(n);
    }

    @Transactional
    public void notifyRole(String roleName, String category, String type, String title, String message,
            String referenceType, Long referenceId) {
        List<User> users = userRepository.findByRoleName(roleName);
        for (User u : users) {
            notifyUser(u, category, type, title, message, referenceType, referenceId);
        }
    }

    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndReadFalse(userId);
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void notifyQuotationExpiringSoon(Quotation quotation) {
        // 1. Notify Customer (via User linked to Customer)
        if (quotation.getCustomer() != null && quotation.getCustomer().getUser() != null) {
            createNotificationInternal(
                    quotation.getCustomer().getUser(),
                    "WARNING",
                    "QUOTATION",
                    "Báo giá sắp hết hạn",
                    "Báo giá #" + quotation.getQuotationNumber()
                            + " sẽ hết hạn trong 3 giờ nữa. Vui lòng phản hồi sớm.",
                    "QUOTATION",
                    quotation.getId());
        }

        // 2. Notify Assigned Sales
        if (quotation.getAssignedSales() != null) {
            createNotificationInternal(
                    quotation.getAssignedSales(),
                    "WARNING",
                    "QUOTATION",
                    "Báo giá sắp hết hạn",
                    "Báo giá #" + quotation.getQuotationNumber() + " gửi cho khách hàng "
                            + quotation.getCustomer().getCompanyName() + " sẽ hết hạn trong 3 giờ.",
                    "QUOTATION",
                    quotation.getId());
        }
    }

    @Transactional
    public void notifyQuotationExpired(Quotation quotation) {
        // 1. Notify Customer
        if (quotation.getCustomer() != null && quotation.getCustomer().getUser() != null) {
            createNotificationInternal(
                    quotation.getCustomer().getUser(),
                    "ERROR",
                    "QUOTATION",
                    "Báo giá đã hết hạn",
                    "Báo giá #" + quotation.getQuotationNumber() + " đã hết hạn do không có phản hồi trong 12 giờ.",
                    "QUOTATION",
                    quotation.getId());
        }

        // 2. Notify Assigned Sales
        if (quotation.getAssignedSales() != null) {
            createNotificationInternal(
                    quotation.getAssignedSales(),
                    "ERROR",
                    "QUOTATION",
                    "Báo giá đã hết hạn",
                    "Báo giá #" + quotation.getQuotationNumber() + " đã tự động bị từ chối do quá hạn 12 giờ.",
                    "QUOTATION",
                    quotation.getId());
        }
    }

    private void createNotificationInternal(User user, String type, String category, String title, String message,
            String refType, Long refId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setCategory(category);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceType(refType);
        notification.setReferenceId(refId);
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
    }
}
