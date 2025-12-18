package tmmsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tmmsystem.entity.Contract;
import tmmsystem.entity.ProductionLot;
import tmmsystem.entity.ProductionLotOrder;
import tmmsystem.repository.ContractRepository;
import tmmsystem.repository.ProductionLotOrderRepository;
import tmmsystem.repository.ProductionLotRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to manage contract status updates based on production lot status.
 * - IN_PRODUCTION: When director approves production plan containing the
 * contract
 * - PRODUCTION_COMPLETED: When ALL lots containing the contract are completed
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContractStatusService {

    private final ContractRepository contractRepository;
    private final ProductionLotOrderRepository lotOrderRepository;
    private final ProductionLotRepository lotRepository;
    private final WebSocketService webSocketService;

    /**
     * Update contract status to IN_PRODUCTION if it's currently in SIGNED status.
     * Called when production plan is approved.
     */
    @Transactional
    public void updateContractToInProduction(Contract contract) {
        if (contract == null)
            return;

        String currentStatus = contract.getStatus();
        // Only transition from APPROVED (after director approved contract)
        if ("APPROVED".equals(currentStatus)) {
            contract.setStatus("IN_PRODUCTION");
            contractRepository.save(contract);
            log.info("Contract {} status updated to IN_PRODUCTION", contract.getContractNumber());
            webSocketService.broadcastDataUpdate("CONTRACT", contract.getId(), "STATUS_CHANGED");
        }
    }

    /**
     * Update all contracts in a lot to IN_PRODUCTION status.
     * Called when lot's production plan is approved.
     */
    @Transactional
    public void updateContractsToInProductionForLot(ProductionLot lot) {
        if (lot == null)
            return;

        List<ProductionLotOrder> lotOrders = lotOrderRepository.findByLotId(lot.getId());
        Set<Contract> contracts = lotOrders.stream()
                .map(ProductionLotOrder::getContract)
                .filter(c -> c != null)
                .collect(Collectors.toSet());

        for (Contract contract : contracts) {
            updateContractToInProduction(contract);
        }
        log.info("Updated {} contracts to IN_PRODUCTION for lot {}", contracts.size(), lot.getLotCode());
    }

    /**
     * Check if all lots containing this contract are completed.
     * If yes, update contract status to PRODUCTION_COMPLETED.
     */
    @Transactional
    public void checkAndUpdateContractCompletion(Contract contract) {
        if (contract == null)
            return;

        // Only check if contract is IN_PRODUCTION
        if (!"IN_PRODUCTION".equals(contract.getStatus())) {
            return;
        }

        List<ProductionLotOrder> lotOrders = lotOrderRepository.findByContractId(contract.getId());
        if (lotOrders.isEmpty()) {
            return;
        }

        // Check if ALL lots are completed
        boolean allLotsCompleted = lotOrders.stream()
                .map(ProductionLotOrder::getLot)
                .filter(lot -> lot != null)
                .allMatch(lot -> "COMPLETED".equals(lot.getStatus()));

        if (allLotsCompleted) {
            contract.setStatus("PRODUCTION_COMPLETED");
            contractRepository.save(contract);
            log.info("Contract {} status updated to PRODUCTION_COMPLETED (all lots completed)",
                    contract.getContractNumber());
            webSocketService.broadcastDataUpdate("CONTRACT", contract.getId(), "STATUS_CHANGED");
        }
    }

    /**
     * Check all contracts in a lot for completion.
     * Called when a lot is marked as completed.
     */
    @Transactional
    public void checkContractsCompletionForLot(ProductionLot lot) {
        if (lot == null)
            return;

        // Save lot status as COMPLETED if not already
        if (!"COMPLETED".equals(lot.getStatus())) {
            lot.setStatus("COMPLETED");
            lotRepository.save(lot);
            log.info("Lot {} status updated to COMPLETED", lot.getLotCode());
            webSocketService.broadcastDataUpdate("PRODUCTION_LOT", lot.getId(), "STATUS_CHANGED");
        }

        List<ProductionLotOrder> lotOrders = lotOrderRepository.findByLotId(lot.getId());
        Set<Contract> contracts = lotOrders.stream()
                .map(ProductionLotOrder::getContract)
                .filter(c -> c != null)
                .collect(Collectors.toSet());

        for (Contract contract : contracts) {
            checkAndUpdateContractCompletion(contract);
        }
    }
}
