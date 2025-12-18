package tmmsystem.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.Contract;
import tmmsystem.entity.ProductionLotOrder;
import tmmsystem.repository.ContractRepository;
import tmmsystem.repository.ProductionLotOrderRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Startup initializer to sync contract statuses with production lot statuses.
 * Runs once when application starts to ensure data consistency.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContractStatusInitializer {

    private final ContractRepository contractRepository;
    private final ProductionLotOrderRepository lotOrderRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncContractStatuses() {
        log.info("Starting contract status synchronization...");

        int updatedToInProduction = 0;
        int updatedToCompleted = 0;

        // Get all contracts that are APPROVED (approved by director, waiting for
        // production)
        List<Contract> approvedContracts = contractRepository.findAll().stream()
                .filter(c -> "APPROVED".equals(c.getStatus()))
                .collect(Collectors.toList());

        for (Contract contract : approvedContracts) {
            List<ProductionLotOrder> lotOrders = lotOrderRepository.findByContractId(contract.getId());
            if (lotOrders.isEmpty())
                continue;

            // Check if any lot has PLAN_APPROVED, IN_PRODUCTION, or COMPLETED status
            boolean hasApprovedLot = lotOrders.stream()
                    .map(ProductionLotOrder::getLot)
                    .filter(lot -> lot != null)
                    .anyMatch(lot -> {
                        String status = lot.getStatus();
                        return "PLAN_APPROVED".equals(status)
                                || "IN_PRODUCTION".equals(status)
                                || "COMPLETED".equals(status);
                    });

            if (hasApprovedLot) {
                contract.setStatus("IN_PRODUCTION");
                contractRepository.save(contract);
                updatedToInProduction++;
                log.debug("Contract {} updated to IN_PRODUCTION", contract.getContractNumber());
            }
        }

        // Get all contracts that are IN_PRODUCTION (check if should be COMPLETED)
        List<Contract> inProductionContracts = contractRepository.findAll().stream()
                .filter(c -> "IN_PRODUCTION".equals(c.getStatus()))
                .collect(Collectors.toList());

        for (Contract contract : inProductionContracts) {
            List<ProductionLotOrder> lotOrders = lotOrderRepository.findByContractId(contract.getId());
            if (lotOrders.isEmpty())
                continue;

            // Check if ALL lots are COMPLETED
            boolean allLotsCompleted = lotOrders.stream()
                    .map(ProductionLotOrder::getLot)
                    .filter(lot -> lot != null)
                    .allMatch(lot -> "COMPLETED".equals(lot.getStatus()));

            if (allLotsCompleted) {
                contract.setStatus("PRODUCTION_COMPLETED");
                contractRepository.save(contract);
                updatedToCompleted++;
                log.debug("Contract {} updated to PRODUCTION_COMPLETED", contract.getContractNumber());
            }
        }

        log.info("Contract status sync complete: {} to IN_PRODUCTION, {} to PRODUCTION_COMPLETED",
                updatedToInProduction, updatedToCompleted);
    }
}
