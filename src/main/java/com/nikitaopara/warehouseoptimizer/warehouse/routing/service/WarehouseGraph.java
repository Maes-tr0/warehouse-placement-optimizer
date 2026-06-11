package com.nikitaopara.warehouseoptimizer.warehouse.routing.service;

import com.nikitaopara.warehouseoptimizer.warehouse.routing.model.WarehouseRouteNode;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WarehouseGraph {

    private final Map<String, WarehouseRouteNode> nodes = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> adjacency = new LinkedHashMap<>();

    void addNode(WarehouseRouteNode node) {
        nodes.putIfAbsent(node.id(), node);
        adjacency.computeIfAbsent(node.id(), ignored -> new LinkedHashMap<>());
    }

    void addBidirectionalEdge(String firstNodeId, String secondNodeId, int distanceMm) {
        if (!nodes.containsKey(firstNodeId) || !nodes.containsKey(secondNodeId)) {
            throw new IllegalArgumentException("Both route nodes must exist before adding an edge");
        }
        if (distanceMm < 0) {
            throw new IllegalArgumentException("Route edge distance cannot be negative");
        }

        addEdge(firstNodeId, secondNodeId, distanceMm);
        addEdge(secondNodeId, firstNodeId, distanceMm);
    }

    WarehouseRouteNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    Collection<WarehouseRouteNode> getNodes() {
        return nodes.values();
    }

    List<RouteEdge> getEdges(String nodeId) {
        return adjacency.getOrDefault(nodeId, Map.of()).entrySet().stream()
                .map(entry -> new RouteEdge(entry.getKey(), entry.getValue()))
                .toList();
    }

    private void addEdge(String fromNodeId, String toNodeId, int distanceMm) {
        adjacency.get(fromNodeId).merge(toNodeId, distanceMm, Math::min);
    }

    record RouteEdge(String targetNodeId, int distanceMm) {
    }
}
