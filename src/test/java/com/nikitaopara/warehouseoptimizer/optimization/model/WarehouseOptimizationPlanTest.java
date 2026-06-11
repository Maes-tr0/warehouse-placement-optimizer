package com.nikitaopara.warehouseoptimizer.optimization.model;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseOptimizationPlanTest {

    @Test
    void approvalMakesOnlyFirstRelocationStepReady() {
        WarehouseOptimizationPlan plan = WarehouseOptimizationPlan.builder().build();
        WarehouseRelocationStep first = WarehouseRelocationStep.builder()
                .sequenceNumber(1)
                .status(RelocationStepStatus.PENDING)
                .build();
        WarehouseRelocationStep second = WarehouseRelocationStep.builder()
                .sequenceNumber(2)
                .status(RelocationStepStatus.PENDING)
                .build();
        plan.addStep(first);
        plan.addStep(second);
        User approver = User.builder().id(10L).build();

        plan.approve(approver);

        assertThat(plan.getStatus()).isEqualTo(OptimizationPlanStatus.APPROVED);
        assertThat(plan.getApprovedBy()).isSameAs(approver);
        assertThat(first.getStatus()).isEqualTo(RelocationStepStatus.READY);
        assertThat(second.getStatus()).isEqualTo(RelocationStepStatus.PENDING);
    }
}
