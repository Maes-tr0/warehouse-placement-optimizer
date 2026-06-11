package com.nikitaopara.warehouseoptimizer.warehouse.routing.service;

import com.nikitaopara.warehouseoptimizer.warehouse.routing.model.WarehouseRouteNode;
import com.nikitaopara.warehouseoptimizer.warehouse.routing.model.WarehouseRouteNodeType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DijkstraWarehouseRouterTest {

    private final DijkstraWarehouseRouter router = new DijkstraWarehouseRouter();

    @Test
    void selectsCheapestRouteInsteadOfFewestEdges() {
        WarehouseGraph graph = new WarehouseGraph();
        addNode(graph, "ENTRY");
        addNode(graph, "EXPENSIVE");
        addNode(graph, "CHEAP-1");
        addNode(graph, "CHEAP-2");
        addNode(graph, "TARGET");
        graph.addBidirectionalEdge("ENTRY", "EXPENSIVE", 900);
        graph.addBidirectionalEdge("EXPENSIVE", "TARGET", 900);
        graph.addBidirectionalEdge("ENTRY", "CHEAP-1", 300);
        graph.addBidirectionalEdge("CHEAP-1", "CHEAP-2", 300);
        graph.addBidirectionalEdge("CHEAP-2", "TARGET", 300);

        var route = router.findShortestRoute(graph, "ENTRY", "TARGET").orElseThrow();

        assertThat(route.distanceMm()).isEqualTo(900);
        assertThat(route.nodes()).extracting(WarehouseRouteNode::id)
                .containsExactly("ENTRY", "CHEAP-1", "CHEAP-2", "TARGET");
    }

    private void addNode(WarehouseGraph graph, String id) {
        graph.addNode(new WarehouseRouteNode(
                id,
                WarehouseRouteNodeType.AISLE_ACCESS,
                id,
                0,
                0
        ));
    }
}
