package tmmsystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tmmsystem.entity.*;
import tmmsystem.repository.ContractRepository;
import tmmsystem.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import tmmsystem.repository.ProductionLotOrderRepository;
import tmmsystem.repository.ProductionLotRepository;

@Service
@Slf4j
public class ContractService {
    private final ContractRepository repository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final ProductionPlanService productionPlanService;
    private final ProductionLotRepository lotRepo;
    private final ProductionLotOrderRepository lotOrderRepo;

    @org.springframework.beans.factory.annotation.Value("${lot.merge.windowDays:1}")
    private int lotMergeWindowDays;

    @Autowired
    public ContractService(ContractRepository repository, 
                         UserRepository userRepository,
                         NotificationService notificationService,
                         FileStorageService fileStorageService,
                         ProductionPlanService productionPlanService,
                         ProductionLotRepository lotRepo,
                         ProductionLotOrderRepository lotOrderRepo) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.fileStorageService = fileStorageService;
        this.productionPlanService = productionPlanService;
        this.lotRepo = lotRepo;
        this.lotOrderRepo = lotOrderRepo;
    }

    public List<Contract> findAll() { return repository.findAll(); }
    public Contract findById(Long id) { return repository.findById(id).orElseThrow(); }

    // NEW: queries by approved sales/planning id
    public List<Contract> findBySalesApprovedUserId(Long userId) { return repository.findBySalesApprovedBy_Id(userId); }
    public List<Contract> findByPlanningApprovedUserId(Long userId) { return repository.findByPlanningApprovedBy_Id(userId); }

    @Transactional
    public Contract create(Contract c) { return repository.save(c); }

    @Transactional
    public Contract update(Long id, Contract updated) {
        Contract existing = repository.findById(id).orElseThrow();
        existing.setContractNumber(updated.getContractNumber());
        existing.setQuotation(updated.getQuotation());
        existing.setCustomer(updated.getCustomer());
        existing.setContractDate(updated.getContractDate());
        existing.setDeliveryDate(updated.getDeliveryDate());
        existing.setTotalAmount(updated.getTotalAmount());
        existing.setFilePath(updated.getFilePath());
        existing.setStatus(updated.getStatus());
        existing.setDirectorApprovedBy(updated.getDirectorApprovedBy());
        existing.setDirectorApprovedAt(updated.getDirectorApprovedAt());
        existing.setDirectorApprovalNotes(updated.getDirectorApprovalNotes());
        existing.setCreatedBy(updated.getCreatedBy());
        existing.setApprovedBy(updated.getApprovedBy());
        existing.setAssignedSales(updated.getAssignedSales());
        existing.setAssignedPlanning(updated.getAssignedPlanning());
        // NEW: map approvals
        existing.setSalesApprovedBy(updated.getSalesApprovedBy());
        existing.setSalesApprovedAt(updated.getSalesApprovedAt());
        existing.setPlanningApprovedBy(updated.getPlanningApprovedBy());
        existing.setPlanningApprovedAt(updated.getPlanningApprovedAt());
        return existing;
    }

    public void delete(Long id) { repository.deleteById(id); }

    // ===== GIAI ĐOẠN 3: CONTRACT UPLOAD & APPROVAL =====
    
    @Transactional
    public Contract uploadSignedContract(Long contractId, MultipartFile file, String notes, Long saleUserId) {
        Contract contract = repository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("Contract not found"));
        
        try {
            // Upload file to local storage
            String filePath = fileStorageService.uploadContractFile(file, contractId);
            
            // Update contract
            contract.setFilePath(filePath);
            contract.setStatus("PENDING_APPROVAL");
            contract.setUpdatedAt(Instant.now());
            
            Contract savedContract = repository.save(contract);
            
            // Send notification to Director
            notificationService.notifyContractUploaded(savedContract);
            
            return savedContract;
        } catch (Exception e) {
            log.error("Error uploading contract file", e);
            throw new RuntimeException("Failed to upload contract file: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public Contract approveContract(Long contractId, Long directorId, String notes) {
        Contract contract = repository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("Contract not found"));
        
        User director = userRepository.findById(directorId)
            .orElseThrow(() -> new RuntimeException("Director not found"));
        
        contract.setStatus("APPROVED");
        contract.setDirectorApprovedBy(director);
        contract.setDirectorApprovedAt(Instant.now());
        contract.setDirectorApprovalNotes(notes);
        contract.setUpdatedAt(Instant.now());
        
        Contract savedContract = repository.save(contract);

        // Merge Lot ngay sau khi hợp đồng được duyệt (theo 3 tiêu chí: cùng sản phẩm, ngày giao ±1, ngày ký ±1)
        try {
            productionPlanService.createOrMergeLotFromContract(savedContract.getId());
        } catch (Exception e) {
            log.warn("Merge lot after contract approval failed: {}", e.getMessage());
        }

        // Merge Lots cho tất cả hợp đồng APPROVED chưa có kế hoạch (batch) vẫn giữ để đảm bảo đồng bộ
        mergeLotsForApprovedContracts();

        // Gửi thông báo cho Planning Department
        notificationService.notifyContractApproved(savedContract);
        
        return savedContract;
    }
    
    @Transactional
    public Contract rejectContract(Long contractId, Long directorId, String rejectionNotes) {
        Contract contract = repository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("Contract not found"));
        
        User director = userRepository.findById(directorId)
            .orElseThrow(() -> new RuntimeException("Director not found"));
        
        contract.setStatus("REJECTED");
        contract.setDirectorApprovedBy(director);
        contract.setDirectorApprovedAt(Instant.now());
        contract.setDirectorApprovalNotes(rejectionNotes);
        contract.setUpdatedAt(Instant.now());
        
        Contract savedContract = repository.save(contract);
        
        // Send notification to Sale Staff
        notificationService.notifyContractRejected(savedContract);
        
        return savedContract;
    }
    
    public List<Contract> getContractsPendingApproval() {
        return repository.findByStatus("PENDING_APPROVAL");
    }
    
    public List<Contract> getDirectorPendingContracts() {
        return repository.findByStatus("PENDING_APPROVAL");
    }
    
    public String getContractFileUrl(Long contractId) {
        try {
            return fileStorageService.getContractFileUrl(contractId);
        } catch (Exception e) {
            log.error("Error getting contract file URL for contract ID: {}", contractId, e);
            return null;
        }
    }
    
    public byte[] downloadContractFile(Long contractId) {
        try {
            return fileStorageService.downloadContractFile(contractId);
        } catch (Exception e) {
            log.error("Error downloading contract file for contract ID: {}", contractId, e);
            throw new RuntimeException("Failed to download contract file: " + e.getMessage(), e);
        }
    }
    
    public String getContractFileName(Long contractId) {
        try {
            return fileStorageService.getContractFileName(contractId);
        } catch (Exception e) {
            log.error("Error getting contract file name for contract ID: {}", contractId, e);
            throw new RuntimeException("Failed to get contract file name: " + e.getMessage(), e);
        }
    }
    
    // ===== ORDER DETAILS API =====
    
    public tmmsystem.dto.sales.OrderDetailsDto getOrderDetails(Long contractId) {
        Contract contract = repository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("Contract not found"));
        
        // Build customer info
        tmmsystem.dto.sales.OrderDetailsDto.CustomerInfo customerInfo = tmmsystem.dto.sales.OrderDetailsDto.CustomerInfo.builder()
            .customerId(contract.getCustomer().getId())
            .customerName(contract.getCustomer().getContactPerson())
            .phoneNumber(contract.getCustomer().getPhoneNumber())
            .companyName(contract.getCustomer().getCompanyName())
            .taxCode(contract.getCustomer().getTaxCode())
            .address(contract.getCustomer().getAddress())
            .build();
        
        // Build order items from quotation details
        List<tmmsystem.dto.sales.OrderDetailsDto.OrderItemDto> orderItems = new ArrayList<>();
        if (contract.getQuotation() != null && contract.getQuotation().getDetails() != null) {
            for (tmmsystem.entity.QuotationDetail detail : contract.getQuotation().getDetails()) {
                tmmsystem.dto.sales.OrderDetailsDto.OrderItemDto item = tmmsystem.dto.sales.OrderDetailsDto.OrderItemDto.builder()
                    .productId(detail.getProduct().getId())
                    .productName(detail.getProduct().getName())
                    .productSize(detail.getProduct().getStandardDimensions()) // Using standardDimensions field
                    .quantity(detail.getQuantity())
                    .unit(detail.getUnit())
                    .unitPrice(detail.getUnitPrice())
                    .totalPrice(detail.getTotalPrice())
                    .noteColor(detail.getNoteColor())
                    .build();
                orderItems.add(item);
            }
        }
        
        return tmmsystem.dto.sales.OrderDetailsDto.builder()
            .contractId(contract.getId())
            .contractNumber(contract.getContractNumber())
            .status(contract.getStatus())
            .contractDate(contract.getContractDate())
            .deliveryDate(contract.getDeliveryDate())
            .totalAmount(contract.getTotalAmount())
            .filePath(contract.getFilePath())
            .customerInfo(customerInfo)
            .orderItems(orderItems)
            .build();
    }
    
    /**
     * Create a basic Production Plan for approved contract
     * This method creates a draft production plan that Planning Department can then customize
     */
    private void createProductionPlanForContract(Contract contract) {
        try {
            // Check if production plan already exists for this contract
            if (productionPlanService.findPlansByContract(contract.getId()).size() > 0) {
                log.info("Production plan already exists for contract {}", contract.getId());
                return;
            }
            
            // Create a basic production plan request
            tmmsystem.dto.production_plan.CreateProductionPlanRequest request = 
                new tmmsystem.dto.production_plan.CreateProductionPlanRequest();
            request.setContractId(contract.getId());
            request.setNotes("Auto-generated from approved contract: " + contract.getContractNumber());
            
            // Create basic plan details from quotation details
            if (contract.getQuotation() != null && contract.getQuotation().getDetails() != null) {
                List<tmmsystem.dto.production_plan.ProductionPlanDetailRequest> details = new ArrayList<>();
                
                for (tmmsystem.entity.QuotationDetail quotationDetail : contract.getQuotation().getDetails()) {
                    tmmsystem.dto.production_plan.ProductionPlanDetailRequest detailRequest = 
                        new tmmsystem.dto.production_plan.ProductionPlanDetailRequest();
                    
                    detailRequest.setProductId(quotationDetail.getProduct().getId());
                    detailRequest.setPlannedQuantity(quotationDetail.getQuantity());
                    detailRequest.setRequiredDeliveryDate(contract.getDeliveryDate());
                    
                    // Set proposed dates (can be adjusted by Planning Department)
                    detailRequest.setProposedStartDate(java.time.LocalDate.now().plusDays(7)); // Start in 1 week
                    detailRequest.setProposedEndDate(contract.getDeliveryDate().minusDays(3)); // End 3 days before delivery
                    
                    detailRequest.setNotes("Auto-generated from quotation detail");
                    
                    details.add(detailRequest);
                }
                
                request.setDetails(details);
            }
            
            // Create the production plan
            productionPlanService.createPlanFromContract(request);
            
            log.info("Successfully created production plan for contract {}", contract.getId());
            
        } catch (Exception e) {
            log.error("Error creating production plan for contract {}: {}", contract.getId(), e.getMessage(), e);
            throw e;
        }
    }

    private void mergeLotsForApprovedContracts() {
        List<Contract> approved = repository.findApprovedWithoutPlan();
        if (approved == null || approved.isEmpty()) return;
        record Key(String productName, String size, java.time.LocalDate deliveryPivot, java.time.LocalDate contractPivot) {}
        java.util.Map<Key, java.util.List<Contract>> groups = new java.util.HashMap<>();
        for (Contract c : approved) {
            if (c.getQuotation()==null || c.getQuotation().getDetails()==null || c.getQuotation().getDetails().isEmpty()) continue;
            var d = c.getQuotation().getDetails().get(0);
            String name = d.getProduct().getName();
            String size = null;
            try { size = d.getProduct().getStandardDimensions(); } catch (Exception ignore) {}
            java.time.LocalDate del = c.getDeliveryDate();
            java.time.LocalDate con = c.getContractDate();
            Key k = new Key(name, size, del, con);
            groups.computeIfAbsent(k, x -> new java.util.ArrayList<>()).add(c);
        }
        for (var e : groups.entrySet()) {
            var baseKey = e.getKey();
            var list = e.getValue();
            // Filter within window ±lotMergeWindowDays around pivots
            java.time.LocalDate del0 = baseKey.deliveryPivot();
            java.time.LocalDate con0 = baseKey.contractPivot();
            java.time.LocalDate delMin = del0.minusDays(lotMergeWindowDays);
            java.time.LocalDate delMax = del0.plusDays(lotMergeWindowDays);
            java.time.LocalDate conMin = con0.minusDays(lotMergeWindowDays);
            java.time.LocalDate conMax = con0.plusDays(lotMergeWindowDays);
            java.util.List<Contract> windowed = list.stream()
                    .filter(c -> !c.getDeliveryDate().isBefore(delMin) && !c.getDeliveryDate().isAfter(delMax))
                    .filter(c -> !c.getContractDate().isBefore(conMin) && !c.getContractDate().isAfter(conMax))
                    .toList();
            if (windowed.isEmpty()) continue;
            Contract base = windowed.get(0);
            var qd0 = base.getQuotation().getDetails().get(0);
            Product product = qd0.getProduct();
            java.math.BigDecimal totalQty = java.math.BigDecimal.ZERO;
            for (Contract c : windowed) {
                for (var qd : c.getQuotation().getDetails()) totalQty = totalQty.add(qd.getQuantity());
            }
            ProductionLot lot = lotRepo.findAll().stream()
                    .filter(l -> l.getProduct()!=null && l.getProduct().getId().equals(product.getId()))
                    .filter(l -> baseKey.size()==null || (l.getSizeSnapshot()!=null && l.getSizeSnapshot().equals(baseKey.size())))
                    .filter(l -> "READY_FOR_PLANNING".equals(l.getStatus()) || "FORMING".equals(l.getStatus()))
                    .findFirst().orElse(null);
            if (lot==null) {
                lot = new ProductionLot();
                lot.setLotCode("LOT-"+java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)+"-"+String.format("%03d", lotRepo.count()+1));
                lot.setProduct(product);
                lot.setSizeSnapshot(baseKey.size()!=null ? baseKey.size() : product.getStandardDimensions());
                lot.setDeliveryDateTarget(base.getDeliveryDate());
                lot.setContractDateMin(base.getContractDate());
                lot.setContractDateMax(base.getContractDate());
                lot.setStatus("FORMING");
                lot.setTotalQuantity(java.math.BigDecimal.ZERO);
                lot = lotRepo.save(lot);
            }
            for (Contract c : windowed) {
                for (var qd : c.getQuotation().getDetails()) {
                    ProductionLotOrder lo = new ProductionLotOrder();
                    lo.setLot(lot); lo.setContract(c); lo.setQuotationDetail(qd); lo.setAllocatedQuantity(qd.getQuantity());
                    lotOrderRepo.save(lo);
                    lot.setTotalQuantity(lot.getTotalQuantity().add(qd.getQuantity()));
                }
                // widen contractDate range
                if (c.getContractDate().isBefore(lot.getContractDateMin())) lot.setContractDateMin(c.getContractDate());
                if (c.getContractDate().isAfter(lot.getContractDateMax())) lot.setContractDateMax(c.getContractDate());
            }
            lot.setStatus("READY_FOR_PLANNING");
            lotRepo.save(lot);
            productionPlanService.createPlanVersion(lot.getId());
        }
    }
}
